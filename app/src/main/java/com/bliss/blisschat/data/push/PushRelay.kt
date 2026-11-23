package com.bliss.blisschat.data.push

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tells the Cloudflare Worker in /cloudflare-worker that a message was sent / a call was started, so it can send
 * the FCM push. This replaces Firebase Cloud Functions (which need the paid Blaze plan).
 *
 * Fire-and-forget: a failure here never affects sending the message.
 */
object PushRelay {
    /** Address of the deployed Cloudflare Worker (see /cloudflare-worker). Empty = pushes off. */
    const val WORKER_URL = "https://blisschat-push.blisschat-relay.workers.dev"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun notifyMessage(chatId: String, msgId: String) =
        post("/message", JSONObject().put("chatId", chatId).put("msgId", msgId))

    fun notifyCall(callId: String) =
        post("/call", JSONObject().put("callId", callId))

    private fun post(path: String, body: JSONObject) {
        if (WORKER_URL.isBlank()) return
        scope.launch {
            runCatching {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@runCatching
                val c = URL(WORKER_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
                try {
                    c.requestMethod = "POST"
                    c.connectTimeout = 8_000
                    c.readTimeout = 8_000
                    c.doOutput = true
                    c.setRequestProperty("Authorization", "Bearer $token")
                    c.setRequestProperty("Content-Type", "application/json")
                    c.outputStream.use { it.write(body.toString().toByteArray()) }
                    c.responseCode // send the request and wait for the answer
                } finally {
                    c.disconnect()
                }
            }
        }
    }
}
