package com.xperiaprocam.presentation.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiaprocam.core.thermal.ThermalGuardian
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.xperiaprocam.data.settings.SettingsRepository
import com.xperiaprocam.data.status.BatteryStatus
import com.xperiaprocam.data.storage.MediaStoreSaver
import com.xperiaprocam.data.camera.ProControlsResolver
import com.xperiaprocam.domain.model.AppSettings
import com.xperiaprocam.domain.model.CaptureMode
import com.xperiaprocam.domain.model.FileNameGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

enum class FlashMode { AUTO, ON, OFF }
enum class LensFacing { BACK, FRONT }

/** State of an in-flight multi-frame burst (night/stars). */
data class BurstUiState(
    val active: Boolean = false,
    val frame: Int = 0,
    val total: Int = 0,
    val cancelRequested: Boolean = false,
)

/** Manual Pro-mode values; null = automatic. WB preset uses CONTROL_AWB_MODE ints. */
data class ProUiState(
    val iso: Int? = null,
    val exposureNs: Long? = null,
    val focusDistance: Float? = null,
    val wbMode: Int? = null,
) {
    val anyManual: Boolean
        get() = iso != null || exposureNs != null || focusDistance != null || wbMode != null
}

data class PanoUiState(
    val active: Boolean = false,
    val frame: Int = 0,
    val total: Int = 8,
    val cancelRequested: Boolean = false,
)

data class VideoUiState(
    val recording: Boolean = false,
    val elapsedSec: Int = 0,
)

