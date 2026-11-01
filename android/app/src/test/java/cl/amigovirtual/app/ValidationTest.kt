package cl.amigovirtual.app
import cl.amigovirtual.app.data.models.Validation
import org.junit.Assert.*
import org.junit.Test
class ValidationTest {
    @Test fun rejectsInvalidMeasurement() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0, 401.0).forEach { assertFalse(Validation.distance(it)) }
        assertTrue(Validation.distance(2.0)); assertTrue(Validation.distance(400.0))
    }
    @Test fun heartbeatExpiresAndRejectsFuture() {
        assertTrue(Validation.connected(1000,61000)); assertFalse(Validation.connected(1000,61001))
        assertFalse(Validation.connected(2000,1000)); assertFalse(Validation.connected(null,1000))
    }
    @Test fun riskNeedsEvidence() {
        assertEquals("riesgo",Validation.risk(30.0,true))
        assertEquals("advertencia",Validation.risk(30.0,false))
        assertEquals("informacion",Validation.risk(null,true))
    }
    @Test fun validatesEmail() {
        assertTrue(Validation.email("test@example.com")); assertFalse(Validation.email("x@"))
        assertFalse(Validation.email("a b@example.com"))
    }
}
