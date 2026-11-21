package com.bliss.blisschat.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.data.push.Notifications
import com.bliss.blisschat.data.update.UpdateState
import com.bliss.blisschat.ui.chat.ImageCache
import com.bliss.blisschat.ui.components.BlissBackdrop
import com.bliss.blisschat.ui.components.BouncyButton
import com.bliss.blisschat.ui.components.OutlinedPillButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun hueColor(h: Float) = Color.hsv(h.coerceIn(0f, 359.9f), 0.78f, 0.95f)
private val Rainbow = Brush.horizontalGradient((0..12).map { hueColor(it * 30f) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onOpenAdmin: () -> Unit, onOpenPlugins: () -> Unit, onOpenStickers: () -> Unit, onOpenModeration: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = container.factory())
    val s by vm.settings.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val muted by vm.muted.collectAsStateWithLifecycle()
    val upd by vm.updates.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        BlissBackdrop(1)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("настройки", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(16.dp))

            /* ---------------------------------------------------------- appearance */
            Section("Внешний вид")
            Card {
                SwitchRow(Icons.Filled.DarkMode, "Тёмная тема", s.dark) { vm.setDark(it) }
                Spacer(Modifier.height(14.dp))
                Text("Цвет приложения", style = MaterialTheme.typography.titleMedium)
                Text("Меняет акценты и свечение фона", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(hueColor(s.hue)))
                    Spacer(Modifier.width(14.dp))
                    Slider(
                        value = s.hue, onValueChange = { vm.setHue(it) }, valueRange = 0f..360f, modifier = Modifier.weight(1f),
                        track = { Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(Rainbow)) },
                        thumb = { Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White).padding(5.dp).clip(CircleShape).background(hueColor(s.hue))) }
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TextFields, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Размер текста в чатах", style = MaterialTheme.typography.bodyLarge)
                        Slider(value = s.textScale, onValueChange = { vm.setTextScale(it) }, valueRange = 0.8f..1.4f)
                    }
                    Text("${(s.textScale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            }

            /* ------------------------------------------------------- notifications */
            Section("Уведомления")
            Card {
                SwitchRow(Icons.Filled.Notifications, "Сообщения", s.notifMessages) { vm.setNotifMessages(it) }
                SwitchRow(Icons.Filled.Call, "Звонки", s.notifCalls) { vm.setNotifCalls(it) }
                SwitchRow(Icons.Filled.Fullscreen, "Звонок на весь экран (на заблокированном телефоне)", s.notifFullScreenCalls) { vm.setNotifFullScreen(it) }
                if (Build.VERSION.SDK_INT >= 34 && s.notifFullScreenCalls && !Notifications.fullScreenAllowed(ctx)) {
                    Text(
                        "Android блокирует полноэкранные звонки. Разрешите их для blisschat.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)
                    )
                    TextButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }) { Text("Открыть разрешение") }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsOff, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Без уведомлений", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Отключить собеседника или чат можно в меню чата (⋮ → «Выключить уведомления»).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                if (muted.isEmpty()) {
                    Text("Список пуст: уведомления приходят от всех.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                muted.forEach { m ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text((if (m.key.startsWith("u:")) "👤 " else "💬 ") + m.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.unmute(m.key) }) { Text("Включить") }
                    }
                }
            }

            /* ------------------------------------------------------ privacy & data */
            Section("Приватность и данные")
            Card {
                SwitchRow(Icons.Filled.RemoveRedEye, "Скрывать статус «в сети»", s.hideOnline) { vm.setHideOnline(it) }
                SwitchRow(Icons.Filled.Image, "Автоматически загружать фото", s.autoLoadPhotos) { vm.setAutoLoadPhotos(it) }
                SwitchRow(Icons.Filled.Lock, "Запомнить меня", s.rememberMe) { vm.setRememberMe(it) }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { ImageCache.clear() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CleaningServices, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Очистить кэш изображений", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Сообщения, фото и звонки шифруются на устройстве. Ключ хранится только на этом телефоне.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            /* ------------------------------------------------------------ extras */
            Section("Расширения")
            Card {
                NavRow(Icons.Filled.Extension, "Плагины", "Каталог и свои плагины", onOpenPlugins)
                NavRow(Icons.Filled.SentimentSatisfied, "Стикеры", "Свои наборы стикеров", onOpenStickers)
                if (user?.moderator == true || user?.isAdmin == true) NavRow(Icons.Filled.Verified, "Модерация плагинов", "Заявки на проверку", onOpenModeration)
                if (user?.isAdmin == true) NavRow(Icons.Filled.AdminPanelSettings, "Панель администратора", "Значки, группы, модераторы", onOpenAdmin)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedPillButton("Выйти", onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth())

            /* ------------------------------------------------------ update centre */
            Section("Центр обновлений")
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("blisschat ${vm.updates.currentVersionName}", style = MaterialTheme.typography.titleMedium)
                        Text("сборка ${vm.updates.currentVersionCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                when (val st = upd.state) {
                    is UpdateState.Checking -> { Text("Проверка…", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(6.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    is UpdateState.UpToDate -> Text("У вас последняя версия ✓", style = MaterialTheme.typography.bodyMedium)
                    is UpdateState.Failed -> Text(st.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    is UpdateState.Available -> {
                        Text("Доступна версия ${st.info.versionName} (сборка ${st.info.versionCode})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        if (st.info.changelog.isNotBlank()) Text(st.info.changelog, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    is UpdateState.Downloading -> {
                        Text("Загрузка обновления…", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        if (st.progress in 0f..1f) LinearProgressIndicator(progress = { st.progress }, Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    is UpdateState.ReadyToInstall -> Text(
                        if (st.needsPermission) "Разрешите установку из этого источника, затем нажмите «Установить»." else "Загрузка завершена.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    UpdateState.Idle -> Text("Проверка выполняется при каждом запуске.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (upd.lastChecked > 0) {
                    Text(
                        "Последняя проверка: " + SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(upd.lastChecked)),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                when (val st = upd.state) {
                    is UpdateState.Available -> BouncyButton("Скачать и установить", onClick = { vm.updates.download() }, modifier = Modifier.fillMaxWidth())
                    is UpdateState.ReadyToInstall -> BouncyButton("Установить", onClick = { vm.updates.install(st.info) }, modifier = Modifier.fillMaxWidth())
                    is UpdateState.Downloading, UpdateState.Checking -> Unit
                    else -> OutlinedPillButton("Проверить обновления", onClick = { vm.updates.check(silent = false) }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(130.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked, onChange)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
