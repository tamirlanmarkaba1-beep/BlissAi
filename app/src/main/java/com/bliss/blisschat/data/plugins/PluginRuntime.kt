package com.bliss.blisschat.data.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bliss.blisschat.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import kotlin.coroutines.resume
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One setting that a code plugin added to the "Плагины" settings page. */
data class PluginSetting(
    val pluginId: String, val pluginName: String, val id: String, val title: String,
    /** switch | slider | choice | text | button */
    val type: String, val description: String, val min: Float, val max: Float, val options: List<String>, val default: String
)

/** Look of the app that plugins may change. Colours are ARGB; null = keep the app's own colour. */
data class PluginStyle(val cornerScale: Float = 1f, val mine: Long? = null, val other: Long? = null, val chatBg: Long? = null)

/** A code plugin that is installed on this phone. */
data class CodePlugin(val id: String, val name: String)

/**
 * Runs JavaScript plugins.
 *
 * Every plugin lives in its own WebView that is completely cut off from the world: no network (all requests are blocked and
 * a Content-Security-Policy forbids everything but the inline script), no files, no access to the app or to chats.
 * The only way out is the small `bliss` object that this class injects:
 *
 *   bliss.settings.add({id, title, type: "switch"|"slider"|"choice"|"text"|"button", default, min, max, options, description})
 *   bliss.settings.get(id)                       -> current value (boolean / number / string)
 *   bliss.on("setting", function (id, value) {}) -> called when the user changes one of the plugin's settings
 *   bliss.app.setTheme({hue: 0..360 | -1, dark: true|false})
 *   bliss.app.setTextScale(0.8 .. 1.4)
 *   bliss.app.setStyle({cornerScale: 0..2, bubbleMine: "#RRGGBB", bubbleOther: "#RRGGBB", chatBackground: "#RRGGBB"})
 *   bliss.chat.onSend(function (text) { return newText })   -> changes every message before it is sent
 *   bliss.toast("text")
 */
class PluginRuntime(context: Context, private val settings: SettingsStore) {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val values = app.getSharedPreferences("plugin_values", Context.MODE_PRIVATE)
    private val meta = app.getSharedPreferences("plugin_meta", Context.MODE_PRIVATE)
    private val dir get() = File(app.filesDir, "plugin_code").apply { mkdirs() }
    private val views = HashMap<String, WebView>()

    private val _defs = MutableStateFlow<List<PluginSetting>>(emptyList())
    val definitions: StateFlow<List<PluginSetting>> = _defs.asStateFlow()
    private val _plugins = MutableStateFlow<List<CodePlugin>>(emptyList())
    val plugins: StateFlow<List<CodePlugin>> = _plugins.asStateFlow()
    private val styles = HashMap<String, PluginStyle>()
    private val sendHooks = HashSet<String>()
    private val _style = MutableStateFlow(PluginStyle())
    val style: StateFlow<PluginStyle> = _style.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init { main.post { restore() } }

    private fun restore() {
        dir.listFiles { f -> f.extension == "js" }.orEmpty().forEach { f ->
            val id = f.nameWithoutExtension
            start(id, meta.getString("name.$id", id).orEmpty(), f.readText())
        }
    }

    /** Saves the code and starts it. A plugin with the same id is replaced. */
    fun install(id: String, name: String, code: String) {
        require(code.length <= MAX_CODE) { "Код плагина больше 100 КБ" }
        File(dir, "${safe(id)}.js").writeText(code)
        meta.edit().putString("name.${safe(id)}", name).apply()
        main.post { start(safe(id), name, code) }
    }

    fun remove(id: String) {
        val pid = safe(id)
        File(dir, "$pid.js").delete()
        meta.edit().remove("name.$pid").apply()
        val ed = values.edit()
        values.all.keys.filter { it.startsWith("$pid.") }.forEach { ed.remove(it) }
        ed.apply()
        main.post { stop(pid) }
    }

    fun has(id: String) = File(dir, "${safe(id)}.js").exists()

    fun value(s: PluginSetting): String = values.getString("${s.pluginId}.${s.id}", null) ?: s.default

    fun setValue(s: PluginSetting, value: String) {
        values.edit().putString("${s.pluginId}.${s.id}", value).apply()
        main.post {
            views[s.pluginId]?.evaluateJavascript("window.__set(${JSONObject.quote(s.id)},${JSONObject.quote(value)})", null)
        }
    }

    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun mergeStyles() {
        var r = PluginStyle()
        for (st in styles.values) r = PluginStyle(
            if (st.cornerScale != 1f) st.cornerScale else r.cornerScale, st.mine ?: r.mine, st.other ?: r.other, st.chatBg ?: r.chatBg
        )
        _style.value = r
    }

    /** Lets plugins with an onSend hook rewrite the text (each gets 0.8 s, then the text goes on unchanged). */
    suspend fun transformOutgoing(text: String): String {
        var result = text
        val ids = withContext(Dispatchers.Main) { sendHooks.toList() }
        for (pid in ids) {
            val out = withTimeoutOrNull(800) {
                suspendCancellableCoroutine<String?> { cont ->
                    val wv = views[pid]
                    if (wv == null) { cont.resume(null); return@suspendCancellableCoroutine }
                    wv.evaluateJavascript("window.__send(${JSONObject.quote(result)})") { raw ->
                        val v = runCatching { JSONTokener(raw).nextValue() }.getOrNull()
                        if (cont.isActive) cont.resume(v as? String)
                    }
                }
            }
            if (!out.isNullOrEmpty()) result = out.take(4000)
        }
        return result
    }

