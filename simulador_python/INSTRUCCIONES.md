# Simulador de sensores ViaTech en Python
Carpeta independiente del módulo Android: simulador_python/.
La ventana representa **dos sensores**, el ultrasónico HC-SR04 y el infrarrojo PIR de movimiento. No necesita Arduino, ESP32 físico ni conectar un sensor al computador.

## Abrir
En Windows, haz doble clic en **ABRIR_SIMULADOR.bat**.
También puedes abrir una terminal en esta carpeta y ejecutar:
```powershell
python simulador.py
```

Necesitas Python 3.10 o superior con Tkinter. No hay que ejecutar pip install.
Para comprobar Tkinter: python -m tkinter. Debe aparecer una ventana de prueba.
Si falta Python/Tkinter, instala Python desde https://www.python.org/downloads/windows/ incluyendo Tcl/Tk e IDLE. La instalación puede requerir autorización según el equipo.

## Usar sin Firebase
1. Mueve la barra del ultrasónico o escribe una distancia de 2 a 400 cm.
2. Marca o desmarca **Movimiento detectado** para el infrarrojo PIR.
3. Pulsa **Simular lectura**. La ventana muestra los valores y el nivel de riesgo.
4. Opcional: marca **Repetir cada 5 segundos**. Desmárcalo para detenerlo.

Al abrir comienza en **Solo pantalla**: estos valores son visuales, no se guardan en Firestore ni se acumulan para enviarlos después. La ventana no permite autoasignarse roles.

Reglas de la maqueta:
- Menos de 50 cm y movimiento: RIESGO.
- Si no se cumple lo anterior y la distancia es menor de 100 cm: ADVERTENCIA.
- Desde 100 cm: INFORMACIÓN.
Son criterios del prototipo; no constituyen evaluación vial real ni mediciones experimentales.

## Enviar las lecturas a la app Android
1. Conserva esta carpeta dentro de la raíz de ViaTech. También puedes moverla y usar **Seleccionar proyecto…** para elegir la raíz.
2. Detén cualquier otro simulador Node que estuviera ejecutándose. Solo debe existir un proceso para esp32-demo y su archivo de cola.
3. Pulsa **Conectar Firebase** y espera el mensaje de dispositivo autenticado.
4. Ajusta distancia/movimiento y pulsa **Enviar a Firebase**.
5. Abre ViaTech cloudDebug con una cuenta autorizada y revisa **Eventos** y **Sensores**.

Python usa el conector existente simulation/simulator.mjs en segundo plano. Por eso, el modo Firebase requiere Node.js, firebase/node_modules y estos dos archivos locales:
- android/app/google-services.json: configuración pública del Android registrado.
- firebase/.env.cloud-demo.json: contraseña privada de la identidad de dispositivo.

El proyecto autorizado es viatech-inacap-bleiva-2026 y el dispositivo esp32-demo.
Si faltan dependencias, ejecuta npm ci dentro de firebase. No copies contraseñas en el código, README o chat.
Si faltan los archivos de configuración al descargar GitHub, consulta ../docs/entrega.md. No vuelvas a crear el proyecto ni las cuentas para solucionar un archivo ausente.

**Guardada localmente** significa que el conector confirmó el archivo persistente; no es por sí sola una confirmación remota.
La ventana muestra cuántos eventos quedan en la cola. Comprueba el evento final en la app o la consola Firestore.
La cola existente se guarda en simulation/.state/cloud-outbox.json y el conector la retira únicamente tras confirmar la escritura.
Si pierdes Internet mientras está conectado, el conector conserva las lecturas que alcanzaron esa cola. Las lecturas generadas en modo Solo pantalla no se encolan.

## Cerrar
Desactiva la repetición automática y espera a que **Esperando almacenamiento** llegue a 0.
Pulsa **Desconectar** o cierra la ventana. El conector intenta terminar normalmente; si no responde, se termina únicamente el proceso iniciado por esta ventana. La cola ya guardada se conserva. Las solicitudes aún no guardadas podrían perderse, y la ventana pide confirmación si las hay.
Al detenerse el heartbeat, Android mostrará el dispositivo desconectado después de aproximadamente 60 segundos.

## Archivos de esta carpeta
- simulador.py: ventana.
- sensores.py: validaciones y comportamiento de ambos sensores.
- conector.py: comunicación opcional con el simulador Firebase existente.
- ABRIR_SIMULADOR.bat: apertura con doble clic en Windows.
- test_simulador.py: pruebas locales.
- ARCHIVOS_FUERA_DE_GITHUB.md: explicación de archivos excluidos.
- VERIFICACION.md: resultados realmente comprobados.

## Permisos
- Usar la ventana y simular no requiere permisos de administrador, cámara, Bluetooth, USB ni ubicación.
- Conectar Firebase utiliza Internet y la identidad de dispositivo ya autorizada. No necesita una clave administrativa ni iniciar sesión con tu cuenta personal.
- Instalar Python/Node o descargar dependencias puede pedir permisos del sistema o acceso a Internet.
- Esta implementación no abre puertos de escucha ni controla infraestructura real.

## Pruebas
Desde esta carpeta: python -m unittest -v test_simulador.py.
Las pruebas locales no acceden a Firebase. Para los resultados de la ventana y de la conexión, consulta VERIFICACION.md.

La prueba gráfica opcional es python prueba_ventana.py. La prueba de integración explícita es python prueba_firebase.py; esta última crea una lectura simulada en Firebase y usa verificar_evento.mjs para comprobarla desde el servidor.
