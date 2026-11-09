# Demostración de sonido y vibración: Python → Firebase → ViaTech

## Qué hace
La app muestra una banda «Última alerta · RIESGO SIMULADO», reproduce el sonido de notificación elegido en Android (máximo 1,5 segundos) y solicita dos pulsos de vibración cuando recibe una lectura nueva de riesgo. El criterio coincide con el controlador y el modelo IoT: distancia válida menor de 50 cm y PIR activado. El evento debe estar confirmado por Firestore y marcado como simulado.

La banda indica la última alerta recibida, no garantiza que el riesgo siga presente. «Entendido» la cierra sin modificar ni borrar el evento. Las lecturas normales siguen apareciendo en Eventos/Sensores.

## Pasos de demostración
1. Compilar/instalar la versión actualizada **cloudDebug** en Android. localDebug usa los emuladores Firebase y no recibe datos de la nube.
2. Iniciar sesión con una cuenta autorizada. En Configuración → Alertas del controlador Python activar Sonido de riesgo y/o Vibración de riesgo.
3. Mantener ViaTech en primer plano, con pantalla desbloqueada. Esperar la carga del historial y que desaparezca el mensaje de caché/conexión sin confirmar.
4. En el PC abrir simulador_python/controlador.py (o ABRIR_SIMULADOR.py). Ambos abren la misma ventana.
5. Pulsar Conectar Firebase y esperar la autenticación. No ejecutar simultáneamente otro simulador del mismo dispositivo.
6. Pulsar «Preparar normal: 150 cm / sin PIR» y después «Enviar a Firebase». Verificar el nuevo evento informativo; no debe sonar ni vibrar.
7. Pulsar «Preparar riesgo: 35 cm / PIR» y después «Enviar a Firebase». Verificar el evento, la banda de alerta y las señales habilitadas.
8. Para repetir, esperar al menos 10 segundos y enviar otra lectura. Un ID ya recibido no vuelve a alertar.
9. Desactivar sonido y repetir: debe permanecer la banda y vibrar si está habilitado. Desactivar también vibración: debe quedar únicamente la banda.
10. Poner la app en segundo plano o bloquear la pantalla: no debe emitir señales. Volver a abrir carga una línea base silenciosa. Enviar una lectura nueva después.
11. Probar desconexión/reconexión: los eventos se recuperan en el historial sin reproducir alertas acumuladas.
12. Comparar el eventoId en Eventos y Firebase. Registrar resultado observado en el teléfono; las pruebas SDK no sustituyen esta observación.

Los botones de preparación solo cambian los valores; no envían solos. Desactivar «Repetir cada 5 segundos» durante la demostración manual.

## Límites y preferencias
- Solo funciona con sesión autorizada, Firebase confirmado y app visible e interactiva. No se implementaron FCM ni servicios de alerta en segundo plano.
- La primera carga, caché y primera carga tras reconexión no emiten alertas.
- Solo detecciones posteriores a activar la pantalla, con una antigüedad de hasta 30 segundos. Una cola antigua subida ahora no genera alarma. La hora del PC y Android debe estar sincronizada; se toleran hasta 5 segundos de adelanto.
- Una alerta por lote, máximo una cada 10 segundos. El historial conserva también los eventos que no producen sonido.
- Preferencias de sonido y vibración persistentes por instalación, independientes y activadas inicialmente. No cambian permisos de Firebase.
- Respeta el modo silencio y No molestar; en modo vibración no reproduce audio. No sube el volumen. Sin motor de vibración o sonido configurado, la banda visual permanece.
- No hay acceso a micrófono, ubicación ni Bluetooth. Se agregó únicamente VIBRATE, permiso normal de instalación. No se pide permiso de notificaciones porque no se publican notificaciones del sistema.
- Al pausar, cerrar sesión, cambiar preferencias o pulsar Entendido se detienen las señales.

## Arquitectura y referencias
MonitoringRepository observa eventos → ViaTechViewModel valida sesión/ciclo de vida → RiskAlertGate filtra historial, timestamps y duplicados → AlertFeedback reproduce señales nativas. Compose muestra el estado y las preferencias; no consulta Firestore directamente.

La implementación usa [VibrationEffect y el permiso VIBRATE](https://developer.android.com/develop/ui/views/haptics/haptics-apis), [Ringtone](https://developer.android.com/reference/android/media/Ringtone) y [LifecycleResumeEffect](https://developer.android.com/topic/libraries/architecture/lifecycle). La simulación sigue siendo teórica, sin ESP32 ni infraestructura vial real.

## Evidencia
Las pruebas de lógica, compilación y Firebase se registran en docs/evidencias/alertas.json. La comprobación física del sonido/vibración y su recepción visual en Android requiere un teléfono o emulador operativo; no debe marcarse aprobada por el solo hecho de compilar.
