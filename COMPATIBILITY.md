# Matriz de capacidades

La app detecta todo en tiempo de ejecución desde `CameraCharacteristics` (Fase 0, pantalla de Diagnóstico). No asume nada del SO-51C; esto es lo que reporta y cómo se comporta si falta:

| Capacidad | Fuente de detección | Si no está disponible |
|---|---|---|
| Nivel Camera2 | `INFO_SUPPORTED_HARDWARE_LEVEL` | Se muestra "No disponible"; CameraX sigue operando en LEGACY |
| RAW/DNG | `REQUEST_AVAILABLE_CAPABILITIES_RAW` | Se muestra "No disponible" (captura RAW llegará en Fase 3, solo si está soportado) |
| Sensor manual | `REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR` | Controles Pro (Fase 2) se ocultarán |
| Burst | `REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE` | HDR/noche (Fase 5) se deshabilitarán |
| Rangos ISO/exposición | `SENSOR_INFO_SENSITIVITY_RANGE` / `SENSOR_INFO_EXPOSURE_TIME_RANGE` | "No disponible" |
| Vendor tags Sony | `REQUEST_AVAILABLE_REQUEST_KEYS` filtrado por `sony` | "ninguno"; panel Sony Labs vacío |
| Flash | `FLASH_INFO_AVAILABLE` | Modo flash inofensivo (CameraX lo ignora) |
| Extensiones Camera2 | `CameraManager.cameraExtensionCharacteristics` (API 31+) | Se reporta "No" |

Mínimo: Android 10 (API 29) con Camera2. Cámara trasera obligatoria; frontal opcional (si no existe, el botón de cambio no hace efecto y CameraX reporta error controlado).
