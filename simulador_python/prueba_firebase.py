"""Prueba explícita: envía UNA lectura simulada a la nube autorizada."""
import json
from pathlib import Path
import queue
import subprocess
import time
from conector import Conector
from sensores import Lectura

raiz = Path(__file__).resolve().parent.parent
puente = Conector(raiz)
iniciado = int(time.time() * 1000)
try:
    puente.iniciar()
    limite = time.monotonic() + 60
    enviado = False
    guardado = False
    while time.monotonic() < limite:
        try:
            tipo, _mensaje = puente.eventos.get(timeout=0.3)
        except queue.Empty:
            continue
        if tipo == "listo" and not enviado:
            puente.enviar(Lectura(37.125, True))
            enviado = True
        elif tipo == "guardado":
            guardado = True
            break
        elif tipo == "fin":
            raise RuntimeError("El conector termino antes de guardar la lectura.")
    if not guardado:
        raise TimeoutError("No se confirmo el almacenamiento local en 60 segundos.")
    resultado = subprocess.run(
        [puente.node, str(Path(__file__).with_name("verificar_evento.mjs")), str(iniciado), "37.125"],
        cwd=str(raiz), capture_output=True, text=True, encoding="utf-8", timeout=60,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    if resultado.returncode:
        raise RuntimeError("La consulta remota no confirmo el evento; revisa conexion y permisos.")
    evidencia = json.loads(resultado.stdout.strip().splitlines()[-1])
    evidencia["pythonEnvioLectura"] = True
    archivo = Path(__file__).with_name("resultado_firebase.json")
    archivo.write_text(json.dumps(evidencia, indent=2) + "\n", encoding="utf-8")
    print("OK: Python envio la lectura y Firestore confirmo el evento " + evidencia["eventoId"])
finally:
    if puente.proceso is not None:
        puente.cerrar()
