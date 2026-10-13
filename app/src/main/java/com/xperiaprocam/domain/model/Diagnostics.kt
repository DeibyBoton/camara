package com.xperiaprocam.domain.model

/** Pure domain models for the camera diagnostics report (Phase 0). */

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val buildFingerprint: String,
)

data class CameraInfo(
    val id: String,
    val facing: String,
    val hardwareLevel: String,
    val supportsRaw: Boolean,
    val supportsManualSensor: Boolean,
    val supportsBurst: Boolean,
    val isoRange: String?,
    val exposureRange: String?,
    val exposureCompensationRange: String?,
    val jpegResolutions: List<String>,
    val rawResolutions: List<String>,
    val sonyVendorTags: List<String>,
    val hasFlash: Boolean,
    val stabilizationModes: List<String>,
)

data class DiagnosticsReport(
    val device: DeviceInfo,
    val cameras: List<CameraInfo>,
    val camera2ExtensionsAvailable: Boolean,
)

data class AppSettings(
    val showGrid: Boolean = true,
    val showLevel: Boolean = false,
    val hapticFeedback: Boolean = true,
    val defaultTimerSeconds: Int = 0,
    val look: CaptureLook = CaptureLook.NATURAL,
    val rawCapture: Boolean = false,
)
