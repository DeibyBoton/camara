# Changelog

## 0.3.1 (2026-10-12)

### Añadido y Mejorado

- **Ajustes de Perfiles Creativos**: selección directa en la pantalla de Ajustes de los perfiles `Natural`, `Vívido`, `Mono` y `Cálido` con persistencia en DataStore.
- **Preparación de Captura RAW / DNG**: opción en Ajustes para activar guardado en DNG, soporte de almacenamiento en `MediaStoreSaver` bajo formato Adobe DNG en `Pictures/XperiaProCam` y método con pruebas en `FileNameGenerator`.
- **Suite de pruebas unitarias ampliada**:
  - Tests de fusión por exposición y ponderación de tonos medios en `HdrMergerTest`.
  - Tests de perfiles de color y monocromo en `LookFilterTest`.
  - Tests de nombrado DNG en `FileNameGeneratorTest`.
- **Correcciones de compilación**: resolución de tipos para `Quality.UHD` en CameraX 1.4.1 e implementaciones correctas de interfaz con `override` en `FrameMerger`.

## 0.3.0 (2026-10-12)

### Corregido

- **Preview fluido en modo Estrellas**: la exposición manual (ISO 3200 / 2 s) ya no se aplica a toda la sesión de cámara; solo se activa durante la ráfaga y después se restaura la sesión automática.

### Añadido

- Sistema completo de modos con chips desplazables y detección real de capacidades:
  - **Foto (clásico)**: AE estándar.
  - **Retrato**: bokeh por hardware vía extensión BOKEH de CameraX (si el HAL la expone; si no, el chip queda deshabilitado).
  - **Noche** y **Estrellas**: ya existentes (v0.2.0); Estrellas ahora avisa si el teléfono no está estable (acelerómetro) antes de la ráfaga.
  - **Pro**: panel con ISO manual, presets de obturador (1/1000…2 s) y enfoque manual en dioptrías, aplicados vía Camera2 Interop al preview y captura; botón "Todo auto".
  - **Alta Resolución**: selector de resolución máxima JPEG que reporta el sensor (en el SO-51C: 12 MP; el chip muestra el valor real).
  - **Macro**: cambia al lente ultra gran angular (cámara trasera con sensor físico más pequeño) si existe.
  - **Panorámica**: barrido guiado de 8 fotos con costura por traslación y degradado en las uniones (`XPC_..._PANO.jpg`).
  - **Comida**: perfil cálido post-captura (temperatura, saturación y contraste) sin destruir el original de la galería del sistema (`XPC_..._FOOD.jpg`).
  - **Video**: grabación a la máxima calidad soportada (Recorder) con permiso `RECORD_AUDIO` solicitado en contexto y audio opcional (`XPC_..._VIDEO.mp4`).
- Modos no implementados (Cine, Slow-Mo, Time-Lapse, Director, Acción, Una Toma, Borrador IA) visibles como chips deshabilitados con motivo.

### Verificado en dispositivo (SO-51C, Android 14)

- Foto clásica, Pro 1/1000, Comida, Panorámica (8 fotos unidas), Video mp4 (4K aprox., 119 MB) y permiso de audio en contexto; preview vivo en Estrellas; sin crashes.

## 0.2.0 (2026-10-12)

### Añadido

- Modo Noche: ráfaga de 6 fotos, alineación por correlación (FrameAligner) y apilado promedio (FrameMerger) con reducción de ruido; progreso, cancelación, pantalla encendida durante el proceso y guardado como `XPC_..._NIGHT.jpg`.
- Modo Estrellas: apilado de 12 fotos con exposición manual (ISO/exposición fijados vía Camera2 Interop, acotados a los rangos que reporta el HAL); degrada a exposición automática si la cámara no soporta sensor manual; guardado como `XPC_..._STARS.jpg`.
- Selector de modos (Auto/Noche/Estrellas) con chips en la pantalla principal.
- Bloqueo de procesamiento con batería crítica (<10% sin cargar).
- Tests unitarios del alineador (traslación pura, idénticos, planos) y del apilador (promedio, límites, ruido).

### Verificado en dispositivo (SO-51C, Android 14)

- Capturas AUTO, NIGHT y STARS guardadas correctamente en MediaStore; sin crashes.

## 0.1.0 (2026-10-12)

### Añadido

- Fase 0: pantalla de Diagnóstico de cámara con detección de cámaras, nivel Camera2, RAW, sensor manual, burst, rangos ISO/exposición/compensación, resoluciones JPEG/RAW, vendor tags Sony, flash y estabilización; exportación del reporte a JSON/texto en almacenamiento privado.
- Fase 1: MVP de cámara — preview CameraX, captura JPEG a MediaStore (`Pictures/XperiaProCam`), cambio de lente, flash auto/on/off, temporizador 3/5/10 s, zoom digital 1x–10x por arrastre, tap-to-focus, grid, nivel horizontal, miniatura de última foto, ajustes persistidos (grid, nivel, háptica, temporizador por defecto).
- Tests unitarios de nombres de archivo y formateador del reporte.
