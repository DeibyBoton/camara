package com.xperiaprocam.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiaprocam.data.settings.SettingsRepository
import com.xperiaprocam.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setShowGrid(v: Boolean) = viewModelScope.launch { repository.setShowGrid(v) }
    fun setShowLevel(v: Boolean) = viewModelScope.launch { repository.setShowLevel(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { repository.setHaptics(v) }
    fun setTimer(v: Int) = viewModelScope.launch { repository.setDefaultTimer(v) }
    fun setLook(v: com.xperiaprocam.domain.model.CaptureLook) = viewModelScope.launch { repository.setLook(v) }
    fun setRawCapture(v: Boolean) = viewModelScope.launch { repository.setRawCapture(v) }
}
