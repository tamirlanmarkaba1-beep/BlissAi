package com.bliss.blisschat.ui.stickers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.ui.components.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val stickerCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

/** A sticker stored in the app's own folder. */
@Composable
fun StickerImage(file: File, size: Dp, modifier: Modifier = Modifier) {
    val key = file.absolutePath + file.lastModified()
    val bmp by produceState<Bitmap?>(stickerCache.get(key), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }?.also { stickerCache.put(key, it) }
        }
    }
    Box(modifier.height(size), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.height(size), contentScale = ContentScale.Fit) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPicker(container: AppContainer, onPick: (File) -> Unit, onManage: () -> Unit, onDismiss: () -> Unit) {
    val packs by container.stickers.packs.collectAsStateWithLifecycle()
    var selected by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            if (packs.none { it.stickers.isNotEmpty() }) {
                Text(
                    "Стикеров пока нет. Создайте свой набор — он появится здесь.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val idx = selected.coerceIn(0, packs.lastIndex)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(packs.size) { i ->
                        FilterChip(selected = i == idx, onClick = { selected = i }, label = { Text(packs[i].name) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(84.dp), modifier = Modifier.height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(packs[idx].stickers) { f ->
                        Box(Modifier.bounceClick { onPick(f); onDismiss() }, contentAlignment = Alignment.Center) { StickerImage(f, 84.dp) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(); onManage() }) { Text("Создать / изменить наборы") }
            }
        }
    }
}
