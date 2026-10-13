@file:OptIn(ExperimentalCamera2Interop::class)

package com.xperiaprocam.presentation.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.util.Size
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchVideo
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xperiaprocam.core.imageprocessing.FoodFilter
import com.xperiaprocam.core.imageprocessing.FrameMerger
import com.xperiaprocam.core.imageprocessing.HdrMerger
import com.xperiaprocam.core.imageprocessing.LookFilter
import com.xperiaprocam.core.imageprocessing.NightStackPipeline
import com.xperiaprocam.core.imageprocessing.PanoramaStitcher
import com.xperiaprocam.data.camera.ManualExposureResolver
import com.xperiaprocam.data.camera.ProControlsResolver
import com.xperiaprocam.data.status.StabilityDetector
import com.xperiaprocam.domain.model.CaptureMode
import com.xperiaprocam.domain.model.FileNameGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import android.app.ActivityManager
import android.content.Context
import com.xperiaprocam.core.thermal.ThermalGuardian
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** WB presets using public Camera2 CONTROL_AWB_MODE values (null = auto). */
private val WB_PRESETS: List<Pair<String, Int?>> = listOf(
    "Auto" to null,
    "Nublado" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    "Soleado" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
    "Fluor." to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT,
    "Incand." to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
    "Sombra" to CaptureRequest.CONTROL_AWB_MODE_SHADE,
)

/** Exposure presets for Pro mode (label → nanoseconds; null = auto). */
private val SHUTTER_PRESETS: List<Pair<String, Long?>> = listOf(
    "Auto" to null, "1/1000" to 1_000_000, "1/500" to 2_000_000, "1/250" to 4_000_000,
    "1/125" to 8_000_000, "1/60" to 16_670_000, "1/30" to 33_330_000, "1/15" to 66_670_000,
    "1/8" to 125_000_000, "1/4" to 250_000_000, "1/2" to 500_000_000,
    "1s" to 1_000_000_000, "2s" to 2_000_000_000,
)

