"""Modelo de los dos sensores simulados. Solo biblioteca estándar."""
from dataclasses import dataclass
import math

@dataclass(frozen=True)
class Lectura:
    distancia_cm: float
    movimiento: bool

    def __post_init__(self):
        if isinstance(self.distancia_cm, bool):
            raise ValueError("Introduce una distancia entre 2 y 400 cm.")
        try:
            cm = float(self.distancia_cm)
        except (ValueError, TypeError):
            raise ValueError("Introduce una distancia entre 2 y 400 cm.") from None
        if not math.isfinite(cm) or not 2 <= cm <= 400:
            raise ValueError("La distancia debe estar entre 2 y 400 cm.")
        if not isinstance(self.movimiento, bool):
            raise ValueError("El movimiento debe ser sí o no.")
        object.__setattr__(self, "distancia_cm", cm)

    @property
    def riesgo(self):
        if self.distancia_cm < 50 and self.movimiento:
            return "RIESGO"
        if self.distancia_cm < 100:
            return "ADVERTENCIA"
        return "INFORMACIÓN"

    def comando(self):
        # Se construye desde valores validados, nunca desde texto libre del usuario.
        return f"lectura {self.distancia_cm:g} {'true' if self.movimiento else 'false'}\n"
