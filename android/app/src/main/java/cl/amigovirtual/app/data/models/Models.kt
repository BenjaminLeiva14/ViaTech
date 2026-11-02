package cl.amigovirtual.app.data.models

data class Profile(val uid: String = "", val rol: String = "", val activo: Boolean = false) {
    val admin get() = activo && rol == "administrador"
    val telemetry get() = activo && rol in listOf("administrador", "operador")
}
data class Record(val id: String, val values: Map<String, Any?>, val pending: Boolean = false) {
    fun text(key: String) = values[key]?.toString().orEmpty()
    fun flag(key: String) = values[key] == true
    fun number(key: String) = (values[key] as? Number)?.toDouble()
    fun time(key: String) = (values[key] as? com.google.firebase.Timestamp)?.toDate()?.time
}
data class Feed(val records: List<Record> = emptyList(), val cached: Boolean = true, val error: String? = null)
object Validation {
    fun email(value: String) = value.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)
    fun distance(value: Double) = value.isFinite() && value in 2.0..400.0
    fun risk(cm: Double?, motion: Boolean) = when {
        cm == null || !distance(cm) -> "informacion"
        cm < 50 && motion -> "riesgo"
        cm < 100 -> "advertencia"
        else -> "informacion"
    }
    fun connected(last: Long?, now: Long) = last != null && now - last in 0..60_000
}
