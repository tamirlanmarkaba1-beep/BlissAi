package com.bliss.blisschat.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.data.plugins.PluginSetting
import com.bliss.blisschat.ui.components.BouncyButton
import com.bliss.blisschat.ui.components.OutlinedPillButton
import com.bliss.blisschat.ui.components.PillTextField

/** Where bots can be kept running 24/7 (see the guide). */
const val BOTKEEP_URL = "https://botkeep.cloud"

/** "Плагины": settings that code plugins added + the list of installed code plugins. */
@Composable
fun PluginSettingsPage(container: AppContainer, onOpenCatalog: () -> Unit, onBack: () -> Unit) {
    val runtime = container.plugins.runtime
    val defs by runtime.definitions.collectAsStateWithLifecycle()
    val plugins by runtime.plugins.collectAsStateWithLifecycle()

    SettingsPage("плагины", onBack) {
        Spacer(Modifier.height(4.dp))
        BouncyButton("Каталог плагинов и свой код", onClick = onOpenCatalog, modifier = Modifier.fillMaxWidth())
        if (plugins.isEmpty()) {
            BlockLabel("пока пусто")
            SCard {
                Text(
                    "Плагины с кодом добавляют сюда свои настройки, меняют вид приложения и умеют менять текст перед отправкой. " +
                        "Установите плагин из каталога или запустите свой код.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        plugins.forEach { p ->
            BlockLabel(p.name)
            SCard {
                val mine = defs.filter { it.pluginId == p.id }
                if (mine.isEmpty()) Text("У этого плагина нет настроек", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                mine.forEach { d ->
                    PluginSettingRow(d, runtime.value(d)) { runtime.setValue(d, it) }
                    Spacer(Modifier.height(10.dp))
                }
                TextButton(onClick = { runtime.remove(p.id) }) { Text("Удалить плагин", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PluginSettingRow(d: PluginSetting, initial: String, onChange: (String) -> Unit) {
    var value by remember(d.pluginId, d.id) { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth()) {
        when (d.type) {
            "switch" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(value == "true", { value = it.toString(); onChange(value) })
            }
            "slider" -> {
                Text(d.title, style = MaterialTheme.typography.bodyLarge)
                val lo = d.min
                val hi = if (d.max > d.min) d.max else d.min + 1f
                Slider(
                    value = (value.toFloatOrNull() ?: lo).coerceIn(lo, hi),
                    onValueChange = { value = it.toString(); onChange(value) }, valueRange = lo..hi
                )
            }
            "choice" -> {
                Text(d.title, style = MaterialTheme.typography.bodyLarge)
                d.options.forEach { o ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(value == o, { value = o; onChange(o) })
                        Text(o, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            "text" -> {
                Text(d.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                PillTextField(value, { value = it; onChange(it) }, d.title)
            }
            "button" -> OutlinedPillButton(d.title, onClick = { onChange("click") }, modifier = Modifier.fillMaxWidth())
        }
        if (d.description.isNotBlank()) Text(d.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shows what plugins say (bliss.toast and script errors) as short toasts. Put it once into the main screen. */
@Composable
fun PluginMessages(container: AppContainer) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { container.plugins.runtime.messages.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
}

/** How to make bots and plugins. */
@Composable
fun GuidePage(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    SettingsPage("руководство", onBack) {
        Spacer(Modifier.height(4.dp))
        GroupLabel("боты")
        SCard {
            Text("Бот — это Python-скрипт", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            GuideText(
                "1. Нажмите «+» → «Создать бота», задайте имя и username. Вы получите токен вида  my_bot:TOKEN.\n" +
                    "2. Напишите скрипт (пример лежит в репозитории: bots/example_bot.py):"
            )
            Code(
                "from blisschat_bot import Bot\nimport os\n\nbot = Bot(os.environ[\"BLISS_BOT_TOKEN\"])\n\n" +
                    "@bot.command(\"start\")\ndef start(ctx):\n    ctx.reply(\"Привет!\")\n\n" +
                    "@bot.command(\"echo\")\ndef echo(ctx):\n    ctx.reply(\" \".join(ctx.args))\n\nbot.run()"
            )
            GuideText(
                "3. Код бота можно хранить прямо в приложении: Настройки → Боты (Python) → «Код».\n" +
                    "4. Скрипт должен работать постоянно. Для этого нужен сервер: botkeep — хостинг для ботов " +
                    "(есть бесплатный тариф), там скрипт работает 24/7 без вашего телефона."
            )
            Spacer(Modifier.height(8.dp))
            BouncyButton("Открыть botkeep", onClick = { uri.openUri(BOTKEEP_URL) }, modifier = Modifier.fillMaxWidth())
        }
        GroupLabel("плагины")
        SCard {
            Text("Плагин — это JavaScript", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            GuideText(
                "Скрипт работает в изолированной песочнице: без интернета, без файлов и без доступа к чатам. " +
                    "Ему доступен только объект bliss:"
            )
            Code(
                "bliss.settings.add({id, title, type, default, min, max, options, description})\n" +
                    "   type: switch | slider | choice | text | button\n" +
                    "bliss.settings.get(id)\n" +
                    "bliss.on(\"setting\", function (id, value) {})\n\n" +
                    "bliss.app.setTheme({hue: 0..360, dark: true})\n" +
                    "bliss.app.setTextScale(0.8..1.4)\n" +
                    "bliss.app.setStyle({\n  cornerScale: 0..2,\n  bubbleMine: \"#RRGGBB\",\n  bubbleOther: \"#RRGGBB\",\n  chatBackground: \"#RRGGBB\"\n})\n\n" +
                    "bliss.chat.onSend(function (text) {\n  return text + \" ✨\";\n})\n\n" +
                    "bliss.toast(\"текст\")"
            )
            GuideText(
                "Свой плагин можно сразу запустить у себя: Настройки → Плагины → «Каталог плагинов и свой код» → «Создать и протестировать». " +
                    "Чтобы поделиться им, упакуйте в .zip файлы manifest.json  {\"name\":\"…\",\"version\":\"1.0\",\"type\":\"code\"}  и main.js, " +
                    "отправьте архив на проверку. Модератор одобрит его, и плагин появится в каталоге."
            )
        }
        Spacer(Modifier.width(1.dp))
    }
}

@Composable
private fun GuideText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun Code(text: String) {
    androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
    }
}
