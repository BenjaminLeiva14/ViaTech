# Simulador teórico ESP32
No es hardware conectado ni firmware validado en Wokwi. Implementa detección local PIR/HC-SR04, cola JSON persistente (500 eventos máximo), UUID, heartbeat de 15 s y ejecución de comandos de prueba.
Desde firebase: npm run seed; npm run simulator.
En consola: lectura 35 true, offline, lectura 80 false, online, salir.
Se conserva la cola hasta confirmar escritura; si el proceso reinicia utiliza el mismo ID. Un evento ya confirmado no se duplica. Retención máxima aceptada por reglas: 7 días. Cola llena produce error explícito, sin borrar datos.
Comandos pendientes expiran a los 120 s; el simulador los marca error cuando vuelve. Un LED es un booleano simulado, no infraestructura real.
Hay un único escritor por archivo outbox. No ejecutar dos simuladores con el mismo dispositivo/cola.
Cloud: node simulation/simulator.mjs --cloud, variables VIATECH_PROJECT_ID, VIATECH_API_KEY, VIATECH_DEVICE_ID, VIATECH_DEVICE_EMAIL y VIATECH_DEVICE_PASSWORD. Proporcionarlas fuera del repositorio; nunca usar una clave administrativa. La cuenta debe tener claims de dispositivo provisionadas por un administrador confiable.
La red local del Emulator no representa TLS real; Firebase nube usa el SDK y validación TLS normal. No se desactiva la verificación de certificados.

Los reintentos de ciclos fallidos se limitan a cinco con espera creciente; online permite reanudación explícita. El SDK puede mantener una petición pendiente durante una interrupción; no se borra la cola por timeout. No hay garantía de tiempo real.
El diagrama Wokwi es únicamente simulado: para hardware físico se debe adaptar el nivel de ECHO de HC-SR04 a 3,3 V antes del GPIO ESP32 y revisar alimentación/pinout. No conectar directamente la maqueta virtual a hardware.
