package com.blisskeyboard

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.graphics.drawable.GradientDrawable

class SettingsActivity: Activity() {
    private val bg = 0xff202728.toInt(); private val card = 0xff2d393a.toInt(); private val teal = 0xffbfe9e7.toInt()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); build() }
    private fun build(){
        val root=LinearLayout(this).apply{orientation=1;setPadding(20,24,20,24);setBackgroundColor(bg)}
        root.addView(TextView(this).apply{text="Bliss Keyboard";textSize=28f;setTextColor(teal);setPadding(4,8,4,18)})
        root.addView(TextView(this).apply{text="Material You • Version 1.0";textSize=14f;setTextColor(0xffd0dddd);setPadding(4,0,4,20)})
        root.addView(card("Проверить клавиатуру","Открой тестовое поле и попробуй Bliss Keyboard") { val e=EditText(this);e.hint="Здесь можно проверить клавиатуру";e.setTextColor(Color.WHITE);e.setHintTextColor(teal);AlertDialog.Builder(this).setTitle("Тест клавиатуры").setView(e).setPositiveButton("Закрыть",null).show() })
        root.addView(card("Настроить клавиатуру по умолчанию","Открыть системный выбор клавиатуры") { startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS")) })
        root.addView(card("Языки","Выбрать язык раскладки") { languageDialog() })
        root.addView(card("Цвет RGB","Настроить цвет клавиш") { colorDialog() })
        root.addView(card("Автозамена","Включить / выключить простые исправления") { val p=getSharedPreferences("bliss",0);val v=!p.getBoolean("autocorrect",true);p.edit().putBoolean("autocorrect",v).apply();Toast.makeText(this,"Автозамена: ${if(v)"включена" else "выключена"}",Toast.LENGTH_SHORT).show() })
        root.addView(card("Буфер обмена","Сохранённые тексты и последние копирования") { clipboardDialog() })
        root.addView(card("Эмодзи","Открыть панель эмодзи") { emojiDialog() })
        root.addView(card("Открыть моё приложение","Переход к дополнительным настройкам") { val pkg=Prefs.get(this,"target_app");if(pkg.isNotBlank()){try{startActivity(packageManager.getLaunchIntentForPackage(pkg))}catch(_:Exception){Toast.makeText(this,"Приложение не найдено",0).show()}}else Toast.makeText(this,"Укажи package name в коде: target_app",Toast.LENGTH_LONG).show() })
        root.addView(TextView(this).apply{text="Bliss Keyboard не сохраняет ввод в полях пароля. Буфер хранится локально.";textSize=12f;setTextColor(0xffaabbbb);setPadding(8,20,8,0)})
        setContentView(ScrollView(this).apply{addView(root)})
    }
    private fun card(title:String,desc:String,action:()->Unit):View=LinearLayout(this).apply{orientation=1;setPadding(18,16,18,16);background=shape(card,30);setOnClickListener{action()};addView(TextView(this@SettingsActivity).apply{text=title;textSize=18f;setTextColor(teal)});addView(TextView(this@SettingsActivity).apply{text=desc;textSize=14f;setTextColor(0xffd2dfdf);setPadding(0,5,0,0)});layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12}}
    private fun shape(color:Int,r:Float=24f)=GradientDrawable().apply{setColor(color);cornerRadius=r}
    private fun languageDialog(){val locales=java.util.Locale.getAvailableLocales().map{it.language to (it.getDisplayName(it).ifBlank{it.language})}.filter{it.first.length in 2..3}.distinctBy{it.first}.sortedBy{it.second}.toTypedArray();AlertDialog.Builder(this).setTitle("Язык клавиатуры").setItems(locales.map{it.second}.toTypedArray()){_,i->Prefs.put(this,"lang",locales[i].first);Toast.makeText(this,"Выбран: ${locales[i].second}",0).show()}.show()}
    private fun colorDialog(){val box=LinearLayout(this).apply{orientation=1;setPadding(20,10,20,10)};val r=EditText(this).apply{hint="R 0–255";inputType=2};val g=EditText(this).apply{hint="G 0–255";inputType=2};val b=EditText(this).apply{hint="B 0–255";inputType=2};listOf(r,g,b).forEach{box.addView(it)};AlertDialog.Builder(this).setTitle("RGB цвет").setView(box).setPositiveButton("Применить"){_,_->try{val c=Color.rgb(r.text.toString().toInt(),g.text.toString().toInt(),b.text.toString().toInt());Prefs.put(this,"color",String.format("#%06X",0xffffff and c));Toast.makeText(this,"Цвет сохранён. Перезапусти клавиатуру.",0).show()}catch(_:Exception){Toast.makeText(this,"Введите значения 0–255",0).show()}}.setNegativeButton("Отмена",null).show()}
    private fun clipboardDialog(){val clips=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager;val text=clips.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty();AlertDialog.Builder(this).setTitle("Буфер обмена").setMessage(if(text.isBlank())"Буфер пуст" else text).setPositiveButton("Вставить в тест") {_,_->Toast.makeText(this,"Скопируй текст и вставь в поле",0).show()}.setNegativeButton("Закрыть",null).show()}
    private fun emojiDialog(){val emojis="😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😉 😊 😍 🥰 😎 🤔 😭 😡 👍 👎 ❤️ 💙 💚 💜 ✨ 🔥 🎉 🌸";AlertDialog.Builder(this).setTitle("Эмодзи").setItems(emojis.split(" ").toTypedArray()){_,i->val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("emoji",emojis.split(" ")[i]));Toast.makeText(this,"Эмодзи скопирован",0).show()}.show()}
}
