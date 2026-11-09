package cl.amigovirtual.app

import cl.amigovirtual.app.alerts.RiskAlertGate
import cl.amigovirtual.app.data.models.*
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class RiskAlertGateTest {
    private val now = 1_000_000L
    private fun event(id: String = "new", detected: Long = now + 1,
                      cm: Double = 35.0, pir: Boolean = true,
                      risk: String = "riesgo", valid: Boolean = true,
                      origin: String = "simulado", pending: Boolean = false) =
        Record(id, mapOf("eventoId" to id, "detectadoEn" to Timestamp(Date(detected)),
            "fechaHora" to Timestamp(Date(now + 1)), "distanciaCm" to cm,
            "movimientoDetectado" to pir, "nivelRiesgo" to risk, "lecturaValida" to valid,
            "origenDatos" to origin, "sensorOrigen" to "PIR+HC-SR04"), pending)
    private fun feed(vararg events: Record) = Feed(events.toList(), cached = false)
    private fun gate() = RiskAlertGate().apply {
        reset(now)
        assertNull(accept(feed(), now, 0, true))
    }

    @Test fun firstServerSnapshotAndCacheAreSilent() {
        val gate = RiskAlertGate().apply { reset(now) }
        assertNull(gate.accept(feed(event()).copy(cached = true), now + 1, 1, true))
        assertNull(gate.accept(feed(event()), now + 1, 1, true))
        assertEquals("next", gate.accept(feed(event(), event("next")), now + 2, 2, true)?.id)
    }
    @Test fun newConfirmedRiskAlertsOnlyOnce() {
        val gate = gate()
        assertEquals("new", gate.accept(feed(event()), now + 2, 2, true)?.id)
        assertNull(gate.accept(feed(event()), now + 20_000, 20_000, true))
    }
    @Test fun safeWarningAndInvalidDataDoNotAlert() {
        for (record in listOf(event(cm = 150.0, pir = false, risk = "informacion"),
                event(cm = 50.0, risk = "advertencia"), event(pir = false),
                event(valid = false), event(cm = Double.NaN), event(origin = "real"),
                event().copy(values = event().values - "detectadoEn"),
                event().copy(values = event().values + ("eventoId" to "otro")))) {
            assertNull(gate().accept(feed(record), now + 2, 2, true))
        }
    }
    @Test fun pendingWriteWaitsForRemoteConfirmation() {
        val gate = gate()
        assertNull(gate.accept(feed(event(pending = true)), now + 2, 2, true))
        assertNotNull(gate.accept(feed(event()), now + 3, 3, true))
    }
    @Test fun oldQueuedEventsAndFutureClockAreIgnored() {
        assertNull(gate().accept(feed(event(detected = now - 1)), now + 2, 2, true))
        assertNull(gate().accept(feed(event()), now + 31_002, 31_002, true))
        assertNull(gate().accept(feed(event(detected = now + 10_000)), now + 2, 2, true))
    }
    @Test fun offlineAndErrorReconnectionUseSilentBaseline() {
        for (interruption in listOf(Feed(cached = true), Feed(cached = false, error = "sin acceso"))) {
            val gate = gate()
            assertNull(gate.accept(interruption, now + 2, 2, true))
            assertNull(gate.accept(feed(event()), now + 3, 3, true))
            assertNotNull(gate.accept(feed(event(), event("live")), now + 4, 4, true))
        }
    }
    @Test fun backgroundAndUnauthorizedCannotAlert() {
        val gate = gate()
        assertNull(gate.accept(feed(event()), now + 2, 2, false))
        assertNull(gate.accept(feed(event()), now + 3, 3, true))
    }
    @Test fun tenSecondCooldownDoesNotReplaySuppressedEvents() {
        val gate = gate()
        assertNotNull(gate.accept(feed(event("one")), now + 1, 100, true))
        assertNull(gate.accept(feed(event("two")), now + 2, 101, true))
        assertNull(gate.accept(feed(event("two")), now + 12_000, 12_000, true))
        assertNotNull(gate.accept(feed(event("three")), now + 12_001, 12_001, true))
    }
    @Test fun batchProducesOnlyNewestAlertAndResetPreventsReplay() {
        val gate = gate()
        val snapshot = feed(event("one"), event("two", detected = now + 2))
        assertEquals("two", gate.accept(snapshot, now + 3, 3, true)?.id)
        gate.reset(now + 4)
        assertNull(gate.accept(snapshot, now + 5, 5, true))
    }
}
