# ViaTech — avance
Actualizado: 2026-11-01, America/Santiago.

## Terminado y comprobado
- Lectura completa de guía Unidad 2, PDF de 17 páginas y referencias Stitch; matriz de requisitos y contradicciones.
- Android Kotlin/Compose/MVVM, Auth, listeners Firestore, roles, dashboard, eventos, sensores, dispositivos, zonas, control simulado, configuración y administración.
- APK local generado: android/app/build/outputs/apk/local/debug/app-local-debug.apk.
- Compilación assembleLocalDebug y cuatro pruebas JVM: BUILD SUCCESSFUL.
- Firebase Emulator: 34 pruebas de reglas, cuatro de dominio y una de Auth aprobadas. Integración aprobada en repetición individual después de corregir cierre del seed y timeout.
- Integración: identidad de dispositivo, comando ejecutado, evento visto por dos clientes SDK y reconexión Firestore. No equivale a dos Android.
- Simulador con cola persistente, UUID, heartbeat y LED lógico; firmware/Wokwi teóricos.
- Google Cloud: viatech-inacap-bleiva-2026 creado. Consulta de API confirma billingEnabled=false y permisos necesarios.
- Repositorio privado autorizado: https://github.com/BenjaminLeiva14/ViaTech.

## En curso
- Activación de Firebase nube: addFirebase devolvió 403. Usuario confirmó aceptación de términos Firebase. Se requiere sesión nueva tras revocar una sesión expuesta en un diagnóstico.
- Publicación GitHub del código y evidencias.

## Pendiente de evidencia
- Firebase nube: base Santiago, Auth, configuración Android, reglas, datos y prueba remota.
- Instalación, navegación, accesibilidad y sincronización en dos Android. ADB no detecta dispositivos; usuario no dispone de teléfono.
- AVD x86_64 bloqueado: Android Emulator hypervisor driver is not installed on this machine. Intento sin aceleración no produjo un destino ADB.
- Persistencia de cola después de reinicio del proceso durante corte real, prueba Wi-Fi y Wokwi/hardware.
- Capturas de pantallas ejecutadas; no se presentan las referencias Stitch como evidencia de la app.
- Revisión docente/AAI no proporcionada.

## Problemas resueltos y límites
- Firebase Auth usa metadata Kotlin 2.4; KGP y Compose alineados a 2.4.10.
- Arranque Firestore en este equipo supera 60 s: wrapper permite hasta 300 s.
- Primer conjunto Node: 39 aprobadas y una integración cancelada por timeout. Integración corregida y repetida: 1/1 aprobada. La suite completa no se volvió a ejecutar.
- npm producción: seis alertas moderadas, cero altas/críticas tras actualización gRPC. No se declara libre de vulnerabilidades.
- IoT/inalámbrico teóricos, sin infraestructura vial real ni certificación ISO.
