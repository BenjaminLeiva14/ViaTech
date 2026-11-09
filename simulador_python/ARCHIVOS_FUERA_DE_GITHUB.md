# Qué archivos no aparecen en GitHub
**Git no los hace desaparecer ni los borra del computador al guardar o subir cambios.**
.gitignore indica qué archivos nuevos no se agregan al repositorio. Al clonar o descargar GitHub en otro equipo, esos archivos no vienen incluidos. Si un archivo ya estaba versionado, añadirlo a .gitignore no lo retira del historial.

## Configuración y datos privados
| Archivo o patrón | Motivo / cómo recuperarlo |
|---|---|
| firebase/.env.cloud-demo.json | Contraseñas aleatorias de demostración. Recuperar una copia privada; no regenerarlas automáticamente. |
| firebase.local.properties | Configuración local cloud/emulador. Completar a partir de firebase.local.properties.example. |
| android/app/google-services.json y cualquier google-services.json | Configuración Firebase, excluida por decisión del proyecto; descargar del mismo registro Android autorizado. No es una clave administrativa. |
| local.properties y **/local.properties | Ruta del SDK de cada computador; Android Studio la genera. |
| .env y .env.* | Configuración privada. La excepción .env.example puede versionarse. |
| *.jks, *.keystore, *.pem, *service-account*.json | Claves o credenciales privadas si existen. Conservar respaldo privado. |

## Archivos generados
| Archivo o carpeta | Qué contiene |
|---|---|
| **/build/ | APK, resultados de compilación y pruebas. Los APK están localmente en android/app/build/outputs/apk/. |
| .gradle/, .kotlin/ | Cachés y estado de compilación. |
| .idea/ | Configuración local de Android Studio. |
| node_modules/ | Dependencias descargadas; se recuperan con npm ci desde firebase. |
| firebase/.firebase/, firebase/emulator-data/ | Estado y datos locales de emuladores. |
| simulation/.state/ | Cola persistente del dispositivo, estado LED y bloqueo de la ventana Python. No borrar si hay eventos pendientes. |
| tmp/ | Archivos temporales. |
| *.log y firebase/*debug.log | Logs locales; pueden contener información de sesiones. |
| simulador_python/__pycache__/ y *.pyc dentro de esa carpeta | Bytecode generado por Python. |
| simulador_python/.state/ y *.log dentro de esa carpeta | Estado temporal o logs si se generan. |

## Plantilla inicial y documentos externos
- /app/ es la plantilla Android original conservada y excluida. **android/app/** es la aplicación ViaTech activa y sí se versiona.
- Los PDF, DOCX y mockups originales que están en Desktop o Downloads están fuera del repositorio; Git no los copia automáticamente.
- .git/ contiene el control de versiones local; no se publica como una carpeta del código.
- Los archivos .py, .bat y .md de este simulador sí deben aparecer en GitHub.

Estos son los patrones configurados; no significa que todos los archivos de la lista existan.
Para saber por qué un archivo específico queda fuera: git check-ignore -v RUTA_DEL_ARCHIVO.

- simulador_python/.env.credenciales-privadas.md: copia local solicitada con credenciales; nunca publicar. Excluida explícitamente y mediante .env.*.