    private fun color(s: String?): Long? {
        val t = s?.trim()?.removePrefix("#") ?: return null
        if (t.length != 6 || !t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return 0xFF000000L or t.toLong(16)
    }

    private fun stop(pid: String) {
        styles.remove(pid); sendHooks.remove(pid); mergeStyles()
        views.remove(pid)?.let { runCatching { it.stopLoading(); it.destroy() } }
        _defs.value = _defs.value.filter { it.pluginId != pid }
        _plugins.value = _plugins.value.filter { it.id != pid }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun start(pid: String, name: String, code: String) {
        stop(pid)
        val wv = WebView(app)
        with(wv.settings) {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            blockNetworkImage = true
            domStorageEnabled = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        wv.webViewClient = object : WebViewClient() {
            // Everything except the page we load ourselves is answered with an empty response.
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                if (request?.url?.toString()?.startsWith(BASE) == true && request.isForMainFrame) null
                else WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), "".byteInputStream())
        }
        wv.addJavascriptInterface(Bridge(pid, name), "__native")
        views[pid] = wv
        _plugins.value = _plugins.value.filter { it.id != pid } + CodePlugin(pid, name)

        val stored = JSONObject()
        values.all.forEach { (k, v) -> if (k.startsWith("$pid.") && v is String) stored.put(k.removePrefix("$pid."), v) }
        val html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
            "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src 'unsafe-inline'\"></head><body>" +
            "<script>" + BOOT.replace("__STORED__", stored.toString()) + "</script>" +
            "<script>try{\n" + code + "\n}catch(e){__native.call(JSON.stringify({t:'error',v:String(e)}))}</script></body></html>"
        wv.loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null)
    }

    private fun handle(pid: String, pname: String, o: JSONObject) {
        when (o.optString("t")) {
            "setting" -> {
                val d = o.optJSONObject("def") ?: return
                val id = d.optString("id").take(40)
                val type = d.optString("type")
                if (id.isEmpty() || type !in TYPES) return
                if (_defs.value.count { it.pluginId == pid } >= MAX_SETTINGS) return
                val opts = d.optJSONArray("options") ?: JSONArray()
                val def = PluginSetting(
                    pid, pname, id, d.optString("title", id).take(60), type, d.optString("description").take(200),
                    d.optDouble("min", 0.0).toFloat(), d.optDouble("max", 1.0).toFloat(),
                    (0 until opts.length()).map { opts.optString(it).take(40) }.take(20),
                    d.opt("default")?.toString().orEmpty().take(200)
                )
                _defs.value = _defs.value.filter { !(it.pluginId == pid && it.id == id) } + def
            }
            "theme" -> {
                val t = o.optJSONObject("o") ?: return
                scope.launch {
                    if (t.has("hue")) settings.setHue(t.optDouble("hue").toFloat())
                    if (t.has("dark")) settings.setDark(t.optBoolean("dark"))
                }
            }
            "style" -> {
                val t = o.optJSONObject("o") ?: return
                styles[pid] = PluginStyle(
                    t.optDouble("cornerScale", 1.0).toFloat().coerceIn(0f, 2f),
                    color(t.optString("bubbleMine")), color(t.optString("bubbleOther")), color(t.optString("chatBackground"))
                )
                mergeStyles()
            }
            "hook" -> if (o.optString("v") == "send") sendHooks.add(pid)
            "scale" -> scope.launch { settings.setTextScale(o.optDouble("v", 1.0).toFloat()) }
            "toast" -> _messages.tryEmit("$pname: ${o.optString("v").take(200)}")
            "error" -> _messages.tryEmit("Ошибка плагина «$pname»: ${o.optString("v").take(200)}")
        }
    }

    private inner class Bridge(val pid: String, val pname: String) {
        @JavascriptInterface
        fun call(json: String) {
            if (json.length > 20_000) return
            val o = runCatching { JSONObject(json) }.getOrNull() ?: return
            main.post { handle(pid, pname, o) }
        }
    }

    companion object {
        const val MAX_CODE = 100 * 1024
        private const val MAX_SETTINGS = 30
        private const val BASE = "https://plugin.invalid/"
        private val TYPES = setOf("switch", "slider", "choice", "text", "button")

        private const val BOOT = """
(function () {
  var R = __STORED__, V = {}, D = {}, H = {};
  function send(o) { __native.call(JSON.stringify(o)); }
  function cv(d, raw) {
    if (raw === undefined || raw === null) raw = (d.default === undefined ? '' : String(d.default));
    if (d.type === 'switch') return raw === 'true';
    if (d.type === 'slider') { var n = parseFloat(raw); return isNaN(n) ? (d.min || 0) : n; }
    return raw;
  }
  window.bliss = {
    settings: {
      add: function (d) { D[d.id] = d; V[d.id] = cv(d, R[d.id]); send({t: 'setting', def: d}); },
      get: function (id) { return V[id]; }
    },
    app: {
      setTheme: function (o) { send({t: 'theme', o: o}); },
      setTextScale: function (x) { send({t: 'scale', v: x}); },
      setStyle: function (o) { send({t: 'style', o: o}); }
    },
    chat: {
      onSend: function (fn) { (H['send'] = H['send'] || []).push(fn); send({t: 'hook', v: 'send'}); }
    },
    toast: function (s) { send({t: 'toast', v: String(s)}); },
    on: function (ev, fn) { (H[ev] = H[ev] || []).push(fn); }
  };
  window.__send = function (t) {
    var r = t;
    (H['send'] || []).forEach(function (f) { try { var x = f(r); if (typeof x === 'string') r = x; } catch (e) {} });
    return r;
  };
  window.__set = function (id, raw) {
    var d = D[id]; if (!d) return;
    V[id] = d.type === 'button' ? true : cv(d, raw);
    (H['setting'] || []).forEach(function (f) { try { f(id, V[id]); } catch (e) {} });
  };
})();
"""
    }
}
