"""Seis lecturas y dos receptores SDK en vivo; no ejecuta Android."""
from datetime import datetime, timezone
import json
from pathlib import Path
import queue
import subprocess
import tempfile
import time
from conector import Conector
from sensores import Lectura

CASOS = [
    {"distanciaCm": 2, "movimientoDetectado": True, "nivelRiesgo": "riesgo"},
    {"distanciaCm": 49.75, "movimientoDetectado": True, "nivelRiesgo": "riesgo"},
    {"distanciaCm": 50, "movimientoDetectado": True, "nivelRiesgo": "advertencia"},
    {"distanciaCm": 99.5, "movimientoDetectado": False, "nivelRiesgo": "advertencia"},
    {"distanciaCm": 100, "movimientoDetectado": False, "nivelRiesgo": "informacion"},
    {"distanciaCm": 400, "movimientoDetectado": False, "nivelRiesgo": "informacion"},
]

def main():
    carpeta = Path(__file__).resolve().parent
    puente = Conector(carpeta.parent)
    receptor = None
    salida = carpeta / "resultado_flujo.json"
    evidencia = {"ejecutadoEn": datetime.now(timezone.utc).isoformat(),
                 "resultado": "en_curso", "androidVerificado": False}
    salida.write_text(json.dumps(evidencia, indent=2) + "\n", encoding="utf-8")
    try:
        with tempfile.TemporaryDirectory() as temporal:
            casos = Path(temporal) / "casos.json"
            listo = Path(temporal) / "listo.json"
            casos.write_text(json.dumps(CASOS), encoding="utf-8")
            receptor = subprocess.Popen(
                [puente.node, str(carpeta / "verificar_flujo.mjs"),
                 str(int(time.time() * 1000)), str(casos), str(listo)],
                cwd=str(carpeta.parent), stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                text=True, encoding="utf-8",
                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            limite = time.monotonic() + 60
            while not listo.exists():
                if receptor.poll() is not None:
                    raise RuntimeError("No se iniciaron los receptores Firebase.")
                if time.monotonic() > limite:
                    raise TimeoutError("No se confirmaron los receptores en 60 segundos.")
                time.sleep(0.1)
            puente.iniciar()
            limite = time.monotonic() + 120
            enviados = False
            guardados = 0
            while guardados < len(CASOS):
                if time.monotonic() > limite:
                    raise TimeoutError("No se confirmaron todas las lecturas locales.")
                try:
                    tipo, _ = puente.eventos.get(timeout=0.3)
                except queue.Empty:
                    continue
                if tipo == "listo" and not enviados:
                    for caso in CASOS:
                        puente.enviar(Lectura(caso["distanciaCm"], caso["movimientoDetectado"]))
                    enviados = True
                elif tipo == "guardado":
                    guardados += 1
                elif tipo == "fin":
                    raise RuntimeError("El simulador terminó antes de guardar los datos.")
            stdout, _stderr = receptor.communicate(timeout=120)
            if receptor.returncode:
                raise RuntimeError("Falló la verificación remota; Android no verificado.")
            evidencia.update(json.loads(stdout.strip().splitlines()[-1]))
            limite = time.monotonic() + 10
            while puente.cola() != 0 and time.monotonic() < limite:
                time.sleep(0.1)
            if puente.cola() != 0:
                raise RuntimeError("Firestore confirmó datos pero quedan lecturas en la cola.")
            evidencia.update(resultado="aprobado", colaFinal=0, pythonEnvioLecturas=guardados)
            salida.write_text(json.dumps(evidencia, indent=2) + "\n", encoding="utf-8")
            print("OK: seis lecturas confirmadas en eventos y lecturas; dos roles las recibieron en vivo; cola vacía.")
    except Exception as error:
        evidencia.update(resultado="fallido", error=type(error).__name__)
        salida.write_text(json.dumps(evidencia, indent=2) + "\n", encoding="utf-8")
        raise
    finally:
        if puente.proceso is not None:
            puente.cerrar()
        if receptor is not None:
            if receptor.poll() is None:
                receptor.terminate()
                receptor.wait(timeout=10)
            if receptor.stdout:
                receptor.stdout.close()
            if receptor.stderr:
                receptor.stderr.close()

if __name__ == "__main__":
    main()
