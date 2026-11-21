package com.bliss.blisschat.ui.chat

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.isSpecified
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bliss.blisschat.data.MemberStat
import com.bliss.blisschat.data.TagStep
import com.bliss.blisschat.data.rankFor
import com.bliss.blisschat.ui.stickers.StickerImage
import com.bliss.blisschat.ui.stickers.StickerPicker
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.data.AppSettings
import com.bliss.blisschat.data.Chat
import com.bliss.blisschat.data.ChatRepository
import com.bliss.blisschat.data.ChatType
import com.bliss.blisschat.data.Content
import com.bliss.blisschat.data.UiMessage
import com.bliss.blisschat.data.UserProfile
import com.bliss.blisschat.ui.components.Avatar
import com.bliss.blisschat.ui.components.BlissHeader
import com.bliss.blisschat.ui.components.BlobButton
import com.bliss.blisschat.ui.components.NameWithBadges
import com.bliss.blisschat.ui.components.PillTextField
import com.bliss.blisschat.ui.components.VerifiedBlue
import com.bliss.blisschat.ui.components.bounceClick
import com.bliss.blisschat.ui.components.formatClock
import com.bliss.blisschat.ui.components.formatDayHeader
import com.bliss.blisschat.ui.components.isSameDay
import com.bliss.blisschat.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥")

private sealed interface Row2 {
    data class Day(val label: String, val key: String) : Row2
    data class Msg(val m: UiMessage) : Row2
}

