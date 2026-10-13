# Roadmap

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Diagnóstico de capacidades + reporte exportable | ✅ Implementada |
| 1 | MVP de cámara (preview, JPEG, flash, timer, zoom, tap-to-focus, grid, nivel, ajustes) | ✅ Implementada |
| 2 | Modo Pro (ISO, exposición, enfoque manual; WB por temperatura no soportado por Camera2 pública) | ✅ Parcial (v0.3.0): ISO/obturador/foco manuales; histograma/zebras pendientes |
| 3 | RAW/DNG (JPEG+DNG simultáneo, DngCreator, metadatos) | ⏳ Pendiente (diagnóstico confirma RAW disponible) |
| 4 | Vendor tags Sony ("Sony Labs", pruebas A/B, registro de aceptación) | ⏳ Pendiente |
| 5 | HDR y modo noche (ráfaga, alineación, fusión, apilado) | ✅ Implementada (Modo Noche apilado + HDR multi-exposición fusionado con HdrMerger y tests) |
| — | Modo estrellas (captura de estrellas, idea avanzada futura) | ✅ Entregado (v0.2.0), con aviso de trípode y preview fluido (v0.3.0) |
| — | Modos extra (Alta Res., Macro, Panorámica, Comida) + Selector Óptico Multi-Lente (0.6x, 1x, 2x, 3.5x, 5.2x, 10x) | ✅ Entregados (v0.3.1, Retrato bokeh HAL descartado por inestabilidad de la extensión OEM) |
| 7 | Video avanzado (opcional) | 🔶 Parcial: video estándar/UHD a máxima calidad y selector de sensor fluido ✅ (v0.3.1) |
| — | IA (Una Toma, Borrador Mágico) | ⏳ Pendiente (requieren modelos ML) |
| 6 | Perfiles de color y edición (LUTs / Looks creativos, antes/después) | ✅ Implementada (LookFilter con Natural/Vivid/Mono/Warm + FoodFilter y tests) |

Antes de cada fase avanzada se consulta el diagnóstico de la Fase 0 para decidir qué controles se exponen.
