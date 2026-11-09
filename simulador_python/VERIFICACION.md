# Verificación del simulador Python
Fecha: 2026-11-09 (America/Santiago).

- Pruebas locales iniciales: 8/8 aprobadas (sensores, protocolo, configuración, cola de entrada y ocultación de secretos).
- Prueba Tkinter: ventana creada, controles de ambos sensores actualizados, lecturas visuales y validación verificadas; cierre correcto.
- Integración: el puente Python inició la identidad de dispositivo, envió 37.125 cm con movimiento y un cliente operador confirmó el evento desde el servidor Firestore.
- Evento confirmado: a2484796-f1fe-49fe-bc64-4bf210f8d33b. Evidencia sin credenciales: resultado_firebase.json.
- Prueba final local: 9/9 aprobadas, incluida prioridad del estado de cierre. La prueba Tkinter se repitió y también pasó.

No se probó hardware físico ni una nueva instalación Android. El modo visual no escribe en Firebase. La cola y autenticación del modo conectado pertenecen al simulador Node existente; Python no contiene claves administrativas.

Repetir desde esta carpeta:

    python -m unittest -v test_simulador.py
    python prueba_ventana.py
    python prueba_firebase.py

La última prueba requiere Internet y crea un evento simulado real en el Firebase autorizado.