private fun buildRows(messages: List<UiMessage>): List<Row2> {
    val out = ArrayList<Row2>(messages.size + 4)
    var last = 0L
    messages.forEach { u ->
        val ts = u.message.ts
        if (last == 0L || !isSameDay(last, ts)) out += Row2.Day(formatDayHeader(ts), "day-${ts / 86_400_000L}")
        out += Row2.Msg(u)
        last = ts
    }
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(container: AppContainer, chatId: String, onBack: () -> Unit, onOpenProfile: (String) -> Unit, onOpenStickers: () -> Unit = {}) {
    val vm: ChatViewModel = viewModel(key = "chat-$chatId", factory = container.factory(chatId))
    val s by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.events.collect { snackbar.showSnackbar(it) } }

    var draft by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<MenuTarget?>(null) }
    var showStickers by remember { mutableStateOf(false) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // "Active in the group" time counts only while the screen is visible
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> vm.setActive(true)
                Lifecycle.Event.ON_PAUSE -> vm.setActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); vm.setActive(false) }
    }
    var viewer by remember { mutableStateOf<Bitmap?>(null) }
    var pendingVideo by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.sendImage(uri) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val ok = res[Manifest.permission.RECORD_AUDIO] == true && (!pendingVideo || res[Manifest.permission.CAMERA] == true)
        if (ok) vm.call(pendingVideo)
    }
    fun startCall(video: Boolean) {
        if (vm.hasCallPermissions(video)) vm.call(video)
        else {
            pendingVideo = video
            val perms = buildList<String> {
                add(Manifest.permission.RECORD_AUDIO)
                if (video) add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissions.launch(perms.toTypedArray())
        }
    }

    val chat = s.chat
    val other = s.other
    val title = when {
        chat == null -> if (s.chatLoaded) "Чат не найден" else ""
        chat.type == ChatType.DIRECT -> other?.title ?: "Личный чат"
        else -> chat.name
    }
    val subtitle = when {
        chat == null -> ""
        chat.type == ChatType.DIRECT -> if (other?.online() == true) "в сети" else other?.let { "@${it.username}" }.orEmpty()
        chat.type == ChatType.CHANNEL -> "канал · ${chat.members.size} подписч."
        else -> "${chat.members.size} участн."
    }
    val openHeader: () -> Unit = {
        if (chat?.type == ChatType.DIRECT) {
            other?.let { onOpenProfile(it.uid) }
        } else {
            showInfo = true
        }
    }

    val blurRadius by animateDpAsState(if (actionFor != null) 18.dp else 0.dp, tween(220), label = "menu-blur")
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
    Column(
        Modifier.fillMaxSize().then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .background(MaterialTheme.colorScheme.background).imePadding()
    ) {
        BlissHeader {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                Row(Modifier.weight(1f).bounceClick(enabled = chat != null, onClick = openHeader), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(other?.avatarUrl.orEmpty(), title.ifBlank { "?" }, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        NameWithBadges(
                            title, verified = other?.verified == true || chat?.verified == true, support = other?.support == true,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
                        }
                    }
                }
                if (chat?.type == ChatType.DIRECT && other != null) {
                    BlobButton(Icons.Filled.Videocam, "Видеозвонок", { startCall(true) }, size = 46.dp,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), content = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                    BlobButton(Icons.Filled.Call, "Звонок", { startCall(false) }, size = 46.dp,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), content = MaterialTheme.colorScheme.onPrimary)
                }
                if (chat != null) Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Меню") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(when (chat.type) { ChatType.DIRECT -> "Профиль"; ChatType.CHANNEL -> "О канале"; else -> "О группе" }) },
                            leadingIcon = { Icon(Icons.Filled.Info, null) },
                            onClick = { menuOpen = false; openHeader() }
                        )
                        val muted = vm.isMuted(chat, s.myId, settings.muted)
                        DropdownMenuItem(
                            text = { Text(if (muted) "Включить уведомления" else "Выключить уведомления") },
                            leadingIcon = { Icon(if (muted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff, null) },
                            onClick = { menuOpen = false; vm.toggleMute() }
                        )
                        if (s.isAdmin && s.isGroup) {
                            DropdownMenuItem(
                                text = { Text(if (chat.verified) "Снять галочку" else "Подтвердить") },
                                leadingIcon = { Icon(Icons.Filled.Verified, null, tint = VerifiedBlue) },
                                onClick = { menuOpen = false; vm.setGroupVerified(!chat.verified) }
                            )
                        }
                        if (s.canLeave) {
                            DropdownMenuItem(
                                text = { Text(if (chat.type == ChatType.CHANNEL) "Отписаться" else "Выйти из группы") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                                onClick = { menuOpen = false; vm.leave(onBack) }
                            )
                        }
                        if (s.canDeleteGroup) {
                            DropdownMenuItem(
                                text = { Text(if (s.isAdmin && !s.isOwner) "Удалить (админ)" else if (chat.type == ChatType.CHANNEL) "Удалить канал" else "Удалить группу", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDelete = true }
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (chat == null && s.chatLoaded) {
                Text("Этот чат не существует или был удалён.", Modifier.align(Alignment.Center).padding(32.dp), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                MessageList(s, chat, vm.repo, settings, hiddenId = actionFor?.u?.message?.id, onLongPress = { u, r -> actionFor = MenuTarget(u, r) }, onReact = { m, e -> vm.react(m, e) }, onImage = { viewer = it },
                    onSender = { onOpenProfile(it.uid) })
            }
        }

        // Input bar
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Box(Modifier.navigationBarsPadding()) {
                when {
                    chat == null -> Spacer(Modifier.height(12.dp))
                    !s.isMember -> Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (s.isChannel) "Вы не подписаны на этот канал" else "Вы не участник этой группы", style = MaterialTheme.typography.bodyMedium)
                    }
                    !s.canPost -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Публикует только владелец канала", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        val muted = vm.isMuted(chat, s.myId, settings.muted)
                        TextButton(onClick = { vm.toggleMute() }) { Text(if (muted) "Включить звук" else "Отключить звук") }
                    }
                    else -> Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                        PillTextField(
                            value = draft, onValueChange = { draft = it }, placeholder = "Сообщение", singleLine = false, maxLines = 5,
                            shape = RoundedCornerShape(28.dp), modifier = Modifier.weight(1f)
                        )
                        if (sending) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp) }
                        } else {
                            IconButton(onClick = { showStickers = true }, Modifier.size(48.dp)) {
                                Icon(Icons.Filled.EmojiEmotions, "Стикеры", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.size(48.dp)) {
                                Icon(Icons.Filled.AttachFile, "Прикрепить фото", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        BlobButton(
                            Icons.AutoMirrored.Filled.Send, "Отправить", size = 56.dp, enabled = draft.isNotBlank(),
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) { draft = ""; vm.sendText(text) { failed -> if (draft.isBlank()) draft = failed } }
                            }
                        )
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { SnackbarHost(snackbar, Modifier.navigationBarsPadding().padding(bottom = 80.dp)) }

    actionFor?.let { t ->
        val m = t.u
        MessageMenuOverlay(
            target = t, origin = origin, mine = m.message.senderId == s.myId, myReaction = m.message.reactions[s.myId],
            bubble = { mod -> BubbleSurface(m, m.message.senderId == s.myId, s.users[m.message.senderId], chat, s.myId, vm.repo, s.stats, settings, {}, mod) },
            onDismiss = { actionFor = null },
            onReact = { vm.react(m, it); actionFor = null },
            onDeleteAll = { vm.deleteForEveryone(m); actionFor = null },
            onDeleteLocal = { vm.deleteLocally(m); actionFor = null }
        )
    }
    } // outer box
    if (showStickers) StickerPicker(container, onPick = { vm.sendSticker(it) }, onManage = onOpenStickers, onDismiss = { showStickers = false })
    if (showInfo && chat != null) {
        GroupInfoSheet(s, chat, onDismiss = { showInfo = false }, onOpenProfile = { showInfo = false; onOpenProfile(it) }, onAdd = vm::addMember, onSelfTag = vm::setSelfTag, onTags = vm::updateTags)
    }
    viewer?.let { bmp ->
        Dialog(onDismissRequest = { viewer = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { viewer = null }, contentAlignment = Alignment.Center) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { viewer = null }, Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Filled.Close, "Закрыть", tint = Color.White) }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (chat?.type == ChatType.CHANNEL) "Удалить канал?" else "Удалить группу?") },
            text = { Text("«${chat?.name.orEmpty()}» и все сообщения будут удалены у всех участников.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteGroup(onBack) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }
}

/* ------------------------------------------------------------------ messages */

@Composable
private fun MessageList(
    s: ChatUiState, chat: Chat?, repo: ChatRepository, settings: AppSettings, hiddenId: String?,
    onLongPress: (UiMessage, Rect) -> Unit, onReact: (UiMessage, String) -> Unit, onImage: (Bitmap) -> Unit, onSender: (UserProfile) -> Unit
) {
    val rows = remember(s.messages) { buildRows(s.messages) }
    val listState = rememberLazyListState()
    var banner by rememberSaveable { mutableStateOf(true) }

    var initialIds by remember { mutableStateOf<Set<String>?>(null) }
    val appeared = remember { mutableSetOf<String>() }
    LaunchedEffect(s.messagesLoaded) { if (s.messagesLoaded && initialIds == null) initialIds = s.messages.map { it.message.id }.toSet() }

    var firstScroll by remember { mutableStateOf(true) }
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty()) {
            if (firstScroll) { listState.scrollToItem(rows.lastIndex); firstScroll = false } else listState.animateScrollToItem(rows.lastIndex)
        }
    }

    LazyColumn(
        state = listState, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()
    ) {
        if (banner) item(key = "e2ee-banner") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Сообщения, фото и звонки защищены сквозным шифрованием. Прочитать их можете только вы и участники чата.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { banner = false }, Modifier.size(36.dp)) { Icon(Icons.Filled.Close, "Скрыть", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
        if (s.messagesLoaded && rows.isEmpty()) item(key = "empty") {
            Text("Сообщений пока нет. Напишите первым!", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(rows, key = { if (it is Row2.Msg) it.m.message.id else (it as Row2.Day).key }) { row ->
            when (row) {
                is Row2.Day -> Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(row.label, Modifier.padding(horizontal = 18.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                is Row2.Msg -> {
                    val u = row.m
                    val animate = remember(u.message.id) { val i = initialIds; i != null && u.message.id !in i && appeared.add(u.message.id) }
                    val mine = u.message.senderId == s.myId
                    val bubble: @Composable () -> Unit = {
                        MessageBubble(u, mine, s.users[u.message.senderId], chat, s.myId, repo, s.stats, settings, hiddenId == u.message.id, onLongPress, onReact, onImage, onSender)
                    }
                    if (animate) {
                        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                        AnimatedVisibility(
                            visibleState = visible,
                            enter = fadeIn(tween(180)) + slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) { it / 2 } +
                                scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), initialScale = 0.85f, transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f))
                        ) { bubble() }
                    } else bubble()
                }
            }
        }
    }
}

private data class MenuTarget(val u: UiMessage, val rect: Rect)

private fun scaled(style: TextStyle, k: Float): TextStyle = if (k == 1f) style else style.copy(
    fontSize = if (style.fontSize.isSpecified) style.fontSize * k else style.fontSize,
    lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * k else style.lineHeight
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    u: UiMessage, mine: Boolean, sender: UserProfile?, chat: Chat?, myId: String, repo: ChatRepository,
    stats: Map<String, MemberStat>, settings: AppSettings, hidden: Boolean,
    onLongPress: (UiMessage, Rect) -> Unit, onReact: (UiMessage, String) -> Unit, onImage: (Bitmap) -> Unit, onSender: (UserProfile) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val msg = u.message
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!mine) {
            Box(Modifier.padding(end = 8.dp).bounceClick(enabled = sender != null) { sender?.let(onSender) }) {
                Avatar(sender?.avatarUrl.orEmpty(), sender?.title ?: "?", size = 38.dp)
            }
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 300.dp)) {
            BubbleSurface(
                u, mine, sender, chat, myId, repo, stats, settings, onImage,
                Modifier.alpha(if (hidden) 0f else 1f).onGloballyPositioned { bounds = it.boundsInRoot() }
                    .pressScale(source, 0.97f)
                    .combinedClickable(interactionSource = source, indication = null, onClick = {}, onLongClick = { onLongPress(u, bounds) })
            )
            if (msg.reactions.isNotEmpty() && !msg.deleted) {
                Row(Modifier.padding(top = 3.dp, start = 6.dp, end = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    msg.reactions.values.groupingBy { it }.eachCount().forEach { (emoji, count) ->
                        val mineR = msg.reactions[myId] == emoji
                        Surface(
                            shape = CircleShape, color = if (mineR) scheme.primaryContainer else scheme.surfaceContainerHigh,
                            modifier = Modifier.then(if (mineR) Modifier.border(1.dp, scheme.primary, CircleShape) else Modifier).bounceClick { onReact(u, emoji) }
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, style = MaterialTheme.typography.bodyMedium)
                                if (count > 1) { Spacer(Modifier.width(3.dp)); Text("$count", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The bubble itself (also drawn in the long-press overlay). */
@Composable
private fun BubbleSurface(
    u: UiMessage, mine: Boolean, sender: UserProfile?, chat: Chat?, myId: String, repo: ChatRepository,
    stats: Map<String, MemberStat>, settings: AppSettings, onImage: (Bitmap) -> Unit, modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // Multi-cornered bubbles: the corner next to the sender is pointed
    val shape = if (mine) RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomEnd = 5.dp, bottomStart = 26.dp)
    else RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomEnd = 26.dp, bottomStart = 5.dp)
    val msg = u.message
    val sticker = !msg.deleted && !u.failed && u.content?.kind == Content.KIND_STICKER
    val bg = when { sticker -> Color.Transparent; mine -> scheme.primary; else -> scheme.surfaceContainerHighest }
    val fg = if (mine) scheme.onPrimary else scheme.onSurface
    val stat = stats[msg.senderId]
    val rank = if (chat?.type == ChatType.GROUP) chat.rankFor(stat) else null
    val selfTag = if (chat?.type == ChatType.GROUP && chat.selfTags) stat?.selfTag?.takeIf { it.isNotBlank() } else null
    val body = scaled(MaterialTheme.typography.bodyLarge, settings.textScale)

    Surface(shape = shape, color = bg, contentColor = fg, modifier = modifier) {
        Column(Modifier.padding(horizontal = if (sticker) 4.dp else 14.dp, vertical = if (sticker) 2.dp else 8.dp)) {
            if (!sticker) {
                val name = (sender?.title ?: "…") + if (mine) " (вы)" else ""
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.6f), modifier = Modifier.weight(1f, fill = false))
                    if (sender?.verified == true) { Spacer(Modifier.width(3.dp)); Icon(Icons.Filled.Verified, null, Modifier.size(13.dp), tint = if (mine) fg else VerifiedBlue) }
                    if (rank != null) TagChip(rank, true, fg)
                    if (selfTag != null) TagChip(selfTag, false, fg)
                }
            }
            when {
                msg.deleted -> Text("Сообщение удалено", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = fg.copy(alpha = 0.6f))
                u.failed || u.content == null -> Text("🔒 Не удалось расшифровать сообщение", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = fg.copy(alpha = 0.7f))
                u.content.kind == Content.KIND_IMAGE -> EncryptedImage(u.content, repo, onImage, autoLoad = settings.autoLoadPhotos)
                u.content.kind == Content.KIND_STICKER -> EncryptedImage(u.content, repo, onImage, sticker = true)
                else -> Text(u.content.text, style = body)
            }
            Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatClock(msg.ts), style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.7f))
                if (mine && !msg.deleted) {
                    Spacer(Modifier.width(4.dp))
                    val read = if (chat?.type == ChatType.DIRECT) msg.readBy.any { it != myId } else msg.readBy.size > 1
                    Icon(if (read) Icons.Filled.DoneAll else Icons.Filled.Done, if (read) "Прочитано" else "Отправлено", Modifier.size(15.dp), tint = fg.copy(alpha = if (read) 1f else 0.6f))
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, rank: Boolean, fg: Color) {
    Spacer(Modifier.width(5.dp))
    Surface(shape = CircleShape, color = fg.copy(alpha = if (rank) 0.22f else 0.12f)) {
        Text(
            text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
        )
    }
}

/* ------------------------------------------------------------------ encrypted images */

internal object ImageCache {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    fun get(k: String): Bitmap? = cache.get(k)
    fun put(k: String, v: Bitmap) { cache.put(k, v) }
    fun clear() { cache.evictAll() }
}

@Composable
fun EncryptedImage(content: Content, repo: ChatRepository, onClick: (Bitmap) -> Unit, sticker: Boolean = false, autoLoad: Boolean = true) {
    var bmp by remember(content.path) { mutableStateOf(ImageCache.get(content.path)) }
    var failed by remember(content.path) { mutableStateOf(false) }
    var requested by remember(content.path) { mutableStateOf(autoLoad || sticker) }
    var attempt by remember(content.path) { mutableIntStateOf(0) }
    LaunchedEffect(content.path, requested, attempt) {
        if (bmp == null && requested) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = repo.downloadMedia(content.path, content.key)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
            if (loaded != null) { ImageCache.put(content.path, loaded); bmp = loaded } else failed = true
        }
    }
    val ratio = if (content.w > 0 && content.h > 0) (content.w.toFloat() / content.h).coerceIn(0.6f, 1.7f) else 1f
    val boxMod = if (sticker) Modifier.padding(top = 2.dp).width(150.dp).aspectRatio(ratio)
    else Modifier.padding(top = 4.dp).width(230.dp).aspectRatio(ratio).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.15f))
    Box(boxMod, contentAlignment = Alignment.Center) {
        val b = bmp
        when {
            b != null -> Image(b.asImageBitmap(), if (sticker) "Стикер" else "Фото", Modifier.fillMaxSize().then(if (sticker) Modifier else Modifier.clickable { onClick(b) }),
                contentScale = if (sticker) ContentScale.Fit else ContentScale.Crop)
            failed -> Text(
                "Не удалось загрузить. Нажмите, чтобы повторить", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp).clickable { failed = false; attempt++ }
            )
            !requested -> Text("Нажмите, чтобы загрузить фото", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize().clickable { requested = true }.padding(12.dp))
            else -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
        }
    }
}

