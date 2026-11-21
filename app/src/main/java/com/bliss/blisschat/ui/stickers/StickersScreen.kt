package com.bliss.blisschat.ui.stickers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.data.stickers.StickerPack
import com.bliss.blisschat.ui.components.BlissHeader
import com.bliss.blisschat.ui.components.BouncyButton
import com.bliss.blisschat.ui.components.PillTextField
import com.bliss.blisschat.ui.components.bounceClick
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun StickersScreen(container: AppContainer, onBack: () -> Unit) {
    val packs by container.stickers.packs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<StickerPack?>(null) }
    var deleting by remember { mutableStateOf<StickerPack?>(null) }
    var removing by remember { mutableStateOf<File?>(null) }
    var target by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        val id = target ?: return@rememberLauncherForActivityResult
        scope.launch {
            var failed = 0
            uris.forEach { u -> container.stickers.addFromUri(id, u).onFailure { failed++ } }
            if (failed > 0) snackbar.showSnackbar("Не удалось добавить: $failed")
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            BlissHeader {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                    Text("Стикеры", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Добавляйте картинки в набор (PNG с прозрачным фоном выглядят лучше всего). Нажмите на стикер и удерживайте, чтобы удалить.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BouncyButton("Новый набор", onClick = { creating = true }, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
                packs.forEach { pack ->
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pack.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { renaming = pack }) { Icon(Icons.Filled.Edit, "Переименовать") }
                                IconButton(onClick = { deleting = pack }) { Icon(Icons.Filled.Delete, "Удалить набор", tint = MaterialTheme.colorScheme.error) }
                            }
                            Spacer(Modifier.size(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pack.stickers.forEach { f ->
                                    Box(
                                        Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            .combinedClickable(onClick = {}, onLongClick = { removing = f }),
                                        contentAlignment = Alignment.Center
                                    ) { StickerImage(f, 68.dp) }
                                }
                                Box(
                                    Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                                        .bounceClick {
                                            target = pack.id
                                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.Add, "Добавить стикеры", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }

    if (creating) NameDialog("Новый набор", "", "Создать", { creating = false }) { n ->
        container.stickers.createPack(n.trim()); creating = false
    }
    renaming?.let { p -> NameDialog("Переименовать", p.name, "Сохранить", { renaming = null }) { n -> container.stickers.rename(p.id, n.trim()); renaming = null } }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Удалить набор «${p.name}»?") },
            text = { Text("Все стикеры набора будут удалены с этого устройства.") },
            confirmButton = { TextButton(onClick = { container.stickers.deletePack(p.id); deleting = null }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } }
        )
    }
    removing?.let { f ->
        AlertDialog(
            onDismissRequest = { removing = null }, title = { Text("Удалить стикер?") },
            confirmButton = { TextButton(onClick = { container.stickers.removeSticker(f); removing = null }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { PillTextField(name, { name = it.take(30) }, "Название") },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onOk(name) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