/** Main camera screen with the full mode system. */
@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val ui by viewModel.uiState.collectAsState()
    val burst by viewModel.burstState.collectAsState()
    val pro by viewModel.proState.collectAsState()
    val pano by viewModel.panoState.collectAsState()
    val video by viewModel.videoState.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var proRanges by remember { mutableStateOf<ProControlsResolver.ProRanges?>(null) }
    var highResLabel by remember { mutableStateOf("Alta Res.") }
    var zoomPresets by remember { mutableStateOf(listOf(1f)) }
    var videoQualities by remember { mutableStateOf(listOf(Quality.FHD)) }
    var videoQuality by remember { mutableStateOf<Quality>(Quality.UHD) }
    var videoFps by remember { mutableStateOf(30) }
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { captureExecutor.shutdown() } }
    val manualResolver = remember { ManualExposureResolver(context) }
    val stability = remember { StabilityDetector(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf(buildVideoCapture(Quality.UHD, 30)) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var audioGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { audioGranted = it }

    // --- Capability discovery (once the provider is ready) ------------------
    LaunchedEffect(Unit) {
        var p: ProcessCameraProvider? = null
        for (attempt in 1..5) {
            p = runCatching { ProcessCameraProvider.getInstance(context).get() }.getOrNull()
            if (p != null) break
            delay(1000)
        }
        if (p == null) {
            scope.launch { snackbar.showSnackbar("Cámara no disponible. Reinicia el dispositivo si el sistema la bloqueó.") }
            return@LaunchedEffect
        }
        provider = p
        viewModel.reportManualExposure(manualResolver.resolve(facingBack = true) != null)
        proRanges = ProControlsResolver(context).resolve(facingBack = true)
        highResLabel = proRanges?.maxJpegSize?.let { (w, h) ->
            "Alta Res. ${w * h / 1_000_000}MP"
        } ?: "Alta Res."
        // Lens zoom presets from the logical back camera zoom range.
        runCatching {
            val info = p.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
            val range = Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            if (range != null) zoomPresets = buildZoomPresets(range.lower, range.upper)
            videoQualities = QualitySelector.getSupportedQualities(info)
                .filter { it in listOf(Quality.UHD, Quality.FHD, Quality.HD) }
                .sortedByDescending { qualityMpx(it) }
            videoQuality = videoQualities.firstOrNull() ?: Quality.FHD
        }
    }

    // --- Use case building ----------------------------------------------------

    fun baseSelector(): CameraSelector = if (ui.lensFacing == LensFacing.FRONT) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    fun buildImageCapture(mode: CaptureMode): ImageCapture =
        buildImageCaptureFor(mode, pro, proRanges, isBackCamera = ui.lensFacing == LensFacing.BACK)


    fun buildPreview(): Preview = buildPreviewWithPro(
        pro, proRanges, ui.mode, previewView.surfaceProvider
    )

    suspend fun rebindCurrent() {
        val p = provider ?: return
        var nextCapture: ImageCapture? = null
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                runCatching {
                    p.unbindAll()
                    val useCases = mutableListOf<UseCase>(buildPreview())
                    if (ui.mode == CaptureMode.VIDEO) {
                        useCases.add(videoCapture)
                    } else {
                        val capture = buildImageCapture(ui.mode)
                        nextCapture = capture
                        useCases.add(capture)
                    }
                    camera = p.bindToLifecycle(lifecycleOwner, baseSelector(), *useCases.toTypedArray())
                    val zoomState = camera?.cameraInfo?.zoomState?.value
                    val minZoom = zoomState?.minZoomRatio ?: 1f
                    val maxZoom = zoomState?.maxZoomRatio ?: 1f
                    camera?.cameraControl?.setZoomRatio(ui.zoomRatio.coerceIn(minZoom, maxZoom))
                    // Macro: jump to the widest lens (min zoom ratio of the logical camera).
                    if (ui.mode == CaptureMode.MACRO && zoomPresets.firstOrNull() != null) {
                        camera?.cameraControl?.setZoomRatio(zoomPresets.first().coerceIn(minZoom, maxZoom))
                    }
                }
                if (cont.isActive) cont.resumeWith(Result.success(Unit))
            }, mainExecutor)

        }
        imageCapture = nextCapture
    }

    suspend fun rebindWithCapture(capture: ImageCapture): Boolean {
        val p = provider ?: return false
        return suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val ok = runCatching {
                    p.unbindAll()
                    camera = p.bindToLifecycle(lifecycleOwner, baseSelector(), buildPreview(), capture)
                    camera?.cameraControl?.setZoomRatio(ui.zoomRatio)
                }.isSuccess
                if (cont.isActive) cont.resumeWith(Result.success(ok))
            }, mainExecutor)
        }
    }

    LaunchedEffect(ui.lensFacing, ui.mode) { rebindCurrent() }

    LaunchedEffect(pro, proRanges, camera) {
        val cam = camera ?: return@LaunchedEffect
        if (ui.mode == CaptureMode.PRO) {
            val ctl = androidx.camera.camera2.interop.Camera2CameraControl.from(cam.cameraControl)
            val b = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
            if (pro.iso != null || pro.exposureNs != null) {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                pro.iso?.let { b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it) }
                pro.exposureNs?.let { b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it) }
            } else {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
            if (pro.focusDistance != null) {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, pro.focusDistance!!)
            } else {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            pro.wbMode?.let { b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, it) }
            ctl.setCaptureRequestOptions(b.build())
        }
    }
    LaunchedEffect(ui.zoomRatio) {
        val zoomState = camera?.cameraInfo?.zoomState?.value
        val minZ = zoomState?.minZoomRatio ?: 1f
        val maxZ = zoomState?.maxZoomRatio ?: 1f
        camera?.cameraControl?.setZoomRatio(ui.zoomRatio.coerceIn(minZ, maxZ))
    }

    // Rebuild the video pipeline when resolution or FPS changes.
    LaunchedEffect(videoQuality, videoFps) {
        if (ui.mode == CaptureMode.VIDEO) {
            videoCapture = buildVideoCapture(videoQuality, videoFps)
            rebindCurrent()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { activeRecording?.stop() }
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    val busy = burst.active || pano.active || video.recording || ui.isCapturing
    LaunchedEffect(busy) { view.keepScreenOn = busy }

    // --- Frame plumbing -------------------------------------------------------

    fun applyFlash(capture: ImageCapture) {
        capture.flashMode = when (ui.flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    suspend fun nextFrame(capture: ImageCapture): Pair<ByteArray, Int>? =
        suspendCancellableCoroutine { cont ->
            capture.takePicture(
                captureExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bytes: ByteArray? = imageToJpegBytes(image)
                        val rotation = image.imageInfo.rotationDegrees
                        try { image.close() } catch (_: Throwable) { }
                        if (cont.isActive) cont.resumeWith(Result.success(bytes?.let { it to rotation }))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (cont.isActive) cont.resumeWith(Result.success(null))
                    }
                },
            )
        }

    suspend fun nextFrameSafe(capture: ImageCapture, timeoutMs: Long = 20_000): Pair<ByteArray, Int>? =
        withTimeoutOrNull(timeoutMs) { nextFrame(capture) }

    fun haptic() {
        if (appSettings.hapticFeedback) {
            previewView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    // --- Single shot (classic / portrait / highres / macro / food) -----------

    fun applyFoodAndSave(bytes: ByteArray): Uri? = try {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return viewModel.saveJpeg(bytes)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        bmp.recycle()
        val filtered = FoodFilter.apply(px)
        val out = Bitmap.createBitmap(filtered, bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val compressed = ByteArrayOutputStream().use { o ->
            out.compress(Bitmap.CompressFormat.JPEG, 95, o); o.toByteArray()
        }
        out.recycle()
        viewModel.saveProcessedJpeg(compressed, "FOOD")
    } catch (t: Throwable) {
        viewModel.saveJpeg(bytes)
    }

    /** Decodes, applies the selected creative look and saves (normal XPC name). */
    fun applyLookAndSave(bytes: ByteArray): Uri? = try {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return viewModel.saveJpeg(bytes)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        bmp.recycle()
        val filtered = LookFilter.apply(appSettings.look, px)
        val out = Bitmap.createBitmap(filtered, bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val compressed = ByteArrayOutputStream().use { o ->
            out.compress(Bitmap.CompressFormat.JPEG, 95, o); o.toByteArray()
        }
        out.recycle()
        viewModel.saveJpeg(compressed)
    } catch (t: Throwable) {
        viewModel.saveJpeg(bytes)
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (ui.isCapturing) return
        viewModel.setCapturing(true)
        haptic()
        applyFlash(capture)
        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = imageToJpegBytes(image)
                    try { image.close() } catch (_: Throwable) { }
                    val uri = bytes?.let {
                        when {
                            ui.mode == CaptureMode.FOOD -> applyFoodAndSave(it)
                            appSettings.look != com.xperiaprocam.domain.model.CaptureLook.NATURAL ->
                                applyLookAndSave(it)
                            else -> viewModel.saveJpeg(it)
                        }
                    }
                    view.post { viewModel.onSaved(uri) }
                }

                override fun onError(exception: ImageCaptureException) {
                    view.post { viewModel.onCaptureError(exception.cause ?: exception) }
                }
            },
        )
    }

    // --- Burst (night / stars) — manual exposure only during the burst ------

    fun takeBurst() {
        if (ui.isCapturing) return
        if (viewModel.batteryCritical()) {
            scope.launch { snackbar.showSnackbar("Batería crítica: procesamiento suspendido") }
            return
        }
        val total = ui.mode.burstFrames
        viewModel.beginBurst(total)
        haptic()

        scope.launch {
            try {
                if (ui.mode == CaptureMode.STARS) {
                    val stable = runCatching { stability.isStable() }.getOrDefault(true)
                    if (!stable) snackbar.showSnackbar("Teléfono en movimiento: usa un trípode para astrofoto")
                }
                val burstCapture: ImageCapture
                val manual = if (ui.mode == CaptureMode.STARS) {
                    manualResolver.resolve(facingBack = ui.lensFacing == LensFacing.BACK)
                } else null
                burstCapture = if (manual != null) {
                    buildManualStarCapture(manual)
                } else {
                    imageCapture ?: buildImageCapture(ui.mode)
                }
                burstCapture.flashMode = ImageCapture.FLASH_MODE_OFF

                if (burstCapture !== imageCapture) {
                    rebindWithCapture(burstCapture)
                    delay(900) // let the manual session settle; preview recovers afterwards
                }

                val isHdr = ui.mode == CaptureMode.HDR
                val memClass = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
                val maxSide = when {
                    memClass >= 384 -> 4032
                    memClass >= 256 -> 3072
                    else -> 2048
                }
                val pipeline = NightStackPipeline(maxSide = maxSide) { w, h ->
                    if (isHdr) HdrMerger(w, h) else FrameMerger(w, h)
                }

                val frameTimeoutMs = if (ui.mode == CaptureMode.STARS) 40_000L else 20_000L

                if (isHdr) {
                    // EV bracketing -2/0/+2 via exposure compensation, reset afterwards.
                    val exposure = camera?.cameraInfo?.exposureState
                    val evOffsets = if (exposure?.isExposureCompensationSupported == true &&
                        exposure.exposureCompensationStep.toFloat() > 0f
                    ) listOf(-2f, 0f, 2f) else listOf(0f)
                    var processed = 0
                    var failures = 0
                    for (ev in evOffsets) {
                        if (viewModel.burstCancelled()) break
                        when (viewModel.thermalState()) {
                            ThermalGuardian.State.CRITICAL -> {
                                snackbar.showSnackbar("Temperatura alta: captura detenida, se guarda lo capturado")
                                break
                            }
                            ThermalGuardian.State.HOT -> delay(3_000)
                            ThermalGuardian.State.WARM -> delay(1_000)
                            else -> Unit
                        }
                        val idx = if (exposure != null && evOffsets.size > 1) {
                            (ev / exposure.exposureCompensationStep.toFloat()).roundToInt()
                                .coerceIn(
                                    exposure.exposureCompensationRange.lower,
                                    exposure.exposureCompensationRange.upper,
                                )
                        } else 0
                        camera?.cameraControl?.setExposureCompensationIndex(idx)
                        delay(350) // settle before the shot
                        val frame = nextFrameSafe(burstCapture, frameTimeoutMs)
                        if (frame == null) {
                            if (++failures >= 3) break else continue
                        }
                        failures = 0
                        withContext(Dispatchers.Default) { pipeline.addJpeg(frame.first, frame.second) }
                        processed++
                        viewModel.onBurstFrame(processed)
                    }
                    camera?.cameraControl?.setExposureCompensationIndex(0)
                } else {
                    var processed = 0
                    var failures = 0
                    for (i in 1..total) {
                        if (viewModel.burstCancelled()) break
                        when (viewModel.thermalState()) {
                            ThermalGuardian.State.CRITICAL -> {
                                snackbar.showSnackbar("Temperatura alta: captura detenida, se guarda lo capturado")
                                break
                            }
                            ThermalGuardian.State.HOT -> delay(3_000)
                            ThermalGuardian.State.WARM -> delay(1_000)
                            else -> Unit
                        }
                        val frame = nextFrameSafe(burstCapture, frameTimeoutMs)
                        if (frame == null) {
                            if (++failures >= 3) break else continue
                        }
                        failures = 0
                        withContext(Dispatchers.Default) { pipeline.addJpeg(frame.first, frame.second) }
                        processed++
                        viewModel.onBurstFrame(processed)
                    }
                }
                val frames = pipeline.frameCount()
                if (viewModel.burstCancelled()) {
                    snackbar.showSnackbar("Proceso cancelado")
                } else if (frames < 2) {
                    snackbar.showSnackbar("No se pudieron capturar suficientes fotos")
                } else {
                    val resultBitmap = withContext(Dispatchers.Default) { pipeline.result() }
                    val bytes = withContext(Dispatchers.Default) {
                        resultBitmap?.let { bmp ->
                            ByteArrayOutputStream().use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 95, out); out.toByteArray()
                            }
                        }
                    }
                    resultBitmap?.recycle()
                    val uri = bytes?.let { viewModel.saveProcessedJpeg(it, ui.mode.fileSuffix ?: "STACK") }
                    snackbar.showSnackbar(
                        if (uri != null) "Guardado: $frames fotos apiladas"
                        else "No se pudo guardar el resultado"
                    )
                    uri?.let { viewModel.onSaved(it) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                viewModel.onCaptureError(e)
                snackbar.showSnackbar("Memoria insuficiente: reduce el tamaño del apilado")
            } catch (t: Throwable) {
                viewModel.onCaptureError(t)
            } finally {
                runCatching { rebindCurrent() }
                viewModel.endBurst()
            }
        }
    }

    // --- Panorama ---------------------------------------------------------------

    fun takePanorama() {
        if (ui.isCapturing || pano.active) return
        val total = pano.total
        viewModel.beginPano(total)
        haptic()

        scope.launch {
            try {
                val capture = imageCapture ?: buildImageCapture(CaptureMode.PANORAMA)
                val frames = mutableListOf<Pair<IntArray, Pair<Int, Int>>>()
                var last: Pair<IntArray, Pair<Int, Int>>? = null
                var failures = 0
                for (i in 1..total) {
                    if (viewModel.panoCancelled()) break
                    if (viewModel.thermalState() == ThermalGuardian.State.CRITICAL) {
                        snackbar.showSnackbar("Temperatura alta: panorámica detenida, procesando fotos capturadas")
                        break
                    }
                    val frame = nextFrameSafe(capture, 20_000L)
                    if (frame == null) {
                        if (++failures >= 3) break else continue
                    }
                    failures = 0
                    val decoded = withContext(Dispatchers.Default) {
                        decodeScaled(frame.first, frame.second, maxSide = 1280)
                    }
                    if (decoded != null) {
                        last?.let { frames.add(it) }
                        last = decoded
                    }
                    viewModel.onPanoFrame(i)
                    delay(1300) // user pans during this window
                }
                last?.let { frames.add(it) }

                if (viewModel.panoCancelled()) {
                    snackbar.showSnackbar("Panorámica cancelada")
                } else if (frames.size < 2) {
                    snackbar.showSnackbar("No se pudieron capturar suficientes fotos")
                } else {
                    val (w, h) = frames.first().second
                    val stitched = withContext(Dispatchers.Default) {
                        runCatching { PanoramaStitcher().stitch(frames.map { it.first }, w, h) }.getOrNull()
                    }
                    val bytes = stitched?.let { s ->
                        withContext(Dispatchers.Default) {
                            runCatching {
                                val bmp = Bitmap.createBitmap(s.pixels, s.width, s.height, Bitmap.Config.ARGB_8888)
                                ByteArrayOutputStream().use { o ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, o); o.toByteArray()
                                }.also { bmp.recycle() }
                            }.getOrNull()
                        }
                    }
                    val uri = bytes?.let { viewModel.saveProcessedJpeg(it, "PANO") }
                    snackbar.showSnackbar(
                        if (uri != null) "Panorámica guardada (${frames.size} fotos)"
                        else "No se pudo unir la panorámica"
                    )
                    uri?.let { viewModel.onSaved(it) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                viewModel.onCaptureError(e)
                snackbar.showSnackbar("Memoria insuficiente para generar la panorámica")
            } catch (t: Throwable) {
                viewModel.onCaptureError(t)
            } finally {
                viewModel.endPano()
            }
        }
    }

    // --- Video ---------------------------------------------------------------------

    fun startVideo() {
        if (video.recording) return
        if (provider == null) return
        haptic()
        if (!audioGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        runCatching {
            val name = FileNameGenerator.withSuffix("VIDEO").replace(".jpg", ".mp4")
            val values = ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, name)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/XperiaProCam")
            }
            val options = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(values).build()
            val pending = videoCapture.output.prepareRecording(context, options)
            if (audioGranted) pending.withAudioEnabled()
            activeRecording = pending.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> viewModel.videoStarted()
                    is VideoRecordEvent.Finalize -> {
                        viewModel.videoStopped()
                        activeRecording = null
                        if (!event.hasError()) {
                            scope.launch { snackbar.showSnackbar("Video guardado") }
                            if (event.outputResults.outputUri != Uri.EMPTY) {
                                viewModel.onSaved(event.outputResults.outputUri)
                            }
                        } else if (event.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA) {
                            scope.launch { snackbar.showSnackbar("Error al grabar video (${event.error})") }
                        }
                    }
                    else -> {}
                }
            }
        }.onFailure {
            scope.launch { snackbar.showSnackbar("No se pudo iniciar la grabación") }
        }
    }

    fun stopVideo() {
        activeRecording?.stop()
    }

    LaunchedEffect(video.recording) {
        while (video.recording) {
            delay(1000)
            if (viewModel.thermalState() == ThermalGuardian.State.CRITICAL) {
                snackbar.showSnackbar("Temperatura crítica: grabación detenida por seguridad")
                stopVideo()
                break
            }
            viewModel.videoTick()
        }
    }

    // --- Shutter dispatch ------------------------------------------------------------

    fun startCaptureSequence() {
        if (busy) return
        val timer = ui.timerSeconds
        val shoot: () -> Unit = {
            when (ui.mode) {
                CaptureMode.NIGHT, CaptureMode.STARS -> takeBurst()
                CaptureMode.PANORAMA -> takePanorama()
                CaptureMode.VIDEO -> startVideo()
                else -> takePhoto()
            }
        }
        if (timer == 0 || ui.mode == CaptureMode.VIDEO) { shoot(); return }
        scope.launch {
            var remaining = timer
            while (remaining > 0) {
                viewModel.setCountdown(remaining)
                delay(1000)
                remaining--
            }
            viewModel.setCountdown(0)
            shoot()
        }
    }

    // --- UI ---------------------------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tap: Offset ->
                    val point = previewView.meteringPointFactory.createPoint(tap.x, tap.y)
                    camera?.cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(point).build()
                    )
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    viewModel.setZoom(ui.zoomRatio - dragAmount / 40f)
                }
            },
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (appSettings.showGrid) CompositionGrid(Modifier.fillMaxSize())
        if (appSettings.showLevel) HorizonLevel(Modifier.fillMaxSize())

        var showQuickMenu by remember { mutableStateOf(false) }
        var showMoreModesDialog by remember { mutableStateOf(false) }

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenGallery) {
                Icon(Icons.Outlined.PhotoLibrary, "Galería", tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Unified Quick Options Button (Flash, Timer, Resolution, Grid)
                IconButton(onClick = { showQuickMenu = !showQuickMenu }) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Opciones rápidas",
                        tint = if (showQuickMenu) Color.Yellow else Color.White,
                    )
                }
                IconButton(onClick = viewModel::cycleFlash) {
                    Icon(
                        when (ui.flashMode) {
                            FlashMode.AUTO -> Icons.Filled.FlashAuto
                            FlashMode.ON -> Icons.Filled.FlashOn
                            FlashMode.OFF -> Icons.Filled.FlashOff
                        },
                        "Flash ${ui.flashMode}", tint = Color.White,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, "Ajustes", tint = Color.White)
                }
            }
        }

        // Quick Options Floater Card
        if (showQuickMenu && !busy) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 12.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xDD1E1E1E)),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Opciones rápidas", color = Color.White, fontSize = 14.sp)

                    // Flash row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Flash", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(FlashMode.AUTO to "Auto", FlashMode.ON to "On", FlashMode.OFF to "Off").forEach { (f, label) ->
                                FilterChip(
                                    selected = ui.flashMode == f,
                                    onClick = { if (ui.flashMode != f) viewModel.cycleFlash() },
                                    label = { Text(label, fontSize = 11.sp) },
                                )
                            }
                        }
                    }

                    // Timer row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Temporizador", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0 to "Off", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                                FilterChip(
                                    selected = ui.timerSeconds == sec,
                                    onClick = {
                                        while (ui.timerSeconds != sec) viewModel.cycleTimer()
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                )
                            }
                        }
                    }

                    // Video Resolution row (only when mode is video)
                    if (ui.mode == CaptureMode.VIDEO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Calidad", color = Color.White, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                videoQualities.forEach { q ->
                                    FilterChip(
                                        selected = videoQuality == q,
                                        onClick = { videoQuality = q },
                                        label = { Text(qualityLabel(q), fontSize = 11.sp) },
                                    )
                                }
                            }
                        }

                        // Video Frame Rate row (24, 30, 60 fps)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("FPS", color = Color.White, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(24 to "24", 30 to "30", 60 to "60").forEach { (f, label) ->
                                    FilterChip(
                                        selected = videoFps == f,
                                        onClick = { videoFps = f },
                                        label = { Text(label, fontSize = 11.sp) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        if (ui.countdownRemaining > 0) {
            Text(
                "${ui.countdownRemaining}",
                color = Color.White, fontSize = 72.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Burst / panorama progress overlay
        if (burst.active || pano.active) {
            val label = when {
                pano.active -> "Panorámica"
                ui.mode == CaptureMode.STARS -> "Modo estrellas"
                ui.mode == CaptureMode.HDR -> "HDR (-2/0/+2 EV)"
                else -> "Modo noche"
            }
            val frame = if (pano.active) pano.frame else burst.frame
            val total = if (pano.active) pano.total else burst.total
            val onCancel = if (pano.active) viewModel::requestCancelPano else viewModel::requestCancelBurst
            Card(modifier = Modifier.align(Alignment.Center).padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(label, color = Color.White)
                    if (pano.active) {
                        Text("Mueve lentamente →", color = Color.Yellow, fontSize = 18.sp)
                    }
                    Text("$frame/$total fotos", color = Color.White, fontSize = 22.sp)
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else frame / total.toFloat() },
                        modifier = Modifier.width(200.dp),
                    )
                    Button(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, contentDescription = null)
                        Text("Cancelar")
                    }
                }
            }
        } else if (ui.isCapturing) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp))
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 230.dp))

        // Pro panel
        if (ui.mode == CaptureMode.PRO && !busy) {
            ProPanel(
                pro = pro,
                ranges = proRanges,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 100.dp, bottom = 250.dp, start = 24.dp, end = 8.dp),
                onIso = { v -> viewModel.setProIso(v, proRanges) },
                onExposure = { ns -> viewModel.setProExposure(ns, proRanges) },
                onWb = viewModel::setProWb,
                onFocus = { d -> viewModel.setProFocus(d, proRanges) },
                onReset = viewModel::resetPro,
            )
        }

        // Bottom: mode chips + shutter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Lens zoom presets (16mm 0.6x, 24mm 1.0x, 85mm 3.5x, 125mm 5.2x native optics)
            if (ui.lensFacing == LensFacing.BACK) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    zoomPresets.forEach { z ->
                        val selected = kotlin.math.abs(ui.zoomRatio - z) < 0.05f
                        val label = if (z < 0.99f || (z % 1f != 0f)) {
                            "%.1fx".format(z)
                        } else {
                            "${z.roundToInt()}x"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setZoom(z) },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x66000000),
                                labelColor = Color.White,
                                selectedContainerColor = Color.Yellow,
                                selectedLabelColor = Color.Black,
                            ),
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                // Primary quick modes: Foto, Video, Pro
                listOf(CaptureMode.CLASSIC, CaptureMode.VIDEO, CaptureMode.PRO).forEach { mode ->
                    FilterChip(
                        selected = ui.mode == mode,
                        onClick = {
                            if (video.recording) stopVideo()
                            viewModel.setMode(mode)
                        },
                        label = { Text(mode.label) },
                        leadingIcon = {
                            Icon(modeIcon(mode), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.DarkGray,
                            labelColor = Color.White,
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }

                // If currently using a specialized mode, show it as selected chip
                if (ui.mode.isSpecialPhoto) {
                    FilterChip(
                        selected = true,
                        onClick = { showMoreModesDialog = true },
                        label = { Text(if (ui.mode == CaptureMode.HIGHRES) highResLabel else ui.mode.label) },
                        leadingIcon = {
                            Icon(modeIcon(ui.mode), contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.DarkGray,
                            labelColor = Color.White,
                            selectedContainerColor = Color.Yellow,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }

                // "Más" button to access specialized photo modes
                FilterChip(
                    selected = showMoreModesDialog,
                    onClick = { showMoreModesDialog = true },
                    label = { Text("Más") },
                    leadingIcon = {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "Más modos", modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0x66333333),
                        labelColor = Color.White,
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ui.lastPhotoUri,
                    contentDescription = "Última foto",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenGallery),
                )
                if (ui.mode == CaptureMode.VIDEO) {
                    Button(
                        onClick = { if (video.recording) stopVideo() else startVideo() },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                    ) {
                        Text(
                            if (video.recording) "■" else "●", fontSize = 30.sp,
                            color = if (video.recording) Color.White else Color.Red,
                        )
                    }
                } else {
                    Button(
                        onClick = ::startCaptureSequence,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                    ) { Text("●", fontSize = 34.sp) }
                }
                IconButton(onClick = viewModel::toggleLens) {
                    Icon(
                        Icons.Filled.FlipCameraAndroid, "Cambiar cámara", tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            if (video.recording) {
                Text(
                    "● REC %02d:%02d".format(video.elapsedSec / 60, video.elapsedSec % 60),
                    color = Color.Red, fontSize = 16.sp,
                )
            }
        }

        Text(
            "${ui.zoomRatio.roundToInt()}x",
            color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
        )

        // Specialized photo modes Dialog
        if (showMoreModesDialog) {
            val specialModes = listOf(
                CaptureMode.NIGHT to "Apila 6 tomas para máxima nitidez nocturna",
                CaptureMode.HDR to "3 exposiciones (-2/0/+2 EV) de alto rango dinámico",
                CaptureMode.STARS to "Astrofotografía de larga exposición y apilado",
                CaptureMode.HIGHRES to "Máxima resolución y detalle del sensor",
                CaptureMode.MACRO to "Enfoque cercano de precisión con ultra gran angular",
                CaptureMode.PANORAMA to "Barrido panorámico de alta resolución",
                CaptureMode.FOOD to "Realce de color y contraste para alimentos",
            )
            AlertDialog(
                onDismissRequest = { showMoreModesDialog = false },
                title = { Text("Modos especiales de foto", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        specialModes.forEach { (mode, desc) ->
                            val isSelected = ui.mode == mode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (video.recording) stopVideo()
                                        viewModel.setMode(mode)
                                        showMoreModesDialog = false
                                    },
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF333333) else Color(0xFF1E1E1E),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        modeIcon(mode),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Yellow else Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (mode == CaptureMode.HIGHRES) highResLabel else mode.label,
                                            color = if (isSelected) Color.Yellow else Color.White,
                                            fontSize = 15.sp,
                                        )
                                        Text(
                                            desc,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoreModesDialog = false }) {
                        Text("Cerrar", color = Color.Yellow)
                    }
                },
                containerColor = Color(0xFF121212),
            )
        }
    }

}

@Composable
private fun ProPanel(
    pro: ProUiState,
    ranges: ProControlsResolver.ProRanges?,
    modifier: Modifier,
    onIso: (Int?) -> Unit,
    onExposure: (Long?) -> Unit,
    onWb: (Int?) -> Unit,
    onFocus: (Float?) -> Unit,
    onReset: () -> Unit,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).width(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pro", color = Color.White)

            // ISO
            val isoRange = ranges?.isoRange ?: (50..6400)
            Text("ISO: ${pro.iso ?: "Auto"}", color = Color.White, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = pro.iso != null,
                    onCheckedChange = { on -> onIso(if (on) (pro.iso ?: isoRange.first) else null) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                Slider(
                    value = (pro.iso ?: isoRange.first).toFloat(),
                    onValueChange = { onIso(it.toInt().coerceIn(isoRange.first, isoRange.last)) },
                    valueRange = isoRange.first.toFloat()..isoRange.last.toFloat(),
                    enabled = pro.iso != null,
                    modifier = Modifier.width(140.dp),
                )
            }

            // Shutter presets
            Text("Obturador", color = Color.White, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SHUTTER_PRESETS) { (label, ns) ->
                    FilterChip(
                        selected = (ns == null && pro.exposureNs == null) ||
                            (ns != null && pro.exposureNs == ns),
                        onClick = { onExposure(ns) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.DarkGray, labelColor = Color.White,
                            selectedContainerColor = Color.White, selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }

            // White balance presets (public AWB modes)
            Text("Balance de blancos", color = Color.White, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(WB_PRESETS) { (label, mode) ->
                    FilterChip(
                        selected = pro.wbMode == mode,
                        onClick = { onWb(mode) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.DarkGray, labelColor = Color.White,
                            selectedContainerColor = Color.White, selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }

            // Focus
            if (ranges?.minFocusDistance != null) {
                Text("Foco: ${pro.focusDistance ?: "Auto"} m⁻¹", color = Color.White, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = pro.focusDistance != null,
                        onCheckedChange = { on ->
                            onFocus(if (on) (pro.focusDistance ?: 0.5f) else null)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Slider(
                        value = pro.focusDistance ?: 0.5f,
                        onValueChange = { onFocus(it) },
                        valueRange = 0f..(ranges.minFocusDistance ?: 10f).coerceAtLeast(0.5f),
                        enabled = pro.focusDistance != null,
                        modifier = Modifier.width(140.dp),
                    )
                }
            }

            Button(onClick = onReset) { Text("Todo auto") }
        }
    }
}

/** Preview use case with Pro interop applied. */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun buildPreviewWithPro(
    pro: ProUiState,
    ranges: ProControlsResolver.ProRanges?,
    mode: CaptureMode,
    surfaceProvider: Preview.SurfaceProvider,
): Preview {
    val builder = Preview.Builder()
    applyProInteropTo(Camera2Interop.Extender(builder), mode, pro, ranges)
    return builder.build().also { it.setSurfaceProvider(surfaceProvider) }
}

/** Builds zoom presets from the logical camera's real zoom range (lens jump points). */
internal fun buildZoomPresets(minRatio: Float, maxRatio: Float): List<Float> {
    val presets = mutableListOf<Float>()
    if (minRatio < 0.99f) presets += minRatio
    presets += 1f
    // Xperia 1 IV: 16mm (0.6x), 24mm (1.0x), 85mm (3.5x), 125mm (5.2x), and extended digital zoom
    listOf(2f, 3.5f, 5.2f, 10f).forEach { z -> if (z <= maxRatio) presets += z }
    return presets.sorted().take(6)
}

/** Video pipeline with chosen quality and fps; uses standard CameraX video pipeline for guaranteed HAL stability. */
internal fun buildVideoCapture(quality: Quality, fps: Int = 30): VideoCapture<Recorder> {
    val recorder = Recorder.Builder()
        .setQualitySelector(
            QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
        )
        .build()
    val builder = VideoCapture.Builder(recorder)
    if (fps != 30) {
        val extender = Camera2Interop.Extender(builder)
        // Set dynamic AE target FPS range allowing high frame rates smoothly without pinning
        val minFps = if (fps >= 60) 30 else fps
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            android.util.Range(minFps, fps)
        )
    }
    return builder.build()
}


internal fun qualityMpx(q: Quality): Int = when (q) {
    Quality.UHD -> 8
    Quality.FHD -> 2
    Quality.HD -> 1
    Quality.SD -> 0
    else -> 0
}

internal fun qualityLabel(q: Quality): String = when (q) {
    Quality.UHD -> "4K"
    Quality.FHD -> "1080p"
    Quality.HD -> "720p"
    Quality.SD -> "480p"
    else -> q.toString()
}

/** Builds the ImageCapture use case with Pro interop and high-res selector. */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun buildImageCaptureFor(
    mode: CaptureMode,
    pro: ProUiState,
    ranges: ProControlsResolver.ProRanges?,
    isBackCamera: Boolean = true,
): ImageCapture {
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    if (mode == CaptureMode.HIGHRES) {
        ranges?.maxJpegSize?.let { (w, h) ->
            builder.setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(w, h),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
        }
    }
    val extender = Camera2Interop.Extender(builder)
    if (isBackCamera) {
        extender.setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON,
        )
    }
    // Qualcomm Spectra ISP / Sony Mobile Camera HAL high quality overrides:
    extender.setCaptureRequestOption(
        CaptureRequest.JPEG_QUALITY,
        100.toByte(),
    )

    extender.setCaptureRequestOption(
        CaptureRequest.EDGE_MODE,
        CaptureRequest.EDGE_MODE_HIGH_QUALITY,
    )
    extender.setCaptureRequestOption(
        CaptureRequest.NOISE_REDUCTION_MODE,
        CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY,
    )
    extender.setCaptureRequestOption(
        CaptureRequest.HOT_PIXEL_MODE,
        CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY,
    )
    extender.setCaptureRequestOption(
        CaptureRequest.SHADING_MODE,
        CaptureRequest.SHADING_MODE_HIGH_QUALITY,
    )
    applyProInteropTo(extender, mode, pro, ranges)
    return builder.build()
}


/** Applies manual ISO/exposure/focus to a use case when Pro mode has values set. */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyProInteropTo(
    extender: Camera2Interop.Extender<*>,
    mode: CaptureMode,
    pro: ProUiState,
    ranges: ProControlsResolver.ProRanges?,
) {
    if (mode != CaptureMode.PRO || !pro.anyManual || ranges == null) return
    if (pro.iso != null || pro.exposureNs != null) {
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        pro.iso?.let { iso ->
            ranges.isoRange?.let { r ->
                extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso.coerceIn(r.first, r.last))
            }
        }
        pro.exposureNs?.let { ns ->
            ranges.exposureNsRange?.let { r ->
                extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, ns.coerceIn(r.first, r.last))
            }
        }
    }
    if (pro.focusDistance != null) {
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, pro.focusDistance!!)
    }
    pro.wbMode?.let { mode ->
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, mode)
    }
}

/** Star-burst ImageCapture with manual exposure pinned (used only during the burst). */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun buildManualStarCapture(manual: ManualExposureResolver.ManualExposure): ImageCapture {
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manual.iso)
        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, manual.exposureNs)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
    return builder.build()
}

