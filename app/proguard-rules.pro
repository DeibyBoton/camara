# Keep CameraX interop and vendor tag reflection surface
-keep class androidx.camera.camera2.interop.** { *; }
# Don't log build fingerprints in release logs
-assumenosideeffects class android.util.Log { public static *** d(...); }
