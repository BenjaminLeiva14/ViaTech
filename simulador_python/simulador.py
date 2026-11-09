"""Ventana sencilla de los sensores ultrasónico e infrarrojo de ViaTech."""
from datetime import datetime
from pathlib import Path
import queue
import threading
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from sensores import Lectura
from conector import Conector

class Ventana:
    def __init__(self, root):
        self.root = root
        self.root.title("ViaTech · Simulador de sensores")
        self.root.geometry("720x690")
        self.root.minsize(640, 640)
        self.root.configure(bg="#f7f9fb")
        self.carpeta = Path(__file__).resolve().parent.parent
        self.conector = None
        self.listo = False
        self.cerrando = False
        self.por_guardar = 0
        self.distancia = tk.DoubleVar(value=150)
        self.entrada = tk.StringVar(value="150")
        self.movimiento = tk.BooleanVar(value=False)
        self.automatico = tk.BooleanVar(value=False)
        self.estado = tk.StringVar(value="Solo pantalla: las lecturas todavía no se envían a Firebase.")
        self.cola_texto = tk.StringVar(value="Cola Firebase: sin conexión")
        self.riesgo = tk.StringVar()
        self.ultimo = tk.StringVar(value="Todavía no has generado una lectura.")
        estilo = ttk.Style()
        estilo.theme_use("clam")
        estilo.configure("TFrame", background="#f7f9fb")
        estilo.configure("TLabel", background="#f7f9fb", font=("Segoe UI", 10))
        estilo.configure("Title.TLabel", font=("Segoe UI", 23, "bold"), foreground="#0f172a")
        estilo.configure("TLabelframe", background="#f7f9fb")
        estilo.configure("TLabelframe.Label", font=("Segoe UI", 11, "bold"))
        estilo.configure("TButton", font=("Segoe UI", 10), padding=8)
        canvas = tk.Canvas(root, bg="#f7f9fb", highlightthickness=0)
        scroll = ttk.Scrollbar(root, orient="vertical", command=canvas.yview)
        canvas.configure(yscrollcommand=scroll.set)
        scroll.pack(side="right", fill="y")
        canvas.pack(side="left", fill="both", expand=True)
        panel = ttk.Frame(canvas, padding=22)
        panel_id = canvas.create_window((0, 0), window=panel, anchor="nw")
        panel.bind("<Configure>", lambda _e: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.bind("<Configure>", lambda e: canvas.itemconfigure(panel_id, width=e.width))
        ttk.Label(panel, text="ViaTech", style="Title.TLabel").pack(anchor="w")
        ttk.Label(panel, text="Dos sensores simulados · No requiere Arduino ni ESP32 físico").pack(anchor="w", pady=(2, 18))
        ultra = ttk.LabelFrame(panel, text="1. Ultrasónico HC-SR04 — distancia", padding=14)
        ultra.pack(fill="x")
        fila = ttk.Frame(ultra)
        fila.pack(fill="x")
        ttk.Label(fila, text="Distancia:").pack(side="left")
        ttk.Spinbox(fila, from_=2, to=400, increment=1, textvariable=self.entrada,
                    width=8, command=self._desde_entrada).pack(side="left", padx=10)
        ttk.Label(fila, text="cm   (entre 2 y 400)").pack(side="left")
        ttk.Scale(ultra, from_=2, to=400, variable=self.distancia,
                  command=self._desde_barra).pack(fill="x", pady=(12, 0))
        ir = ttk.LabelFrame(panel, text="2. Infrarrojo PIR — presencia / movimiento", padding=14)
        ir.pack(fill="x", pady=12)
        ttk.Checkbutton(ir, text="Movimiento detectado", variable=self.movimiento,
                        command=self._vista).pack(anchor="w")
        self.indicador = tk.Label(panel, textvariable=self.riesgo, font=("Segoe UI", 12, "bold"),
                                 anchor="w", padx=12, pady=9)
        self.indicador.pack(fill="x")
        acciones = ttk.Frame(panel)
        acciones.pack(fill="x", pady=12)
        self.enviar_btn = ttk.Button(acciones, text="Simular lectura", command=self.generar)
        self.enviar_btn.pack(side="left")
        ttk.Checkbutton(acciones, text="Repetir cada 5 segundos", variable=self.automatico).pack(side="left", padx=15)
        ttk.Label(panel, textvariable=self.ultimo, wraplength=650).pack(anchor="w")
        conexion = ttk.Frame(panel)
        conexion.pack(fill="x", pady=(16, 6))
        self.conectar_btn = ttk.Button(conexion, text="Conectar Firebase", command=self.conectar)
        self.conectar_btn.pack(side="left")
        self.desconectar_btn = ttk.Button(conexion, text="Desconectar", command=self.desconectar, state="disabled")
        self.desconectar_btn.pack(side="left", padx=8)
        self.carpeta_btn = ttk.Button(conexion, text="Seleccionar proyecto…", command=self.elegir_carpeta)
        self.carpeta_btn.pack(side="left")
        ttk.Label(panel, textvariable=self.estado, wraplength=650).pack(anchor="w", pady=4)
        ttk.Label(panel, textvariable=self.cola_texto).pack(anchor="w")
        self.registro = tk.Text(panel, height=5, font=("Consolas", 9), wrap="word",
                                state="disabled", background="white", foreground="#334155")
        self.registro.pack(fill="both", expand=True, pady=(10, 0))
        self.entrada.trace_add("write", self._entrada_cambiada)
        self._vista()
        self.root.protocol("WM_DELETE_WINDOW", self.cerrar)
        self.root.after(150, self._actualizar)
        self.root.after(5000, self._automatico)

    def _anotar(self, texto):
        self.registro.configure(state="normal")
        self.registro.insert("end", f"{datetime.now():%H:%M:%S}  {texto}\n")
        if int(self.registro.index("end-1c").split(".")[0]) > 100:
            self.registro.delete("1.0", "2.0")
        self.registro.see("end")
        self.registro.configure(state="disabled")

    def _desde_barra(self, _valor):
        self.entrada.set(str(round(self.distancia.get())))

    def _desde_entrada(self):
        try:
            self.distancia.set(Lectura(self.entrada.get(), self.movimiento.get()).distancia_cm)
        except ValueError:
            pass
        self._vista()

    def _entrada_cambiada(self, *_args):
        self._desde_entrada()

    def _vista(self):
        try:
            lectura = Lectura(self.entrada.get(), self.movimiento.get())
            colores = {"RIESGO": ("#fee2e2", "#b91c1c"), "ADVERTENCIA": ("#fef3c7", "#92400e"),
                       "INFORMACIÓN": ("#dbeafe", "#1d4ed8")}
            fondo, tinta = colores[lectura.riesgo]
            self.riesgo.set(f"{lectura.riesgo} · PIR: {'sí' if lectura.movimiento else 'no'}")
        except ValueError:
            fondo, tinta = "#fee2e2", "#b91c1c"
            self.riesgo.set("Introduce una distancia válida: 2 a 400 cm")
        self.indicador.configure(bg=fondo, fg=tinta)

    def generar(self):
        try:
            lectura = Lectura(self.entrada.get(), self.movimiento.get())
            resumen = f"{lectura.distancia_cm:g} cm · Movimiento: {'sí' if lectura.movimiento else 'no'} · {lectura.riesgo}"
            if self.conector is not None:
                if not self.listo:
                    raise ValueError("Espera a que el conector termine de iniciar.")
                self.conector.enviar(lectura)
                self.por_guardar += 1
                self.ultimo.set("Entregada al conector; esperando almacenamiento local.")
                self._anotar("Solicitada: " + resumen)
            else:
                self.ultimo.set("Solo pantalla: " + resumen)
                self._anotar("Lectura visual: " + resumen)
        except ValueError as error:
            self.automatico.set(False)
            messagebox.showwarning("Revisa la lectura", str(error), parent=self.root)

    def conectar(self):
        self.automatico.set(False)
        try:
            nuevo = Conector(self.carpeta)
            nuevo.iniciar()
            self.conector = nuevo
            self.listo = False
            self.conectar_btn.configure(state="disabled")
            self.carpeta_btn.configure(state="disabled")
            self.desconectar_btn.configure(state="normal")
            self.enviar_btn.configure(state="disabled")
            self.estado.set("Iniciando el conector…")
            self._anotar("Iniciando el dispositivo esp32-demo con su identidad privada.")
        except (ValueError, OSError) as error:
            messagebox.showerror("No se pudo conectar", str(error), parent=self.root)

    def elegir_carpeta(self):
        elegida = filedialog.askdirectory(title="Selecciona la raíz de ViaTech")
        if elegida:
            self.carpeta = Path(elegida)
            self._anotar("Carpeta del proyecto seleccionada.")

    def desconectar(self):
        if self.conector is None or self.conector.cerrando:
            return
        actual = self.conector
        if self.por_guardar and not messagebox.askyesno(
                "Lecturas pendientes",
                "Hay lecturas que aún no confirmaron almacenamiento local. Si el conector no responde, podrían perderse. ¿Quieres desconectar?",
                parent=self.root):
            return
        if self.conector is not actual:
            return
        self.automatico.set(False)
        self.listo = False
        self.enviar_btn.configure(state="disabled")
        self.desconectar_btn.configure(state="disabled")
        self.estado.set("Cerrando el conector… La cola ya guardada se conserva.")
        threading.Thread(target=self.conector.cerrar, daemon=True).start()

    def _actualizar(self):
        if self.conector is not None:
            actual = self.conector
            while True:
                try:
                    tipo, texto = actual.eventos.get_nowait()
                except queue.Empty:
                    break
                self._anotar(texto)
                if tipo == "listo" and not actual.cerrando:
                    self.listo = True
                    self.estado.set(texto)
                    self.enviar_btn.configure(text="Enviar a Firebase", state="normal")
                elif tipo == "guardado":
                    self.por_guardar = max(0, self.por_guardar - 1)
                    self.ultimo.set("Guardada localmente. Comprueba el evento confirmado en ViaTech.")
                elif tipo == "fin":
                    self.conector = None
                    self.listo = False
                    self.por_guardar = 0
                    self.automatico.set(False)
                    self.estado.set("Solo pantalla: conector cerrado.")
                    self.cola_texto.set("Cola guardada en simulation/.state/cloud-outbox.json")
                    self.conectar_btn.configure(state="normal")
                    self.carpeta_btn.configure(state="normal")
                    self.desconectar_btn.configure(state="disabled")
                    self.enviar_btn.configure(text="Simular lectura", state="normal")
                    if self.cerrando:
                        self.root.destroy()
                        return
            if self.conector is not None:
                pendientes = actual.cola()
                self.cola_texto.set(f"En la cola persistente: {pendientes if pendientes is not None else 'sin información'} · Esperando almacenamiento: {self.por_guardar}")
        self.root.after(150, self._actualizar)

    def _automatico(self):
        if self.automatico.get() and not self.cerrando:
            self.generar()
        self.root.after(5000, self._automatico)

    def cerrar(self):
        if self.conector is None:
            self.root.destroy()
            return
        self.desconectar()
        if self.conector is None:
            self.root.destroy()
        elif self.conector.cerrando:
            self.cerrando = True

def main():
    root = tk.Tk()
    Ventana(root)
    root.mainloop()

if __name__ == "__main__":
    main()