private fun modeIcon(mode: CaptureMode) = when (mode) {
    CaptureMode.CLASSIC -> Icons.Filled.PhotoCamera
    CaptureMode.NIGHT -> Icons.Filled.DarkMode
    CaptureMode.HDR -> Icons.Filled.HighQuality
    CaptureMode.PRO -> Icons.Filled.Tune
    CaptureMode.HIGHRES -> Icons.Filled.HighQuality
    CaptureMode.MACRO -> Icons.Filled.FilterCenterFocus
    CaptureMode.PANORAMA -> Icons.Filled.Landscape
    CaptureMode.FOOD -> Icons.Filled.Restaurant
    CaptureMode.STARS -> Icons.Filled.Star
    CaptureMode.VIDEO -> Icons.Filled.Videocam
}

/** Copies the JPEG bytes out of the captured image plane; caller closes the image. */
private fun imageToJpegBytes(image: ImageProxy): ByteArray? = try {
    val buffer = image.planes[0].buffer
    ByteArray(buffer.remaining()).also { buffer.get(it) }
} catch (t: Throwable) {
    null
}

/** Decodes a JPEG to (pixels, w to h) scaled so the longest side <= maxSide. */
private fun decodeScaled(
    jpeg: ByteArray,
    rotationDegrees: Int,
    maxSide: Int,
): Pair<IntArray, Pair<Int, Int>>? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    var bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts) ?: return null
    val longSide = maxOf(bmp.width, bmp.height)
    if (longSide > maxSide) {
        val scale = maxSide.toFloat() / longSide
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).roundToInt(),
            (bmp.height * scale).roundToInt(),
            true,
        )
        bmp.recycle()
        bmp = scaled
    }
    if (rotationDegrees != 0) {
        val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        bmp.recycle()
        bmp = rotated
    }
    val px = IntArray(bmp.width * bmp.height)
    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    val size = bmp.width to bmp.height
    bmp.recycle()
    px to size
}.getOrNull()

@Composable
private fun CompositionGrid(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (i in 1..2) {
            drawLine(Color.White.copy(alpha = 0.35f), Offset(w * i / 3, 0f), Offset(w * i / 3, h), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, h * i / 3), Offset(w, h * i / 3), strokeWidth = 1.5f)
        }
    }
}

@Composable
private fun HorizonLevel(modifier: Modifier) {
    Canvas(modifier) {
        drawLine(
            Color.Yellow.copy(alpha = 0.6f),
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            strokeWidth = 2f,
        )
    }
}
