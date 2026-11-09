# ViaTech
Aplicación Android nativa para observar riesgos simulados alrededor de una plaza. Evolución del proyecto académico **Amigo Virtual**, Unidad 2 TI3042, INACAP.

El sensor PIR representa movimiento; HC-SR04 representa proximidad. No identifican personas, matrículas ni infracciones confirmadas. El LED es un estado de prueba, sin conexión a infraestructura vial.

## Estado y entrega
APK local y cloud compilados; cuatro pruebas Kotlin y pruebas Firebase locales y remotas aprobadas. Firestore Standard conectado en Santiago sin facturación. Ver [entrega y accesos locales](docs/entrega.md), [resultados](docs/pruebas/resultados.md) y [avance](PROGRESS.md). La ejecución en dos Android sigue pendiente.

## Abrir en Android Studio
Abrir la carpeta raíz del repositorio (ViaTech al clonar; evaluacion2 en el equipo de desarrollo). El módulo :app apunta a android/app. La carpeta app/ original se conserva sin usar.
SDK 37, JDK de Android Studio, Gradle Wrapper 9.6.0, AGP 9.5.0, Kotlin 2.4.10 y Compose.
Seleccionar **localDebug** para laboratorio; **cloudDebug** para Firebase real.
En PowerShell:
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleLocalDebug :app:testLocalDebugUnitTest
```
APK: android/app/build/outputs/apk/local/debug/app-local-debug.apk.

## Laboratorio Firebase reproducible
Requisitos: Node LTS, Java 21 o posterior y acceso inicial a Maven/npm.
```powershell
cd firebase
npm ci
npm run verify
npm run emulators
```
En una segunda terminal, desde firebase:
```powershell
npm run seed
npm run simulator
```
Firebase UI: http://127.0.0.1:4000. Auth: 9099. Firestore: 8080.
Credenciales **exclusivamente locales**: admin@viatech.test, operador@viatech.test y consulta@viatech.test; contraseña de laboratorio ViaTechLocal2026!.
El script seed rechaza hosts externos, no utiliza credenciales de producción y no sobrescribe documentos existentes.
El simulador usa una cuarta identidad esp32@viatech.test con claims de dispositivo. El APK nunca contiene esa contraseña.

## Funcionalidades
- Login/registro sin privilegios, recuperación genérica, sesión Firebase y cierre de sesión.
- Dashboard, historial (muestra reciente de 200 registros), filtros por fecha local, zona, dispositivo, tipo, sensor y riesgo.
- Sensores, dispositivos con heartbeat, zonas, comandos pendientes/ejecutados/error.
- Gestión administrativa de autorizaciones por UID, zonas y dispositivos; auditoría atómica.
- Preferencia de tarjetas compactas; simulación explícita de desconexión Firestore.
- Detección y cola persistente en el simulador; UUID y confirmación antes de retirar eventos.

## Arquitectura
Compose → ViewModel/StateFlow → repositorios → SDK Firebase. Las vistas no consultan Firestore.
Auth identifica usuarios; usuarios/{uid} autoriza roles. Una identidad de dispositivo con claims propias escribe eventos inmutables.
Administrador → comando+auditoría → simulador autenticado → lectura+evento → listeners Android A y B.
Ver docs/arquitectura/diseno.md y modelo-firestore.md.

## Firebase nube
Proyecto autorizado conectado: viatech-inacap-bleiva-2026. Email/Password y Firestore Standard en southamerica-west1 habilitados, reglas e índices desplegados, sin facturación comprobada por API. Ver docs/entrega.md para usarlo en otro equipo.
Registrar Android cl.amigovirtual.app. Copiar firebase.local.properties.example a firebase.local.properties y completar la configuración pública del registro Android. No usar claves privadas.
Desplegar reglas e índices:
```powershell
cd firebase
npx firebase deploy --only firestore:rules,firestore:indexes --project viatech-inacap-bleiva-2026
```
Cuentas demo provisionadas por IAM autorizado; contraseñas aleatorias en firebase/.env.cloud-demo.json, excluido de Git. Ver docs/seguridad/provisionamiento.md.
Cloud rechaza HTTP. Local usa HTTP de laboratorio y nunca debe emplearse con contraseñas reales.

## Dos Android
Emuladores: host 10.0.2.2. Dos teléfonos por USB: configurar emulator.host=127.0.0.1 y ejecutar adb -s SERIAL reverse tcp:8080 tcp:8080 y lo mismo para 9099.
Esto demuestra sincronización en dos Android pero **no prueba Wi-Fi físico**.
Demostración Wi-Fi real: misma variante cloud, mismo proyecto Firebase y red WPA2/WPA3 con Internet. No hace falta abrir puertos locales.
Ver docs/demostracion.md.

## Seguridad y límites
Reglas deniegan por defecto; no autoasignación admin; roles validados en servidor; eventos solo de su dispositivo; auditoría inmutable. No se almacenan manualmente contraseñas.
ISO/IEC 27400 orienta el diseño; no se afirma certificación.
Las preferencias no alteran privilegios. Historial limitado explícitamente; no hay paginación histórica completa. Wokwi/PlatformIO son diseño teórico complementario, no firmware probado.
Las dependencias npm se fijan mediante package-lock.json; revisar el informe de auditoría de dependencias antes de producción.

## Ventana Python para los sensores
Abre simulador_python/ABRIR_SIMULADOR.py para ajustar distancia ultrasónica y movimiento PIR. Funciona en modo visual y permite conectar el simulador existente a Firebase. Ver [instrucciones](simulador_python/INSTRUCCIONES.md) y [archivos excluidos de GitHub](simulador_python/ARCHIVOS_FUERA_DE_GITHUB.md).

## Alertas desde el controlador Python
Abre simulador_python/controlador.py, conecta Firebase y prepara una lectura de riesgo (35 cm con PIR). Al enviarla, ViaTech cloudDebug abierta muestra una alerta y puede sonar/vibrar. Las preferencias están en Configuración; el historial y las reconexiones no reproducen alertas acumuladas. Ver [guion y límites](docs/alertas-sensores.md).