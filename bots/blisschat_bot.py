"""
blisschat bot SDK (Python 3.9+).  pip install -r requirements.txt

A bot is an ordinary blisschat account whose username ends with "_bot". You create it in the app
("+" -> Бот) and get a token like  my_cool_bot:Ab3dE...  .  The bot speaks the same end-to-end encrypted
protocol as the app, so run it on any always-on machine (a VPS, Render, Railway, Fly.io, a Raspberry Pi,
PythonAnywhere "always-on task"...) - just like a Telegram bot, but nothing is sent to the server in clear text.

    from blisschat_bot import Bot
    bot = Bot("my_cool_bot:TOKEN")

    @bot.command("start")
    def start(ctx):
        ctx.reply("Привет! Я бот.")

    @bot.on_message
    def echo(ctx):
        ctx.reply("Вы написали: " + ctx.text)

    bot.run()
"""
from __future__ import annotations

import base64
import hashlib
import json
import os
import time
import uuid
from dataclasses import dataclass
from typing import Any, Callable, Dict, List, Optional

import requests
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.hkdf import HKDF

API_KEY = "AIzaSyBUXc71LMSwk0Nb82bieZFZbGeMtJ5uU2s"   # Firebase web API key of the blisschat project (public by design)
PROJECT = "bitrate-71684"
BOT_EMAIL_DOMAIN = "bots.blisschat.app"

# ----------------------------------------------------------------------------------------------- crypto
# Byte-for-byte the same scheme as the Android app (data/crypto/Crypto.kt + E2EE.kt).


def b64(b: bytes) -> str:
    return base64.b64encode(b).decode()


def unb64(s: str) -> bytes:
    return base64.b64decode(s)


def kid_of(public_x509: bytes) -> str:
    return base64.urlsafe_b64encode(hashlib.sha256(public_x509).digest()).decode().rstrip("=")[:16]


def pair_key(priv: ec.EllipticCurvePrivateKey, peer_x509: bytes, salt: str) -> bytes:
    peer = serialization.load_der_public_key(peer_x509)
    shared = priv.exchange(ec.ECDH(), peer)
    return HKDF(algorithm=hashes.SHA256(), length=32, salt=salt.encode(), info=b"blisschat-pair-v1").derive(shared)


def seal(key: bytes, plain: bytes, aad: Optional[bytes] = None) -> bytes:
    iv = os.urandom(12)
    return iv + AESGCM(key).encrypt(iv, plain, aad)          # iv(12) || ciphertext || tag(16)


def open_(key: bytes, blob: bytes, aad: Optional[bytes] = None) -> bytes:
    return AESGCM(key).decrypt(blob[:12], blob[12:], aad)


# ----------------------------------------------------------------------------------------------- Firestore REST


def _enc(v: Any) -> dict:
    if v is None:
        return {"nullValue": None}
    if isinstance(v, bool):
        return {"booleanValue": v}
    if isinstance(v, int):
        return {"integerValue": str(v)}
    if isinstance(v, float):
        return {"doubleValue": v}
    if isinstance(v, bytes):
        return {"bytesValue": b64(v)}
    if isinstance(v, str):
        return {"stringValue": v}
    if isinstance(v, (list, tuple)):
        return {"arrayValue": {"values": [_enc(x) for x in v]}}
    if isinstance(v, dict):
        return {"mapValue": {"fields": {k: _enc(x) for k, x in v.items()}}}
    raise TypeError(type(v))


def _dec(v: dict) -> Any:
    (t, x), = v.items()
    if t == "nullValue":
        return None
    if t == "integerValue":
        return int(x)
    if t == "arrayValue":
        return [_dec(i) for i in x.get("values", [])]
    if t == "mapValue":
        return {k: _dec(i) for k, i in x.get("fields", {}).items()}
    if t == "bytesValue":
        return unb64(x)
    return x


def _doc(d: dict) -> dict:
    out = {k: _dec(v) for k, v in d.get("fields", {}).items()}
    out["_id"] = d["name"].rsplit("/", 1)[-1]
    return out


class BotError(Exception):
    pass


