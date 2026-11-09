# ViaTech — avance
Actualizado: 2026-11-01, America/Santiago.

## Terminado y comprobado
- Lectura completa de guía Unidad 2, PDF de 17 páginas y referencias Stitch; matriz de requisitos y contradicciones.
- Android Kotlin/Compose/MVVM, Auth, Firestore, roles, dashboard, eventos, sensores, dispositivos, zonas, control simulado, configuración y administración.
- APK local y cloud compilados; cuatro pruebas JVM aprobadas.
- Firebase Emulator: 34 pruebas de reglas, cuatro de dominio y una de Auth aprobadas. Integración aprobada en repetición individual después de corregir cierre del seed y timeout.
- Firebase nube: viatech-inacap-bleiva-2026, Firestore Standard en Santiago, protección contra borrado y billingEnabled=false comprobados por API.
- App Android registrada; Email/Password habilitado; reglas e índices desplegados.
- Siete colecciones con datos. Cuentas humanas de administrador, operador y consulta; identidad independiente de dispositivo.
- Prueba nube aprobada: login válido/inválido, roles, comando ejecutado, mismo evento remoto en dos clientes SDK, reconexión y logout.
- Prueba de cola aprobada: red pausada por SDK, terminación del proceso, recuperación desde disco y ausencia de duplicado.
- Publicación en repositorio privado https://github.com/BenjaminLeiva14/ViaTech mediante conector autorizado.

## Pendiente para aceptación académica completa
- Instalación, navegación, accesibilidad y sincronización en dos Android. ADB no detecta dispositivos; usuario no dispone de teléfono.
- AVD x86_64 bloqueado: Android Emulator hypervisor driver is not installed on this machine. Intento sin aceleración no produjo un destino ADB.
- Capturas de pantallas ejecutadas. No se presentan referencias Stitch como capturas de la app.
- Prueba Wi-Fi física; Wokwi/PlatformIO y sensores permanecen teóricos, según alcance.
- Revisión docente y entrega del enlace en AAI corresponden al estudiante.
- Git CLI no tiene credenciales GitHub; la publicación se verificó con el conector. El respaldo Git local usa entrega-local. Para trabajar con main, autenticar GitHub y clonar el repositorio en una carpeta nueva o recuperar origin/main sin fusionar historiales independientes.

## Evidencia y límites
- Compilaciones y pruebas: docs/pruebas/resultados.md; resúmenes portables en docs/evidencias.
- Primer conjunto Node: 39 aprobadas y una integración cancelada por timeout. Integración corregida: 1/1 aprobada en repetición individual. No se afirma una segunda suite completa.
- Dos clientes SDK no equivalen a dos teléfonos; disableNetwork no equivale a cortar una red Wi-Fi física.
- npm producción: seis alertas moderadas, cero altas/críticas; evaluar antes de distribuir.
- Simulación sin infraestructura vial real ni certificación ISO.

## Actualización de pruebas Compose
- LoginUiTest migrado a androidx.compose.ui.test.junit4.v2.createAndroidComposeRule para eliminar el aviso de API obsoleta.
- :app:compileLocalDebugAndroidTestKotlin: BUILD SUCCESSFUL (2 min 41 s; 29 tareas). Evidencia local: docs/pruebas/compose-v2-compile.log.
- Las pruebas instrumentadas compilan; su ejecución en Android continúa pendiente.

## Simulador Python — 2026-11-09
- Creada carpeta independiente simulador_python con ventana Tkinter para HC-SR04 y PIR, lanzador Windows e instrucciones.
- Modo visual sin red y modo Firebase opcional mediante el conector Node existente; no se incluyeron credenciales.
- Prueba gráfica y envío Python a Firestore ejecutados y aprobados; evidencia y límites en simulador_python/VERIFICACION.md.
- Lista de archivos excluidos de GitHub documentada. Los archivos ignorados no se eliminan del computador.
