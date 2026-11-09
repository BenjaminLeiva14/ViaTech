package cl.amigovirtual.app.alerts

import cl.amigovirtual.app.data.models.Feed
import cl.amigovirtual.app.data.models.Record
import cl.amigovirtual.app.data.models.Validation

/**
 * Alertas de primer plano: el primer snapshot remoto es una línea base silenciosa.
 * Tras caché/error se exige otra línea base para no alertar por la cola reconectada.
 * No confundir llegada a Firestore con detección reciente en el dispositivo.
 */
class RiskAlertGate {
    private var primed = false
    private var activatedAt = Long.MAX_VALUE
    private var lastAlertElapsed: Long? = null
    private val seen = linkedSetOf<String>()

    fun reset(now: Long) {
        primed = false
        activatedAt = now
        lastAlertElapsed = null
        seen.clear()
    }

    fun accept(feed: Feed, now: Long, elapsed: Long, active: Boolean): Record? {
        if (!active || feed.cached || feed.error != null) {
            primed = false
            return null
        }
        val confirmed = feed.records.filterNot { it.pending }
        if (!primed) {
            confirmed.forEach { seen.add(it.id) }
            primed = true
            trim()
            return null
        }
        val fresh = confirmed.filter { seen.add(it.id) }
        trim()
        val event = fresh.filter { r ->
            val detected = r.time("detectadoEn")
            val committed = r.time("fechaHora")
            r.id.isNotBlank() && r.text("eventoId") == r.id &&
                r.text("origenDatos") == "simulado" && r.flag("lecturaValida") &&
                r.text("sensorOrigen") == "PIR+HC-SR04" &&
                r.text("nivelRiesgo") == "riesgo" &&
                Validation.risk(r.number("distanciaCm"), r.flag("movimientoDetectado")) == "riesgo" &&
                detected != null && committed != null && detected >= activatedAt &&
                now - detected in -5_000..30_000 && now - committed in -5_000..30_000
        }.maxByOrNull { it.time("detectadoEn") ?: 0L } ?: return null
        val previous = lastAlertElapsed
        // Una alerta por lote y no más de una cada 10 segundos.
        // Los eventos omitidos siguen en el historial; nunca se reproducen más tarde.
        if (previous != null && elapsed - previous < 10_000) return null
        lastAlertElapsed = elapsed
        return event
    }

    private fun trim() {
        while (seen.size > 1000) seen.remove(seen.first())
    }
}
