package com.blisskeyboard

import android.inputmethodservice.InputMethodService
import android.view.*
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.*

class BlissImeService : InputMethodService() {
    private var caps = true
    private var symbols = false
    private var emojiMode = false
    private var lang = "en"
    private var suggestions: LinearLayout? = null
    private val teal = 0xffbfe9e7.toInt()
    private val dark = 0xff354c4b.toInt()
    private val blue = 0xff455f82.toInt()

    override fun onCreateInputView(): View {
        lang = Prefs.get(this, "lang", "en")
        return makeKeyboard()
    }

    private fun bg(c: Int, r: Float = 18f) = GradientDrawable().apply { setColor(c); cornerRadius = r }

    private fun button(text: String, c: Int = teal, tc: Int = 0xff102626.toInt(), size: Float = 20f): TextView = TextView(this).apply {
        textSize = size
        this.text = text
        setTextColor(tc)
        gravity = Gravity.CENTER
        background = bg(c)
        setPadding(4, 4, 4, 4)
        isClickable = true
        isFocusable = true
    }

    private fun makeKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 8, 6, 8)
            background = bg(dark, 28f)
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("⌨", "▢", "☺", "⚙").forEachIndexed { idx, s ->
            val b = button(s, if (idx == 0) blue else dark, teal, 24f)
            toolbar.addView(b, LinearLayout.LayoutParams(0, 52, 1f))
            b.setOnClickListener {
                when (idx) {
                    1 -> clipboard()
                    2 -> showEmoji()
                    3 -> startActivity(Intent(this@BlissImeService, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        root.addView(toolbar)

        val cand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(2, 8, 2, 8) }
        suggestions = cand
        listOf(" ", " ", " ").forEach { cand.addView(button("слово", teal, 0xff142626.toInt(), 16f), LinearLayout.LayoutParams(0, 44, 1f)) }
        root.addView(cand)

        val rows = if (emojiMode) {
            listOf("😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😉 😊 😍 🥰 😎 🤔 😭 😡 👍 👎 ❤️ 💙 💚 💜 ✨ 🔥 🎉 🌸".split(" "))
        } else if (symbols) {
            listOf("1234567890".map { it.toString() }, "@#$%&-*+()".map { it.toString() }, "=/:;!?\'.".map { it.toString() })
        } else if (lang == "ru") {
            listOf("йцукенгшщзх".map { it.toString() }, "фывапролджэ".map { it.toString() }, "ячсмитьбю".map { it.toString() })
        } else {
            listOf("qwertyuiop".map { it.toString() }, "asdfghjkl".map { it.toString() }, "zxcvbnm".map { it.toString() })
        }

        rows.forEachIndexed { ri, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            row.forEach { key ->
                val k = button(if (caps && !symbols && !emojiMode) key.uppercase() else key, teal, 0xff102626.toInt(), 19f)
                line.addView(k, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(2, 3, 2, 3) })
                k.setOnClickListener {
                    if (emojiMode) commit(key) else commit(if (caps && !symbols) key.uppercase() else key)
                    caps = false
                }
            }
            if (ri == 2 && !symbols && !emojiMode) {
                val shift = button("⇧", blue, Color.WHITE)
                line.addView(shift, LinearLayout.LayoutParams(48, 48).apply { setMargins(2, 3, 2, 3) })
                shift.setOnClickListener { caps = !caps; refresh() }
                val del = button("⌫", blue, Color.WHITE)
                line.addView(del, LinearLayout.LayoutParams(48, 48).apply { setMargins(2, 3, 2, 3) })
                del.setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) }
            }
            root.addView(line)
        }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun add(t: String, w: Float, c: Int = blue, action: () -> Unit) {
            val v = button(t, c, Color.WHITE, 18f)
            bottom.addView(v, LinearLayout.LayoutParams(0, 50, w).apply { setMargins(2, 3, 2, 3) })
            v.setOnClickListener { action() }
        }
        add("?123", 1f) { symbols = !symbols; emojiMode = false; refresh() }
        add(",", 0.7f, teal) { commit(",") }
        add("🌐", 0.8f) { lang = if (lang == "ru") "en" else "ru"; Prefs.put(this, "lang", lang); refresh() }
        add("space", 3f, teal) { commit(" ") }
        add(".", 0.7f, teal) { commit(".") }
        add("↵", 1f) { currentInputConnection?.performEditorAction(currentInputEditorInfo?.actionId ?: 0) }
        root.addView(bottom)
        return root
    }

    private fun refresh() {
        setInputView(makeKeyboard())
    }

    private fun commit(s: String) {
        currentInputConnection?.commitText(s, 1)
    }

    private fun clipboard() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            commit(clip.getItemAt(0).coerceToText(this).toString())
        } else {
            Toast.makeText(this, "Буфер пуст", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmoji() {
        emojiMode = true
        refresh()
    }

    override fun onStartInput(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        caps = true
        emojiMode = false
    }
}
