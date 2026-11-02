# Guion reproducible INACAP
## Preparación
Abrir Android Studio en la raíz, seleccionar localDebug y compilar. Ejecutar Firebase Emulator Suite, seed y simulador. Instalar APK en dos Android (dos AVD o dos teléfonos). Configurar host y puertos según README. Capturar seriales, versión APK y proyecto. La variante cloud ya apunta a viatech-inacap-bleiva-2026. Ver docs/entrega.md e iniciar node scripts/simulator-cloud.mjs desde firebase.
## Demostración
1. Android A: iniciar admin@viatech.test. Android B: iniciar operador@viatech.test. En nube usar cuentas autorizadas de firebase/.env.cloud-demo.json con contraseñas aleatorias; nunca la contraseña pública del laboratorio.
2. Mostrar badges de simulación, usuario/rol y dashboard.
3. Control IoT de A: PIR activo, distancia 35, Generar evento simulado.
4. Mostrar comando pendiente y luego ejecutado. Si el simulador está apagado, no afirmar ejecución.
5. En Firebase UI local o consola Firestore nube, comprobar documentos con el mismo ID en lecturas/eventos y el destinatario del comando.
6. En A y B, verificar idéntico eventoId, fecha y origenDatos=simulado. Tomar captura de ambos. La coincidencia de pantallas, sin comprobar ID, no basta.
7. A: encender LED de prueba y comprobar estado. B: demostrar ausencia de control; pruebas de reglas demuestran denegación incluso enviando solicitud directa.
8. Android B: configuración, simular desconexión. Mostrar aviso de caché/desactualización.
9. Consola simulador: offline; lectura 35 true. Mostrar outbox sin exponer credenciales. Detección local continúa.
10. Consola: online. Verificar que outbox se vacía solo tras confirmar y no duplica evento. B: reconectar y comprobar actualización.
11. Detener simulador; esperar >60 s y verificar dispositivo desconectado. Reiniciar y comprobar heartbeat.
12. Cerrar sesión; intentar acceso a historial y mostrar login.
## Evidencia
Registrar esperado, obtenido, estado y archivo de captura/log por paso. Si no hay dos Android o Wi-Fi físico, dejar pendiente; no sustituir por resultados inventados.
## Entrega académica
La guía pide un bloc de notas con URL GitHub en AAI. Publicar solo el repositorio revisado, sin secretos, y copiar el enlace. Fecha exacta y escala de apreciación no estaban adjuntas. Revisión docente: pendiente, no simulada.
