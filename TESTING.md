# Plan de pruebas

## Unitarias (automatizadas, `./gradlew test`)

- `FileNameGeneratorTest`: patrón `XPC_YYYYMMDD_HHMMSS(.jpg)`, sufijos `_HDR`/`_NIGHT`, unicidad por segundo.
- `ReportFormatterTest`: texto y JSON del diagnóstico; capacidades ausentes como "No disponible"; sin fingerprint en el reporte exportable; vendor tags Sony formateados.

## Integración (pendientes de dispositivo)

Pendiente de un SO-51C o emulador conectado: apertura de cámara, captura JPEG verificable en `MediaStore`, cambio de cámara, rotación, permiso denegado, falta de espacio. Añadir con `androidTest` + Espresso cuando haya dispositivo.

## Manuales (escenas)

Pendiente de ejecutar en el dispositivo con: día soleado, interior con poca luz, escena nocturna, objeto en movimiento, zoom digital, ráfaga de 50 capturas (verificar ausencia de fugas de memoria con Android Profiler).

## Criterios de calidad objetivo

- 0 crashes en 100 capturas consecutivas.
- 0 fugas de memoria tras 50 capturas.
- Todos los permisos solicitados en contexto. Funcionamiento offline y sin root.
