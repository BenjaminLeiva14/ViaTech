# Verificación del simulador Python
Fecha: 2026-11-09 (America/Santiago).

## Apertura y datos locales
- ABRIR_SIMULADOR.bat sustituido por ABRIR_SIMULADOR.py.
- 10/10 pruebas locales aprobadas: límites y tipos de distancia/PIR, niveles de riesgo, protocolo, rechazo de otro proyecto, cola limitada, ocultación de secretos, prioridad del estado final y delegación del lanzador a la ventana.
- Prueba Tkinter aprobada: ventana real creada, dos lecturas visuales, controles actualizados, entrada inválida rechazada y cierre correcto.
- Las pruebas locales no escriben en Firebase.

## Envío y recepción Firebase
prueba_flujo.py ejecutada y aprobada. Evidencia reproducible: resultado_flujo.json.

| Distancia (cm) | PIR | Riesgo esperado y obtenido |
|---|---|---|
| 2 | Sí | riesgo |
| 49.75 | Sí | riesgo |
| 50 | Sí | advertencia |
| 99.5 | No | advertencia |
| 100 | No | informacion |
| 400 | No | informacion |

- Python inició el conector Node con la identidad de dispositivo y envió seis lecturas.
- Los seis eventos se confirmaron desde el servidor; eventos y lecturas contienen documentos idénticos para cada ID.
- Dos clientes SDK autenticados como administrador y operador recibieron los seis IDs en tiempo real en ambas colecciones.
- Se verificaron valores, origen simulado, validez, sensor, timestamps y riesgo.
- Se usó la consulta de MonitoringRepository.kt: orderBy(fechaHora, desc).limit(200).
- Se verificó que firebase.local.properties de la variante cloud coincide con el proyecto, API key y app ID de google-services.json, sin imprimir esos valores.
- Cola persistente al terminar: 0. No se borraron los eventos de demostración.
- La prueba anterior de una lectura (37.125 cm) permanece registrada en resultado_firebase.json.

## Android y límites
- adb devices -l: ningún teléfono/emulador conectado.
- emulator -accel-check: Android Emulator hypervisor driver is not installed on this machine.
- Recepción visual en la app Android: **pendiente, no ejecutada**. Dos clientes SDK no equivalen a dos teléfonos ni comprueban el renderizado Kotlin.
- Para comprobarla: ejecutar cloudDebug en Android, iniciar sesión como operador, abrir Eventos/Sensores, conectar la ventana Python y enviar una lectura. localDebug se conecta a Firebase Emulator Suite, no a la nube.
- No se probó hardware físico. Todo dato generado está marcado como simulado.

## Credenciales
Se creó .env.credenciales-privadas.md solo en este equipo por solicitud del propietario. Contiene las cuentas existentes, sin regenerarlas. Se verificó su correspondencia con el archivo de origen y la exclusión mediante git check-ignore. No se publica ni se imprime su contenido.

## Repetir
Desde simulador_python:

    python -m unittest -v test_simulador.py
    python prueba_ventana.py
    python prueba_flujo.py

La última prueba requiere Internet y crea seis eventos simulados en Firebase. Detener otros simuladores del mismo dispositivo antes de ejecutarla.
