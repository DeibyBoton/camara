# Xperia ProCam

App de cámara avanzada para Sony Xperia SO-51C (NTT docomo). Fase 0 (diagnóstico de capacidades) y Fase 1 (MVP de cámara) implementadas. Funciona sin root, sin bootloader desbloqueado y 100% offline.

## Compilación

Requisitos: JDK 17+, Android SDK (platform 35, build-tools 35).

```bash
cp local.properties.tpl local.properties   # ajusta sdk.dir si hace falta
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Instalar en dispositivo (bootloader bloqueado OK si la firma es admitida; con `adb install -r app-debug.apk` no hay problema):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests y lint:

```bash
./gradlew test lint
```

## Funciones (v0.3.0 — sistema de modos completo)

- **Foto**: AE estándar con flash, temporizador, zoom, tap-to-focus, grid y nivel.
- **Retrato**: bokeh por hardware (extensión BOKEH si el HAL la expone).
- **Noche**: 6 fotos alineadas y apiladas (`_NIGHT.jpg`).
- **Estrellas**: 12 fotos con exposición manual (ISO/exposición fijos dentro de rangos del HAL) apiladas; aviso de trípode por acelerómetro; preview fluido (la exposición manual solo se aplica durante la ráfaga).
- **Pro**: ISO, obturador (1/1000…2 s) y enfoque manuales vía Camera2 Interop, con "Todo auto".
- **Alta Res.**: resolución máxima JPEG del sensor (chip con MP reales).
- **Macro**: lente ultra gran angular cuando existe.
- **Panorámica**: barrido de 8 fotos con costura (`_PANO.jpg`).
- **Comida**: perfil cálido post-captura (`_FOOD.jpg`).
- **Video**: máxima calidad soportada, audio opcional con permiso en contexto (`_VIDEO.mp4`).
- Modos pendientes (Cine, Slow-Mo, Time-Lapse, Director, Acción, Una Toma, Borrador IA) deshabilitados con explicación.
- Diagnóstico de capacidades (Ajustes → Diagnóstico de cámara) con exporte JSON/texto.

## Permisos

Solo `CAMERA` (en contexto) y `VIBRATE`. Sin almacenamiento, sin internet, sin trackers.
