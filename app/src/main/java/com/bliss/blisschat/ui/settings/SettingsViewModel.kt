package com.bliss.blisschat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bliss.blisschat.AppContainer
import com.bliss.blisschat.Session
import com.bliss.blisschat.data.AppSettings
import com.bliss.blisschat.data.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MutedItem(val key: String, val name: String)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val user: StateFlow<UserProfile?> = c.session.map { (it as? Session.Ready)?.user }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val updates get() = c.updates

    /** People and chats whose notifications are switched off (with readable names). */
    val muted: StateFlow<List<MutedItem>> = c.settings.settings.map { it.muted }.distinctUntilChanged().mapLatest { keys ->
        keys.sorted().map { k ->
            val id = k.substring(2)
            val name = runCatching {
                if (k.startsWith("u:")) c.repo.observeUser(id).first()?.title else c.repo.observeChat(id).first()?.name?.ifBlank { "Личный чат" }
            }.getOrNull()
            MutedItem(k, name ?: id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDark(v: Boolean) { viewModelScope.launch { c.settings.setDark(v) } }
    fun setHue(v: Float) { viewModelScope.launch { c.settings.setHue(v) } }
    fun setRememberMe(v: Boolean) { viewModelScope.launch { c.settings.setRememberMe(v) } }
    fun setNotifMessages(v: Boolean) { viewModelScope.launch { c.settings.setNotifMessages(v) } }
    fun setNotifCalls(v: Boolean) { viewModelScope.launch { c.settings.setNotifCalls(v) } }
    fun setNotifFullScreen(v: Boolean) { viewModelScope.launch { c.settings.setNotifFullScreen(v) } }
    fun setTextScale(v: Float) { viewModelScope.launch { c.settings.setTextScale(v) } }
    fun setAutoLoadPhotos(v: Boolean) { viewModelScope.launch { c.settings.setAutoLoadPhotos(v) } }
    fun setHideOnline(v: Boolean) { viewModelScope.launch { c.settings.setHideOnline(v) } }
    fun unmute(key: String) { viewModelScope.launch { c.settings.setMuted(key, false) } }
    fun signOut() { c.auth.signOut() }
}
