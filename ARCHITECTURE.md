# Arquitectura

Módulo único con capas Clean Architecture y MVVM + flujo de datos unidireccional.

```
app/src/main/java/com/xperiaprocam/
  MainActivity.kt            # Entrada, puerta de permisos en contexto
  presentation/
    camera/                  # CameraScreen (preview CameraX + controles MVP), CameraViewModel
    diagnostics/             # DiagnosticsScreen, DiagnosticsViewModel
    settings/                # SettingsScreen, SettingsViewModel
    navigation/              # NavHost (camera → settings → diagnostics)
    theme/                   # Tema oscuro por defecto
  domain/model/              # Modelos puros: DiagnosticsReport, AppSettings,
                             # FileNameGenerator, ReportFormatter (sin dependencias Android)
  data/
    camera/                  # (futuro) repositorio de controles avanzados Camera2 Interop
    diagnostics/             # CameraDiagnosticsRepository (CameraManager), ReportExporter
    storage/                 # MediaStoreSaver
    settings/                # SettingsRepository (DataStore)
  core/permissions/          # Manejo de permisos en contexto (extensible en fases futuras)
```

- Inyección con Hilt (`@HiltViewModel`, `@HiltAndroidApp`).
- Asincronía con Coroutines/Flow; ningún acceso a cámara en el hilo principal directo.
- CameraX (`Preview` + `ImageCapture` + `CameraControl`); la superficie del `PreviewView` es estable ante rotación y la cámara se re-bind solo al cambiar de lente.
- El dominio (`domain/model`) no depende de Android y es testeable con JUnit puro.

## Fases futuras

- Fase 2 (Modo Pro): ampliar `data/camera` con Camera2 Interop (`SENSOR_SENSITIVITY`, `SENSOR_EXPOSURE_TIME`, etc.).
- Fase 3 (RAW/DNG): `ImageReader` + `DngCreator` en un caso de uso propio.
- Fase 4 (vendor tags Sony): panel "Sony Labs" leyendo `REQUEST_AVAILABLE_REQUEST_KEYS`.
