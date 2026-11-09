"""Prueba de los controles Tkinter; abre la ventana brevemente, sin Firebase."""
import tkinter as tk
from simulador import Ventana

root = tk.Tk()
try:
    ventana = Ventana(root)
    root.update()
    ventana.entrada.set("35")
    ventana.movimiento.set(True)
    ventana._vista()
    root.update()
    assert ventana.riesgo.get() == "RIESGO · PIR: sí"
    ventana.generar()
    assert "Solo pantalla: 35 cm" in ventana.ultimo.get()
    ventana.entrada.set("150")
    ventana.movimiento.set(False)
    ventana._vista()
    ventana.generar()
    root.update()
    assert ventana.riesgo.get() == "INFORMACIÓN · PIR: no"
    assert "150 cm" in ventana.registro.get("1.0", "end")
    ventana.entrada.set("999")
    root.update()
    assert "distancia válida" in ventana.riesgo.get()
    ventana.preparar_demo(True)
    assert ventana.entrada.get() == "35" and ventana.movimiento.get()
    assert ventana.riesgo.get() == "RIESGO · PIR: sí"
    ventana.preparar_demo(False)
    assert ventana.entrada.get() == "150" and not ventana.movimiento.get()
    assert ventana.riesgo.get() == "INFORMACIÓN · PIR: no"
    assert ventana.conector is None
    assert ventana.enviar_btn.cget("text") == "Simular lectura"
    print("OK: ventana iniciada, controles actualizados, dos lecturas visuales, validacion y cierre; sin Firebase.")
finally:
    root.destroy()