@dataclass
class Context:
    """What your handler receives for every incoming message."""
    bot: "Bot"
    chat: dict
    sender_id: str
    text: str
    kind: str
    raw: dict

    @property
    def chat_id(self) -> str:
        return self.chat["_id"]

    @property
    def is_private(self) -> bool:
        return self.chat.get("type") == "DIRECT"

    @property
    def command(self) -> Optional[str]:
        t = self.text.strip()
        return t[1:].split()[0].split("@")[0].lower() if t.startswith("/") and len(t) > 1 else None

    @property
    def args(self) -> List[str]:
        return self.text.strip().split()[1:]

    def reply(self, text: str) -> None:
        self.bot.send(self.chat_id, text)

    def reply_photo(self, data: bytes, width: int = 0, height: int = 0) -> None:
        self.bot.send_image(self.chat_id, data, width, height)


class Bot:
    def __init__(self, token: str, key_file: Optional[str] = None, poll_interval: float = 1.5):
        try:
            self.username, self._password = token.strip().split(":", 1)
        except ValueError:
            raise BotError("Токен должен выглядеть как  имя_bot:пароль")
        self.email = f"{self.username}@{BOT_EMAIL_DOMAIN}"
        self.poll = poll_interval
        self.key_file = key_file or f".blisschat_{self.username}.key.json"
        self._s = requests.Session()
        self._id_token = ""
        self._refresh = ""
        self._exp = 0.0
        self.uid = ""
        self._priv: Optional[ec.EllipticCurvePrivateKey] = None
        self.kid = ""
        self._pub_x509 = b""
        self._handlers: List[Callable[[Context], None]] = []
        self._commands: Dict[str, Callable[[Context], None]] = {}
        self._peer_keys: Dict[str, bytes] = {}
        self._peer_kid_cache: Dict[str, str] = {}
        self._group_keys: Dict[str, bytes] = {}
        self._seen: Dict[str, int] = {}
        self._start = int(time.time() * 1000)

    # ------------------------------------------------------------------ decorators
    def on_message(self, fn: Callable[[Context], None]):
        self._handlers.append(fn)
        return fn

    def command(self, name: str):
        def deco(fn):
            self._commands[name.lower().lstrip("/")] = fn
            return fn
        return deco

    # ------------------------------------------------------------------ auth
    def _login(self) -> None:
        r = self._s.post(
            f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={API_KEY}",
            json={"email": self.email, "password": self._password, "returnSecureToken": True}, timeout=20)
        if r.status_code != 200:
            raise BotError("Не удалось войти: проверьте токен бота (" + r.json().get("error", {}).get("message", str(r.status_code)) + ")")
        self._store(r.json())

    def _store(self, j: dict) -> None:
        self._id_token = j.get("idToken") or j.get("id_token")
        self._refresh = j.get("refreshToken") or j.get("refresh_token")
        self.uid = j.get("localId") or j.get("user_id") or self.uid
        self._exp = time.time() + int(j.get("expiresIn") or j.get("expires_in") or 3600) - 120

    def _token(self) -> str:
        if time.time() >= self._exp:
            r = self._s.post(f"https://securetoken.googleapis.com/v1/token?key={API_KEY}",
                             data={"grant_type": "refresh_token", "refresh_token": self._refresh}, timeout=20)
            if r.status_code != 200:
                self._login()
            else:
                self._store(r.json())
        return self._id_token

    # ------------------------------------------------------------------ Firestore
    @property
    def _base(self) -> str:
        return f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents"

    def _req(self, method: str, url: str, **kw) -> requests.Response:
        for attempt in range(4):
            r = self._s.request(method, url, headers={"Authorization": "Bearer " + self._token()}, timeout=30, **kw)
            if r.status_code in (429, 500, 503):
                time.sleep(1 + attempt)
                continue
            return r
        return r

    def _get(self, path: str) -> Optional[dict]:
        r = self._req("GET", f"{self._base}/{path}")
        if r.status_code == 404:
            return None
        if r.status_code != 200:
            raise BotError(f"GET {path}: {r.status_code} {r.text[:200]}")
        return _doc(r.json())

    def _query(self, parent: str, q: dict) -> List[dict]:
        url = f"{self._base}/{parent}:runQuery" if parent else f"{self._base}:runQuery"
        r = self._req("POST", url, json={"structuredQuery": q})
        if r.status_code != 200:
            raise BotError(f"query: {r.status_code} {r.text[:200]}")
        return [_doc(x["document"]) for x in r.json() if "document" in x]

    def _commit(self, writes: List[dict]) -> None:
        r = self._req("POST", f"{self._base}:commit", json={"writes": writes})
        if r.status_code != 200:
            raise BotError(f"commit: {r.status_code} {r.text[:300]}")

    def _name(self, path: str) -> str:
        return f"projects/{PROJECT}/databases/(default)/documents/{path}"

    # ------------------------------------------------------------------ keys
    def _ensure_keys(self) -> None:
        if os.path.exists(self.key_file):
            d = json.load(open(self.key_file))
            self._priv = serialization.load_der_private_key(unb64(d["priv"]), None)
        else:
            self._priv = ec.generate_private_key(ec.SECP256R1())
            der = self._priv.private_bytes(serialization.Encoding.DER, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())
            self._pub_x509 = b""
            with open(self.key_file, "w") as f:
                json.dump({"priv": b64(der)}, f)
            try:
                os.chmod(self.key_file, 0o600)
            except OSError:
                pass
        self._pub_x509 = self._priv.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        self.kid = kid_of(self._pub_x509)
        pub = b64(self._pub_x509)
        self._commit([{"update": {"name": self._name(f"users/{self.uid}/keys/{self.kid}"), "fields": {"publicKey": _enc(pub)}}}])
        self._commit([{"update": {"name": self._name(f"users/{self.uid}"), "fields": {"publicKey": _enc(pub), "kid": _enc(self.kid)}},
                       "updateMask": {"fieldPaths": ["publicKey", "kid"]}}])

    def _peer_pub(self, uid: str, kid: str) -> bytes:
        k = f"{uid}:{kid}"
        if k not in self._peer_keys:
            d = self._get(f"users/{uid}/keys/{kid}")
            if not d:
                raise BotError("Нет публичного ключа собеседника")
            pub = unb64(d["publicKey"])
            if kid_of(pub) != kid:
                raise BotError("Ключ собеседника повреждён")
            self._peer_keys[k] = pub
        return self._peer_keys[k]

    def _peer_kid(self, uid: str) -> str:
        d = self._get(f"users/{uid}") or {}
        kid = d.get("kid") or ""
        if not kid:
            raise BotError("У собеседника нет ключа шифрования")
        return kid

    def _group_key(self, chat_id: str) -> bytes:
        if chat_id not in self._group_keys:
            d = self._get(f"chats/{chat_id}/keys/{self.uid}")
            if not d:
                raise BotError("Ключ группы для бота ещё не выдан (владелец должен добавить бота в группу)")
            key = pair_key(self._priv, self._peer_pub(d["by"], d["bkid"]), "gk:" + chat_id)
            self._group_keys[chat_id] = open_(key, unb64(d["wrapped"]), f"gk:{chat_id}:{self.uid}".encode())
        return self._group_keys[chat_id]

    # ------------------------------------------------------------------ encrypt / decrypt
    def _decrypt(self, chat: dict, msg: dict) -> dict:
        aad = chat["_id"].encode()
        if chat.get("type") == "DIRECT":
            peer = next(m for m in chat["members"] if m != self.uid)
            from_me = msg["senderId"] == self.uid
            my_kid, peer_kid = (msg["a"], msg["b"]) if from_me else (msg["b"], msg["a"])
            if my_kid != self.kid:
                raise BotError("сообщение зашифровано другим ключом")
            key = pair_key(self._priv, self._peer_pub(peer, peer_kid), chat["_id"])
        else:
            key = self._group_key(chat["_id"])
        return json.loads(open_(key, unb64(msg["ct"]), aad))

    def _encrypt(self, chat: dict, content: dict) -> dict:
        plain = json.dumps(content, ensure_ascii=False).encode()
        aad = chat["_id"].encode()
        if chat.get("type") == "DIRECT":
            peer = next(m for m in chat["members"] if m != self.uid)
            peer_kid = self._peer_kid(peer)
            key = pair_key(self._priv, self._peer_pub(peer, peer_kid), chat["_id"])
            return {"ct": b64(seal(key, plain, aad)), "a": self.kid, "b": peer_kid}
        return {"ct": b64(seal(self._group_key(chat["_id"]), plain, aad)), "a": "", "b": ""}

    # ------------------------------------------------------------------ public API
    def chats(self) -> List[dict]:
        return self._query("", {
            "from": [{"collectionId": "chats"}],
            "where": {"fieldFilter": {"field": {"fieldPath": "members"}, "op": "ARRAY_CONTAINS", "value": _enc(self.uid)}},
        })

    def _chat(self, chat_id: str) -> dict:
        d = self._get(f"chats/{chat_id}")
        if not d:
            raise BotError("Чат не найден")
        return d

    def _send_content(self, chat_id: str, content: dict) -> None:
        chat = self._chat(chat_id)
        env = self._encrypt(chat, content)
        now = int(time.time() * 1000)
        msg_id = uuid.uuid4().hex[:20]
        self._commit([
            {"update": {"name": self._name(f"chats/{chat_id}/messages/{msg_id}"), "fields": {
                "senderId": _enc(self.uid), "ct": _enc(env["ct"]), "a": _enc(env["a"]), "b": _enc(env["b"]),
                "ts": _enc(now), "readBy": _enc([self.uid]), "deleted": _enc(False)}},
             "currentDocument": {"exists": False}},
            {"update": {"name": self._name(f"chats/{chat_id}"), "fields": {
                "lastEnv": _enc({"ct": env["ct"], "a": env["a"], "b": env["b"]}), "lastMsgId": _enc(msg_id),
                "lastSenderId": _enc(self.uid), "lastAt": _enc(now), "readers": _enc([self.uid])}},
             "updateMask": {"fieldPaths": ["lastEnv", "lastMsgId", "lastSenderId", "lastAt", "readers"]}},
        ])

    def send(self, chat_id: str, text: str) -> None:
        self._send_content(chat_id, {"k": "text", "t": text, "p": "", "e": "", "w": 0, "h": 0})

    def send_image(self, chat_id: str, data: bytes, width: int = 0, height: int = 0, sticker: bool = False) -> None:
        """Encrypts a picture (JPEG/PNG/WebP, < ~600 KB) and sends it."""
        if len(data) > 600_000:
            raise BotError("Картинка слишком большая (максимум ~600 КБ)")
        key = os.urandom(32)
        mid = str(uuid.uuid4())
        self._commit([{"update": {"name": self._name(f"media/{mid}"), "fields": {
            "chatId": _enc(chat_id), "by": _enc(self.uid), "ts": _enc(int(time.time() * 1000)), "ct": _enc(seal(key, data))}},
            "currentDocument": {"exists": False}}])
        self._send_content(chat_id, {"k": "sticker" if sticker else "image", "t": "", "p": "m:" + mid, "e": b64(key), "w": width, "h": height})

    def download_image(self, ctx: Context) -> bytes:
        c = ctx.raw
        d = self._get("media/" + c["p"].removeprefix("m:"))
        if not d:
            raise BotError("Файл не найден")
        return open_(unb64(c["e"]), d["ct"])

    # ------------------------------------------------------------------ main loop
    def _poll_chat(self, chat: dict) -> None:
        cid = chat["_id"]
        last = self._seen.setdefault(cid, self._start)
        msgs = self._query(f"chats/{cid}", {
            "from": [{"collectionId": "messages"}],
            "where": {"fieldFilter": {"field": {"fieldPath": "ts"}, "op": "GREATER_THAN", "value": _enc(last)}},
            "orderBy": [{"field": {"fieldPath": "ts"}, "direction": "ASCENDING"}],
            "limit": 50,
        })
        for m in msgs:
            self._seen[cid] = max(self._seen[cid], m["ts"])
            if m.get("senderId") == self.uid or m.get("deleted"):
                continue
            try:
                c = self._decrypt(chat, m)
            except Exception as e:                                   # pragma: no cover
                print(f"[{cid}] не удалось расшифровать сообщение: {e}")
                continue
            ctx = Context(self, chat, m["senderId"], c.get("t", ""), c.get("k", "text"), c)
            try:
                fn = self._commands.get(ctx.command or "") if ctx.kind == "text" else None
                if fn:
                    fn(ctx)
                else:
                    for h in self._handlers:
                        h(ctx)
            except Exception as e:
                print("Ошибка в обработчике:", repr(e))

    def run(self) -> None:
        self._login()
        self._ensure_keys()
        print(f"Бот @{self.username} запущен (uid {self.uid}, ключ {self.kid}). Ctrl+C - остановить.")
        while True:
            try:
                for chat in self.chats():
                    self._poll_chat(chat)
            except KeyboardInterrupt:
                raise
            except Exception as e:
                print("Ошибка опроса:", repr(e))
                time.sleep(5)
            time.sleep(self.poll)
