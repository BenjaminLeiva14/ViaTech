"""Puente opcional al simulador Firebase ya existente, sin claves en el código."""
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import threading

PROYECTO_AUTORIZADO = "viatech-inacap-bleiva-2026"

def configuracion(carpeta):
    raiz = Path(carpeta).resolve()
    script = raiz / "simulation" / "simulator.mjs"
    archivo_sdk = raiz / "android" / "app" / "google-services.json"
    archivo_privado = raiz / "firebase" / ".env.cloud-demo.json"
    if not script.is_file():
        raise ValueError("Selecciona la carpeta raíz de ViaTech, donde están android, firebase y simulation.")
    if not archivo_sdk.is_file() or not archivo_privado.is_file():
        raise ValueError("Falta google-services.json o firebase/.env.cloud-demo.json. Consulta INSTRUCCIONES.md; estos archivos privados no vienen de GitHub.")
    try:
        sdk = json.loads(archivo_sdk.read_text(encoding="utf-8-sig"))
        privado = json.loads(archivo_privado.read_text(encoding="utf-8-sig"))
        proyecto = sdk["project_info"]["project_id"]
        if proyecto != PROYECTO_AUTORIZADO or privado["project"] != proyecto:
            raise ValueError("La configuración no pertenece al proyecto ViaTech autorizado.")
        cliente = next(c for c in sdk["client"] if c["client_info"]["android_client_info"]["package_name"] == "cl.amigovirtual.app")
        dispositivo = next(a for a in privado["accounts"] if a["rol"] == "dispositivo")
        api_key = cliente["api_key"][0]["current_key"]
        correo, clave = dispositivo["email"], dispositivo["password"]
        if not all(isinstance(x, str) and x for x in (api_key, correo, clave)):
            raise ValueError("La configuración del dispositivo está incompleta.")
    except (KeyError, StopIteration, TypeError, json.JSONDecodeError):
        raise ValueError("Los archivos de configuración no tienen el formato esperado.") from None
    node = shutil.which("node")
    if not node:
        raise ValueError("No se encontró Node.js. El modo visual funciona sin él; Firebase necesita Node.js.")
    if not (raiz / "firebase" / "node_modules" / "firebase").is_dir():
        raise ValueError("Faltan dependencias de Firebase. Ejecuta npm ci dentro de la carpeta firebase.")
    entorno = os.environ.copy()
    entorno.update(VIATECH_PROJECT_ID=proyecto, VIATECH_API_KEY=api_key,
                   VIATECH_DEVICE_EMAIL=correo, VIATECH_DEVICE_PASSWORD=clave,
                   VIATECH_DEVICE_ID="esp32-demo")
    return raiz, node, script, entorno, (clave, api_key)

class Conector:
    def __init__(self, carpeta):
        self.raiz, self.node, self.script, self.entorno, self.secretos = configuracion(carpeta)
        self.eventos = queue.Queue(maxsize=200)
        self.ordenes = queue.Queue(maxsize=30)
        self.proceso = None
        self.cerrando = False
        self._lock = None

    def _avisar(self, tipo, mensaje=""):
        for secreto in self.secretos:
            mensaje = mensaje.replace(secreto, "[oculto]")
        dato = (tipo, mensaje[:400])
        try:
            self.eventos.put_nowait(dato)
        except queue.Full:
            # Priorizar el estado final frente a mensajes repetitivos, sin bloquear.
            if tipo != "registro":
                try:
                    self.eventos.get_nowait()
                    self.eventos.put_nowait(dato)
                except (queue.Empty, queue.Full):
                    pass

    def _bloquear(self):
        estado = self.raiz / "simulation" / ".state"
        estado.mkdir(parents=True, exist_ok=True)
        self._lock = (estado / "ventana-python.lock").open("a+b")
        self._lock.seek(0, 2)
        if self._lock.tell() == 0:
            self._lock.write(b"0")
            self._lock.flush()
        self._lock.seek(0)
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(self._lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(self._lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            self._lock.close()
            self._lock = None
            raise ValueError("Ya hay otra ventana Python conectada a este proyecto.") from None

    def _desbloquear(self):
        if self._lock is not None:
            self._lock.close()
            self._lock = None

    def iniciar(self):
        self._bloquear()
        try:
            self.proceso = subprocess.Popen(
                [self.node, str(self.script), "--cloud"],
                cwd=str(self.raiz), env=self.entorno,
                stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
                bufsize=1, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        except Exception:
            self._desbloquear()
            raise
        threading.Thread(target=self._leer, daemon=True).start()
        threading.Thread(target=self._escribir, daemon=True).start()

    def _leer(self):
        try:
            for linea in self.proceso.stdout:
                linea = linea.strip()
                if linea.startswith("Simulador teórico conectado."):
                    self._avisar("listo", "Dispositivo autenticado. Conector de Firebase activo.")
                elif linea == "Evento guardado localmente.":
                    self._avisar("guardado", "Lectura guardada en la cola persistente del simulador.")
                elif linea:
                    self._avisar("registro", linea)
        finally:
            self.proceso.wait()
            self._desbloquear()
            self._avisar("fin", f"Conector finalizado (código {self.proceso.returncode}).")
            if self.proceso.stdout:
                self.proceso.stdout.close()

    def _escribir(self):
        try:
            while self.proceso.poll() is None:
                try:
                    orden = self.ordenes.get(timeout=0.2)
                except queue.Empty:
                    continue
                self.proceso.stdin.write(orden)
                self.proceso.stdin.flush()
                if orden == "salir\n":
                    return
        except (BrokenPipeError, OSError, ValueError):
            self._avisar("registro", "Se interrumpió la comunicación con el conector.")

    def enviar(self, lectura):
        if self.cerrando or self.proceso is None or self.proceso.poll() is not None:
            raise ValueError("El conector no está disponible.")
        try:
            self.ordenes.put_nowait(lectura.comando())
        except queue.Full:
            raise ValueError("Hay demasiadas lecturas esperando. Espera a que se guarden.") from None

    def cola(self):
        archivo = self.raiz / "simulation" / ".state" / "cloud-outbox.json"
        try:
            return len(json.loads(archivo.read_text(encoding="utf-8"))["queue"])
        except (OSError, ValueError, KeyError, TypeError):
            return None

    def cerrar(self):
        """La GUI llama desde un hilo: nunca bloquea la ventana."""
        if self.cerrando:
            return
        self.cerrando = True
        try:
            self.ordenes.put("salir\n", timeout=1)
            self.proceso.wait(timeout=6)
        except (queue.Full, subprocess.TimeoutExpired):
            # Solo termina el proceso que inició esta ventana, nunca otros Node.
            if self.proceso.poll() is None:
                self.proceso.terminate()
                self.proceso.wait(timeout=5)
        finally:
            if self.proceso.stdin:
                self.proceso.stdin.close()
