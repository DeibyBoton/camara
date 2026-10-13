# Seguridad

## Permisos

| Permiso | Uso | Momento de solicitud |
|---|---|---|
| `CAMERA` | Preview y captura | Al primer uso, con explicación |
| `VIBRATE` | Feedback háptico al capturar | Nunca se solicita (normal) |

No se solicitan: ubicación, micrófono, contactos, SMS, almacenamiento (`MANAGE_EXTERNAL_STORAGE` prohibido). Las capturas se guardan vía `MediaStore`, que no requiere permiso de almacenamiento para las propias imágenes de la app.

## Datos

- Sin telemetría, sin anuncios, sin cuentas, sin red (la app no declara `INTERNET`).
- El reporte de diagnóstico se guarda en almacenamiento privado de la app y no contiene datos personales (el fingerprint de build se recopila pero se excluye del reporte exportable).
- Sin claves, tokens ni secretos en el código. Sin código dinámico descargado. Sin WebView.
- R8/ProGuard activado en release con reglas mínimas.
- Logs de depuración eliminados en release (`-assumenosideeffects` sobre `Log.d`).
