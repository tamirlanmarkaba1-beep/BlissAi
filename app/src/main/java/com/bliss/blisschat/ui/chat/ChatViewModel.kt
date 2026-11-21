package com.bliss.blisschat.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.Session
import com.bliss.blisschat.data.AppSettings
import com.bliss.blisschat.data.Chat
import com.bliss.blisschat.data.MemberStat
import com.bliss.blisschat.data.TagStep
import com.bliss.blisschat.data.ChatType
import com.bliss.blisschat.data.Content
import com.bliss.blisschat.data.Message
import com.bliss.blisschat.data.UiMessage
import com.bliss.blisschat.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val chat: Chat? = null,
    val chatLoaded: Boolean = false,
    val messages: List<UiMessage> = emptyList(),
    val messagesLoaded: Boolean = false,
    val users: Map<String, UserProfile> = emptyMap(),
    val me: UserProfile? = null,
    val stats: Map<String, MemberStat> = emptyMap()
) {
    val myId get() = me?.uid.orEmpty()
    val isMember get() = chat?.members?.contains(myId) == true
    val isAdmin get() = me?.isAdmin == true
    val isOwner get() = chat != null && chat.ownerId == myId
    /** Group or channel (anything that is not a direct chat). */
    val isGroup get() = chat != null && chat.type != ChatType.DIRECT
    val isChannel get() = chat?.type == ChatType.CHANNEL
    /** In a channel only the owner publishes. */
    val canPost get() = isMember && (!isChannel || isOwner)
    val other: UserProfile? get() = chat?.peerOf(myId)?.let { users[it] }
    val canDeleteGroup get() = isGroup && (isOwner || isAdmin)
    val canLeave get() = isGroup && isMember && !isOwner
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val c: AppContainer, private val chatId: String) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    val repo get() = c.repo

    private val me: StateFlow<UserProfile?> = c.session.map { (it as? Session.Ready)?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private data class ChatBox(val chat: Chat?, val loaded: Boolean)
    private data class MsgBox(val list: List<Message>, val loaded: Boolean)

    private val chatFlow = c.repo.observeChat(chatId).map { ChatBox(it, true) }.catch { emit(ChatBox(null, true)) }
    private val msgFlow = c.repo.observeMessages(chatId).map { MsgBox(it, true) }
        .catch { _events.tryEmit(it.message ?: "Не удалось загрузить сообщения"); emit(MsgBox(emptyList(), true)) }

    private val users = combine(chatFlow, msgFlow) { ch, m ->
        buildSet<String> {
            ch.chat?.let { addAll(it.members) }
            m.list.forEach { add(it.senderId) }
        }.filter { it.isNotBlank() }.toSet()
    }.distinctUntilChanged().flatMapLatest { c.repo.observeUsers(it) }.catch { emit(emptyMap()) }

    private val decryptCache = HashMap<String, UiMessage>()

    private val decrypted = combine(chatFlow, msgFlow, me, c.settings.settings.map { it.hidden }.distinctUntilChanged()) { ch, m, user, hidden ->
        Triple(ch.chat, m.list.filter { "$chatId/${it.id}" !in hidden }, user)
    }.mapLatest { (chat, list, user) ->
        var keyMissing = false
        list.map { msg ->
            val key = "${msg.id}:${msg.env.ct.length}:${msg.deleted}"
            decryptCache[key] ?: run {
                if (msg.deleted) UiMessage(msg, null, false).also { decryptCache[key] = it }
                else if (chat == null || user == null || keyMissing) UiMessage(msg, null, true)
                else try {
                    UiMessage(msg, c.e2ee.decrypt(chat, user.uid, msg.senderId, msg.env), false).also { decryptCache[key] = it }
                } catch (e: Exception) {
                    if (chat.type != ChatType.DIRECT) keyMissing = true
                    UiMessage(msg, null, true)
                }
            }
        }
    }

    private val stats = chatFlow.map { it.chat?.type }.distinctUntilChanged().flatMapLatest { t ->
        if (t == null || t == ChatType.DIRECT) flowOf(emptyMap<String, MemberStat>()) else c.repo.observeStats(chatId)
    }.catch { emit(emptyMap()) }

    private val base = combine(chatFlow, msgFlow, decrypted, users, me) { ch, m, d, u, user ->
        ChatUiState(ch.chat, ch.loaded, d, m.loaded, u, user)
    }

    val state: StateFlow<ChatUiState> = combine(base, stats) { b, st -> b.copy(stats = st) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** True while the chat screen is on screen; used to count time spent in a group (for rank tags). */
    @Volatile private var active = false
    fun setActive(value: Boolean) { active = value }

    init {
        // Time in the group: +1 minute for every minute the chat is open
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val s = state.value
                if (active && s.isGroup && s.isMember && s.myId.isNotEmpty()) c.repo.touchStats(chatId, s.myId, 1, 0)
            }
        }
        // Read receipts
        viewModelScope.launch {
            state.map { s -> Triple(s.chat, s.myId, s.messages.count { s.myId !in it.message.readBy && !it.message.deleted }) }
                .distinctUntilChanged { a, b -> a.first?.lastMsgId == b.first?.lastMsgId && a.first?.readers == b.first?.readers && a.third == b.third }
                .collectLatest { (chat, myId, _) ->
                    val s = state.value
                    if (chat != null && s.isMember && myId.isNotEmpty()) c.repo.markRead(chat, myId, s.messages.map { it.message })
                }
        }
    }

    fun sendText(text: String, onFailure: (String) -> Unit) {
        val s = state.value
        val chat = s.chat ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            c.repo.send(chat, s.myId, Content(Content.KIND_TEXT, text = body))
                .onSuccess { if (chat.type != ChatType.DIRECT) c.repo.touchStats(chatId, s.myId, 0, 1) }
                .onFailure { _events.tryEmit("Не отправлено: ${it.message}"); onFailure(text) }
        }
    }

    fun sendImage(uri: Uri) {
        val s = state.value
        val chat = s.chat ?: return
        viewModelScope.launch {
            _sending.value = true
            c.repo.sendImage(chat, s.myId, uri).onFailure { _events.tryEmit("Фото не отправлено: ${it.message}") }
            _sending.value = false
        }
    }

    fun sendSticker(file: java.io.File) {
        val s = state.value
        val chat = s.chat ?: return
        viewModelScope.launch {
            _sending.value = true
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = file.readBytes()
                    val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                    Triple(bytes, o.outWidth, o.outHeight)
                }
            }
            r.onSuccess { (bytes, w, h) ->
                c.repo.sendSticker(chat, s.myId, bytes, w, h).onFailure { _events.tryEmit("Стикер не отправлен: ${it.message}") }
            }.onFailure { _events.tryEmit("Не удалось прочитать стикер") }
            _sending.value = false
        }
    }

    /** Mutes / unmutes notifications of this chat (or of the other person in a direct chat). */
    fun toggleMute() {
        val s = state.value
        val chat = s.chat ?: return
        val key = if (chat.type == ChatType.DIRECT) "u:${chat.peerOf(s.myId).orEmpty()}" else "c:${chat.id}"
        val muted = key in settings.value.muted
        viewModelScope.launch { c.settings.setMuted(key, !muted) }
    }

    fun isMuted(chat: Chat?, myId: String, muted: Set<String>): Boolean =
        chat != null && (if (chat.type == ChatType.DIRECT) "u:${chat.peerOf(myId).orEmpty()}" in muted else "c:${chat.id}" in muted)

    fun setSelfTag(tag: String) {
        val s = state.value
        viewModelScope.launch { c.repo.setSelfTag(chatId, s.myId, tag).onFailure { _events.tryEmit(it.message ?: "Не удалось сохранить тег") } }
    }

    fun updateTags(creatorTags: Boolean, selfTags: Boolean, ladder: List<TagStep>) {
        viewModelScope.launch { c.repo.updateTagSettings(chatId, creatorTags, selfTags, ladder).onFailure { _events.tryEmit(it.message ?: "Не удалось сохранить") } }
    }

    fun react(m: UiMessage, emoji: String) {
        val s = state.value
        val mine = m.message.reactions[s.myId]
        viewModelScope.launch {
            c.repo.react(chatId, m.message.id, s.myId, if (mine == emoji) null else emoji)
                .onFailure { _events.tryEmit(it.message ?: "Не удалось поставить реакцию") }
        }
    }

    fun deleteForEveryone(m: UiMessage) {
        val s = state.value
        val chat = s.chat ?: return
        if (m.message.senderId != s.myId) return
        viewModelScope.launch {
            c.repo.deleteForEveryone(chat, m.message, m.content?.path)
                .onFailure { _events.tryEmit(it.message ?: "Не удалось удалить") }
        }
    }

    fun deleteLocally(m: UiMessage) { viewModelScope.launch { c.settings.hide(chatId, m.message.id) } }

    fun call(video: Boolean) {
        val s = state.value
        val peer = s.other ?: return
        c.calls.startCall(chatId, peer, video)
    }

    fun hasCallPermissions(video: Boolean) = c.calls.hasPermissions(video)

    /* ---- groups */

    fun addMember(username: String, onDone: (Result<Unit>) -> Unit) {
        val s = state.value
        val chat = s.chat ?: return
        viewModelScope.launch { onDone(c.repo.addMember(chat, s.myId, username)) }
    }

    fun leave(onDone: () -> Unit) {
        viewModelScope.launch {
            c.repo.leave(chatId, state.value.myId).onSuccess { onDone() }.onFailure { _events.tryEmit(it.message ?: "Не удалось выйти") }
        }
    }

    fun deleteGroup(onDone: () -> Unit) {
        val actor = me.value ?: return
        viewModelScope.launch {
            val r = if (actor.isAdmin) c.repo.adminDeleteChat(actor, chatId) else c.repo.deleteChat(chatId)
            r.onSuccess { onDone() }.onFailure { _events.tryEmit(it.message ?: "Не удалось удалить") }
        }
    }

    fun setGroupVerified(value: Boolean) {
        val actor = me.value?.takeIf { it.isAdmin } ?: return
        viewModelScope.launch {
            c.repo.setGroupVerified(actor, chatId, value).onFailure { _events.tryEmit(it.message ?: "Ошибка") }
        }
    }
}
