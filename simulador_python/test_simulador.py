"""Pruebas locales sin cuentas, red ni escrituras Firebase."""
import json
import math
from pathlib import Path
import queue
import tempfile
import unittest
import runpy
from unittest.mock import patch
from sensores import Lectura
from conector import Conector, configuracion, PROYECTO_AUTORIZADO

class PruebasSensores(unittest.TestCase):
    def test_lanzador_python_abre_la_ventana(self):
        with patch("simulador.main") as main:
            for archivo in ("ABRIR_SIMULADOR.py", "controlador.py"):
                main.reset_mock()
                runpy.run_path(str(Path(__file__).with_name(archivo)), run_name="__main__")
                main.assert_called_once_with()

    def test_limites_de_distancia(self):
        for valor in (2, 400, "35.5"):
            self.assertEqual(Lectura(valor, False).distancia_cm, float(valor))
        for valor in (1, 401, math.nan, math.inf, -math.inf, True, "35\nsalir", ""):
            with self.subTest(valor=valor), self.assertRaises(ValueError):
                Lectura(valor, False)

    def test_pir_no_admite_texto_como_booleano(self):
        for valor in ("false", 0, 1, None):
            with self.assertRaises(ValueError):
                Lectura(35, valor)

    def test_riesgo_y_fronteras(self):
        for cm, pir, esperado in ((35, True, "RIESGO"), (35, False, "ADVERTENCIA"),
                                  (50, True, "ADVERTENCIA"), (99, False, "ADVERTENCIA"),
                                  (100, True, "INFORMACIÓN"), (400, False, "INFORMACIÓN")):
            with self.subTest(cm=cm, pir=pir):
                self.assertEqual(Lectura(cm, pir).riesgo, esperado)

    def test_protocolo_compatible_con_simulador(self):
        self.assertEqual(Lectura(35, True).comando(), "lectura 35 true\n")
        self.assertEqual(Lectura(80.5, False).comando(), "lectura 80.5 false\n")

class PruebasConector(unittest.TestCase):
    def test_no_expone_secretos_en_registro(self):
        puente = Conector.__new__(Conector)
        puente.secretos = ("clave-ficticia", "api-ficticia")
        puente.eventos = queue.Queue(maxsize=200)
        puente._avisar("registro", "Fallo clave-ficticia api-ficticia")
        self.assertEqual(puente.eventos.get(), ("registro", "Fallo [oculto] [oculto]"))

    def test_configuracion_de_otro_proyecto_se_rechaza(self):
        with tempfile.TemporaryDirectory() as carpeta:
            raiz = Path(carpeta)
            (raiz / "simulation").mkdir()
            (raiz / "simulation/simulator.mjs").write_text("", encoding="utf-8")
            (raiz / "android/app").mkdir(parents=True)
            (raiz / "firebase").mkdir()
            (raiz / "android/app/google-services.json").write_text(json.dumps(
                {"project_info": {"project_id": "otro-proyecto"}}), encoding="utf-8")
            (raiz / "firebase/.env.cloud-demo.json").write_text(json.dumps(
                {"project": PROYECTO_AUTORIZADO}), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "autorizado"):
                configuracion(raiz)

    def test_estado_final_tiene_prioridad_sobre_registro(self):
        puente = Conector.__new__(Conector)
        puente.secretos = ()
        puente.eventos = queue.Queue(maxsize=1)
        puente._avisar("registro", "mensaje anterior")
        puente._avisar("fin", "terminado")
        self.assertEqual(puente.eventos.get(), ("fin", "terminado"))

    def test_carpeta_incorrecta(self):
        with tempfile.TemporaryDirectory() as carpeta:
            with self.assertRaisesRegex(ValueError, "raíz"):
                configuracion(carpeta)

    def test_limite_de_entradas_no_bloquea_ventana(self):
        class Proceso:
            def poll(self):
                return None
        puente = Conector.__new__(Conector)
        puente.cerrando = False
        puente.proceso = Proceso()
        puente.ordenes = queue.Queue(maxsize=1)
        puente.enviar(Lectura(35, True))
        with self.assertRaisesRegex(ValueError, "demasiadas"):
            puente.enviar(Lectura(35, True))

if __name__ == "__main__":
    unittest.main(verbosity=2)
