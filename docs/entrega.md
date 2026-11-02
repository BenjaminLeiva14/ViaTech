# Entrega y uso inmediato
Repositorio: https://github.com/BenjaminLeiva14/ViaTech.
Firebase: https://console.firebase.google.com/project/viatech-inacap-bleiva-2026/overview.
Base: Standard, southamerica-west1 (Santiago), sin cuenta de facturación.

## Android Studio y APK
Abrir la raíz de este proyecto, seleccionar cloudDebug y ejecutar en Android API 26 o posterior.
APK cloud: android/app/build/outputs/apk/cloud/debug/app-cloud-debug.apk.
APK laboratorio: android/app/build/outputs/apk/local/debug/app-local-debug.apk.
Ambos son debug; no están firmados para una publicación de producción. Se comprobó compilación, no instalación.

## Cuentas de demostración nube
Los accesos están en el archivo local privado firebase/.env.cloud-demo.json. Abrirlo localmente para usar el correo y contraseña del rol correspondiente; no copiar su contenido en chat, GitHub o capturas.
Tres roles humanos y una identidad de dispositivo, con contraseñas aleatorias diferentes. Los correos @viatech.invalid son ficticios y no reciben recuperación por email. Para probar recuperación real, usar posteriormente una cuenta propia autorizada; no se enviaron emails en estas pruebas.
El APK no contiene estas contraseñas. El archivo privado tiene acceso Windows para el usuario Dell, administradores del equipo y SYSTEM.

## Simulador nube
Desde firebase:
```powershell
node scripts/simulator-cloud.mjs
```
Mantenerlo activo durante la demostración. Escribir lectura 35 true para generar un evento, offline para pausar la red y online para recuperar. salir termina el simulador. El heartbeat vence después de 60 s sin comunicación.
En Android, un administrador también puede solicitar lecturas y encender/apagar el LED lógico desde Control IoT.

## Pruebas y datos
Ver docs/evidencias/firebase-cloud.json, cola-reinicio.json y firebase-entorno.json.
La prueba nube observó datos en usuarios, dispositivos, zonas, lecturas, eventos, auditoria y comandos.
Pruebas explícitas contra nube desde firebase:
```powershell
node --test scripts/cloud-smoke.test.mjs
node --test scripts/cloud-queue.test.mjs
```
Estas pruebas generan pocos datos simulados. No se ejecutan automáticamente con npm test.
No ejecutar dos simuladores con la misma cola. No borrar la cola para resolver errores.

## Otro equipo
1. Clonar el repositorio privado usando la cuenta GitHub autorizada.
2. npm ci desde firebase; iniciar sesión Firebase mediante flujo oficial.
3. Descargar configuración pública del Android registrado:
```powershell
npx -y firebase-tools@latest apps:sdkconfig ANDROID 1:1031041183390:android:76eac31125a23bb75a958f --project viatech-inacap-bleiva-2026 --out ../android/app/google-services.json
```
4. Copiar firebase.local.properties.example a firebase.local.properties y rellenar projectId, appId y apiKey desde ese archivo público.
5. Transferir las credenciales demo por un medio privado o provisionar nuevas cuentas autorizadas. No ejecutar bootstrap para regenerar contraseñas de cuentas existentes.
6. Abrir raíz en Android Studio y compilar cloudDebug. GitHub no contiene dependencias, builds, configuración privada ni credenciales.

## Aceptación pendiente
Falta ejecutar la app en dos Android y obtener capturas, revisar navegación/accesibilidad y demostrar Wi-Fi físico si el docente lo requiere. Las pruebas SDK y compilaciones no sustituyen esa evidencia.