data class CameraUiState(
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.AUTO,
    val zoomRatio: Float = 1f,
    val timerSeconds: Int = 0,
    val countdownRemaining: Int = 0,
    val lastPhotoUri: Uri? = null,
    val isCapturing: Boolean = false,
    val mode: CaptureMode = CaptureMode.CLASSIC,
    val manualExposureAvailable: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val saver: MediaStoreSaver,
    private val settings: SettingsRepository,
    private val battery: BatteryStatus,
    private val thermal: ThermalGuardian,
) : ViewModel() {

    fun thermalState() = thermal.state()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState

    private val _burstState = MutableStateFlow(BurstUiState())
    val burstState: StateFlow<BurstUiState> = _burstState

    private val _proState = MutableStateFlow(ProUiState())
    val proState: StateFlow<ProUiState> = _proState

    private val _panoState = MutableStateFlow(PanoUiState())
    val panoState: StateFlow<PanoUiState> = _panoState

    private val _videoState = MutableStateFlow(VideoUiState())
    val videoState: StateFlow<VideoUiState> = _videoState

    val appSettings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun reportManualExposure(available: Boolean) {
        if (_uiState.value.manualExposureAvailable != available) {
            _uiState.value = _uiState.value.copy(manualExposureAvailable = available)
        }
    }

    fun setMode(mode: CaptureMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            message = when (mode) {
                CaptureMode.STARS -> if (_uiState.value.manualExposureAvailable) {
                    "Modo estrellas: sujeta el teléfono muy quieto o usa trípode"
                } else {
                    "Exposición manual no soportada; modo estrellas con exposición automática"
                }
                CaptureMode.NIGHT -> "Modo noche: apila ${mode.burstFrames} fotos para reducir ruido"
                CaptureMode.HDR -> "HDR: 3 exposiciones (-2/0/+2 EV) fusionadas"
                CaptureMode.PANORAMA -> "Panorámica: dispara y mueve el teléfono lentamente hacia la derecha"
                CaptureMode.MACRO -> "Macro: acércate a 2-5 cm; se usa el lente gran angular"
                else -> null
            },
        )
    }

    // --- Pro controls -----------------------------------------------------

    fun setProIso(v: Int?, ranges: ProControlsResolver.ProRanges? = null) {
        val clamped = if (v != null && ranges?.isoRange != null) {
            v.coerceIn(ranges.isoRange.first, ranges.isoRange.last)
        } else {
            v?.coerceIn(50, 12800)
        }
        _proState.value = _proState.value.copy(iso = clamped)
    }

    fun setProExposure(ns: Long?, ranges: ProControlsResolver.ProRanges? = null) {
        val clamped = if (ns != null && ranges?.exposureNsRange != null) {
            ns.coerceIn(ranges.exposureNsRange.first, ranges.exposureNsRange.last)
        } else {
            ns
        }
        _proState.value = _proState.value.copy(exposureNs = clamped)
    }

    fun setProFocus(d: Float?, ranges: ProControlsResolver.ProRanges? = null) {
        val maxFocus = ranges?.minFocusDistance ?: 20f
        _proState.value = _proState.value.copy(focusDistance = d?.coerceIn(0f, maxFocus))
    }

    fun setProWb(mode: Int?) { _proState.value = _proState.value.copy(wbMode = mode) }
    fun resetPro() { _proState.value = ProUiState() }

    // --- Camera basics ----------------------------------------------------

    fun toggleLens() {
        _uiState.value = _uiState.value.copy(
            lensFacing = if (_uiState.value.lensFacing == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK,
            zoomRatio = 1f,
        )
    }


    fun cycleFlash() {
        val next = when (_uiState.value.flashMode) {
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
            FlashMode.OFF -> FlashMode.AUTO
        }
        _uiState.value = _uiState.value.copy(flashMode = next)
    }

    fun setZoom(ratio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = ratio.coerceIn(0.5f, 15f))
    }

    fun cycleTimer() {
        val next = when (_uiState.value.timerSeconds) {
            0 -> 3; 3 -> 5; 5 -> 10; else -> 0
        }
        _uiState.value = _uiState.value.copy(timerSeconds = next, countdownRemaining = 0)
    }

    fun setCapturing(capturing: Boolean) {
        _uiState.value = _uiState.value.copy(isCapturing = capturing)
    }

    fun setCountdown(remaining: Int) {
        _uiState.value = _uiState.value.copy(countdownRemaining = remaining)
    }

    fun batteryCritical(): Boolean = battery.isCritical()

    // --- Burst (night/stars) ----------------------------------------------

    fun beginBurst(total: Int) {
        _burstState.value = BurstUiState(active = true, frame = 0, total = total)
        _uiState.value = _uiState.value.copy(isCapturing = true)
    }

    fun onBurstFrame(frame: Int) {
        _burstState.value = _burstState.value.copy(frame = frame)
    }

    fun requestCancelBurst() {
        _burstState.value = _burstState.value.copy(cancelRequested = true)
    }

    fun burstCancelled(): Boolean = _burstState.value.cancelRequested

    fun endBurst() {
        _burstState.value = BurstUiState()
        _uiState.value = _uiState.value.copy(isCapturing = false)
    }

    // --- Panorama -----------------------------------------------------------

    fun beginPano(total: Int = 8) {
        _panoState.value = PanoUiState(active = true, frame = 0, total = total)
        _uiState.value = _uiState.value.copy(isCapturing = true)
    }

    fun onPanoFrame(frame: Int) {
        _panoState.value = _panoState.value.copy(frame = frame)
    }

    fun requestCancelPano() {
        _panoState.value = _panoState.value.copy(cancelRequested = true)
    }

    fun panoCancelled(): Boolean = _panoState.value.cancelRequested

    fun endPano() {
        _panoState.value = PanoUiState()
        _uiState.value = _uiState.value.copy(isCapturing = false)
    }

    // --- Video --------------------------------------------------------------

    fun videoStarted() {
        _videoState.value = VideoUiState(recording = true, elapsedSec = 0)
    }

    fun videoTick() {
        _videoState.value = _videoState.value.copy(
            elapsedSec = _videoState.value.elapsedSec + 1,
        )
    }

    fun videoStopped() {
        _videoState.value = VideoUiState()
    }

    // --- Results --------------------------------------------------------------

    fun onSaved(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            isCapturing = false,
            countdownRemaining = 0,
            lastPhotoUri = uri ?: _uiState.value.lastPhotoUri,
            message = if (uri != null) null else "No se pudo guardar la foto",
        )
    }

    fun onCaptureError(t: Throwable?) {
        val msg = when ((t as? ImageCaptureException)?.imageCaptureError) {
            ImageCapture.ERROR_CAMERA_CLOSED -> "La cámara se cerró. Inténtalo de nuevo."
            ImageCapture.ERROR_CAPTURE_FAILED -> "Falló la captura. Revisa luz y enfoque."
            ImageCapture.ERROR_FILE_IO -> "No se pudo guardar. Revisa el espacio disponible."
            ImageCapture.ERROR_INVALID_CAMERA -> "Cámara no disponible."
            else -> "Error al capturar (${t?.javaClass?.simpleName ?: "desconocido"})"
        }
        _uiState.value = _uiState.value.copy(isCapturing = false, countdownRemaining = 0, message = msg)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** Persists JPEG bytes via MediaStore. Returns the Uri, or null on failure. */
    fun saveJpeg(bytes: ByteArray): Uri? = saver.saveJpeg(bytes)

    /** Persists a processed result with a mode suffix (XPC_..._NIGHT.jpg). */
    fun saveProcessedJpeg(bytes: ByteArray, suffix: String): Uri? =
        saver.saveJpeg(bytes, FileNameGenerator.withSuffix(suffix, LocalDateTime.now()))
}