/* ------------------------------------------------------------------ long-press menu */

/**
 * Telegram-style menu: the chat behind is blurred and dimmed, the pressed message is shown sharp
 * in its place and the reactions + actions appear right under it.
 */
@Composable
private fun MessageMenuOverlay(
    target: MenuTarget, origin: androidx.compose.ui.geometry.Offset, mine: Boolean, myReaction: String?,
    bubble: @Composable (Modifier) -> Unit,
    onDismiss: () -> Unit, onReact: (String) -> Unit, onDeleteAll: () -> Unit, onDeleteLocal: () -> Unit
) {
    val m = target.u
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    val text = m.content?.takeIf { it.kind == Content.KIND_TEXT && !m.message.deleted }?.text
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        val hPx = with(density) { maxHeight.toPx() }
        val wPx = with(density) { maxWidth.toPx() }
        val gap = with(density) { 10.dp.toPx() }
        val menuW = with(density) { 264.dp.toPx() }
        val actionRows = (if (text != null) 1 else 0) + (if (mine && !m.message.deleted) 1 else 0) + 1
        val menuH = with(density) { (if (!mine && !m.message.deleted) 60.dp.toPx() else 0f) + actionRows * 52.dp.toPx() + 16.dp.toPx() }
        val r = target.rect.translate(-origin.x, -origin.y)
        val topLimit = with(density) { 72.dp.toPx() }
        val bottomLimit = hPx - with(density) { 24.dp.toPx() }
        var bubbleTop = r.top
        if (bubbleTop + r.height + gap + menuH > bottomLimit) bubbleTop = bottomLimit - menuH - gap - r.height
        bubbleTop = bubbleTop.coerceAtLeast(topLimit)
        val menuTop = (bubbleTop + r.height + gap).coerceAtMost(hPx - menuH - with(density) { 8.dp.toPx() })
        val menuLeft = (if (mine) r.right - menuW else r.left).coerceIn(with(density) { 8.dp.toPx() }, (wPx - menuW - with(density) { 8.dp.toPx() }).coerceAtLeast(0f))

        // the sharp copy of the pressed message
        Box(
            Modifier.offset { IntOffset(r.left.toInt(), bubbleTop.toInt()) }
                .width(with(density) { r.width.toDp() })
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        ) { bubble(Modifier) }

        AnimatedVisibility(
            visibleState = appear,
            enter = fadeIn(tween(160)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), initialScale = 0.8f, transformOrigin = TransformOrigin(if (mine) 1f else 0f, 0f)),
            modifier = Modifier.offset { IntOffset(menuLeft.toInt(), menuTop.toInt()) }.width(with(density) { menuW.toDp() })
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!mine && !m.message.deleted) {
                    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            REACTIONS.forEach { e ->
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape)
                                        .background(if (myReaction == e) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .bounceClick { onReact(e) },
                                    contentAlignment = Alignment.Center
                                ) { Text(e, fontSize = 18.sp) }
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        if (text != null) MenuAction("Копировать", Icons.Filled.ContentCopy, MaterialTheme.colorScheme.onSurface) {
                            clipboard.setText(AnnotatedString(text)); onDismiss()
                        }
                        if (mine && !m.message.deleted) MenuAction("Удалить у всех", Icons.Filled.Delete, MaterialTheme.colorScheme.error, onDeleteAll)
                        MenuAction("Удалить у меня", Icons.Filled.Delete, if (mine) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error, onDeleteLocal)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).bounceClick(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/* ------------------------------------------------------------------ group info */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInfoSheet(
    s: ChatUiState, chat: Chat, onDismiss: () -> Unit, onOpenProfile: (String) -> Unit,
    onAdd: (String, (Result<Unit>) -> Unit) -> Unit, onSelfTag: (String) -> Unit, onTags: (Boolean, Boolean, List<TagStep>) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val isGroup = chat.type == ChatType.GROUP
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar("", chat.name, size = 88.dp)
            Spacer(Modifier.height(10.dp))
            NameWithBadges(chat.name, chat.verified, false, style = MaterialTheme.typography.headlineSmall, badgeSize = 24.dp)
            Text(
                (if (chat.type == ChatType.CHANNEL) "Канал · ${chat.members.size} подписчиков" else "Группа · ${chat.members.size} участников"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            chat.members.forEach { uid ->
                val u = s.users[uid]
                val stat = s.stats[uid]
                val rank = if (isGroup) chat.rankFor(stat) else null
                val own = if (isGroup && chat.selfTags) stat?.selfTag?.takeIf { it.isNotBlank() } else null
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).bounceClick { onOpenProfile(uid) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u?.avatarUrl.orEmpty(), u?.title ?: "?", size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        NameWithBadges(u?.title ?: "…", u?.verified == true, u?.support == true)
                        val sub = listOfNotNull("@${u?.username.orEmpty()}", rank, own).joinToString(" · ")
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (uid == chat.ownerId) Text("владелец", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (s.isOwner) {
                Spacer(Modifier.height(14.dp))
                PillTextField(username, { username = it; error = null }, "Добавить по @username", leading = Icons.Filled.PersonAdd, isError = error != null, supporting = error,
                    trailing = {
                        TextButton(enabled = username.isNotBlank() && !busy, onClick = {
                            busy = true
                            onAdd(username) { r -> busy = false; r.onSuccess { username = "" }.onFailure { error = it.message } }
                        }) { Text("Добавить") }
                    })
            }
            if (isGroup && s.isMember) {
                Spacer(Modifier.height(20.dp))
                if (chat.selfTags) MySelfTag(s.stats[s.myId]?.selfTag.orEmpty(), onSelfTag)
                if (s.isOwner) TagSettings(chat, onTags)
            }
        }
    }
}

@Composable
private fun MySelfTag(current: String, onSave: (String) -> Unit) {
    var tag by remember(current) { mutableStateOf(current) }
    Column(Modifier.fillMaxWidth()) {
        Text("Мой тег", style = MaterialTheme.typography.titleMedium)
        Text("Короткая подпись рядом с вашим именем в этой группе.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PillTextField(tag, { tag = it.take(16) }, "например, дизайнер",
            trailing = { TextButton(enabled = tag.trim() != current, onClick = { onSave(tag.trim()) }) { Text("Сохранить") } })
    }
}

@Composable
private fun TagSettings(chat: Chat, onTags: (Boolean, Boolean, List<TagStep>) -> Unit) {
    var creator by remember(chat.creatorTags) { mutableStateOf(chat.creatorTags) }
    var selfT by remember(chat.selfTags) { mutableStateOf(chat.selfTags) }
    var ladder by remember(chat.ladder) { mutableStateOf(chat.ladder.map { it.name to it.minutes.toString() }) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("Теги группы (для создателя)", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Теги от создателя (растут со временем)", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(creator, { creator = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Теги, которые пишут участники", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(selfT, { selfT = it })
        }
        if (creator) {
            Text(
                "Ступень выдаётся, когда активность участника достигает порога: 1 минута в группе = 1 очко, каждое сообщение = 2 очка.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ladder.forEachIndexed { i, (name, min) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillTextField(name, { v -> ladder = ladder.toMutableList().also { it[i] = v.take(16) to min } }, "Название", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    PillTextField(min, { v -> ladder = ladder.toMutableList().also { it[i] = name to v.filter(Char::isDigit).take(6) } }, "очков",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.width(110.dp))
                    IconButton(onClick = { ladder = ladder.toMutableList().also { it.removeAt(i) } }) { Icon(Icons.Filled.Delete, "Убрать ступень") }
                }
            }
            TextButton(onClick = { ladder = ladder + ("" to "0") }) { Text("+ Добавить ступень") }
        }
        TextButton(onClick = {
            onTags(creator, selfT, ladder.mapNotNull { (n, m) -> n.trim().takeIf { it.isNotEmpty() }?.let { TagStep(it, m.toIntOrNull() ?: 0) } })
        }) { Text("Сохранить настройки тегов") }
    }
}
