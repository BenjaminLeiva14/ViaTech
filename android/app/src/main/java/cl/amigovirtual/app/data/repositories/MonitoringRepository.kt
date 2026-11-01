package cl.amigovirtual.app.data.repositories

import cl.amigovirtual.app.data.firebase.FirebaseProvider
import cl.amigovirtual.app.data.models.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MonitoringRepository(private val firebase: FirebaseProvider) {
    private val db get() = requireNotNull(firebase.db)
    fun profile(uid: String) = callbackFlow {
        val listener = db.collection("usuarios").document(uid).addSnapshotListener { doc, error ->
            if (error != null || doc == null || !doc.exists()) trySend(Profile(uid))
            else trySend(Profile(uid, doc.getString("rol").orEmpty(), doc.getBoolean("activo") == true))
        }
        awaitClose { listener.remove() }
    }
    fun observe(collection: String) = callbackFlow {
        val query = when (collection) {
            "eventos", "lecturas", "auditoria" -> db.collection(collection).orderBy("fechaHora", Query.Direction.DESCENDING).limit(200)
            "comandos" -> db.collection(collection).orderBy("creacion", Query.Direction.DESCENDING).limit(100)
            else -> db.collection(collection).limit(100)
        }
        val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) trySend(Feed(error = "No se pudo consultar. Revisa conexión y permisos."))
            else if (snapshot != null) trySend(Feed(snapshot.documents.map {
                Record(it.id, it.data.orEmpty(), it.metadata.hasPendingWrites())
            }, snapshot.metadata.isFromCache))
        }
        awaitClose { listener.remove() }
    }
    suspend fun command(device: String, type: String, distance: Double, motion: Boolean) {
        require(Validation.distance(distance))
        val uid = requireNotNull(firebase.auth?.currentUser).uid
        // Probe the server before privileged mutations. There is no unbounded UI retry loop.
        db.collection("usuarios").document(uid).get(Source.SERVER).await()
        val id = UUID.randomUUID().toString()
        val batch = db.batch()
        batch.set(db.collection("comandos").document(id), mapOf(
            "comandoId" to id, "dispositivoId" to device, "tipo" to type,
            "estado" to "pendiente", "solicitante" to uid,
            "creacion" to FieldValue.serverTimestamp(), "ejecucion" to null,
            "distanciaCm" to distance, "movimientoDetectado" to motion, "resultado" to "",
            "origenDatos" to "simulado"
        ))
        batch.set(db.collection("auditoria").document(id), audit(uid, "comando", id))
        batch.commit().await()
    }
    suspend fun updateUser(uid: String, role: String, active: Boolean) {
        require(role in listOf("administrador", "operador", "consulta"))
        mutate("usuarios", uid, mapOf("uid" to uid, "rol" to role, "activo" to active), "usuario")
    }
    suspend fun saveZone(id: String, name: String, type: String, active: Boolean) {
        require(id.matches(Regex("[a-z0-9_-]{1,64}")))
        mutate("zonas", id, mapOf("zonaId" to id, "nombre" to name.trim(), "tipo" to type,
            "activo" to active, "sensores" to listOf("PIR", "HC-SR04")), "zona")
    }
    suspend fun saveDevice(id: String, name: String, zone: String, active: Boolean) {
        require(id.matches(Regex("[a-z0-9_-]{1,64}")))
        val old = db.collection("dispositivos").document(id).get(Source.SERVER).await()
        mutate("dispositivos", id, mapOf("dispositivoId" to id, "nombre" to name.trim(), "zonaId" to zone,
            "activo" to active, "versionFirmware" to (old.getString("versionFirmware") ?: "sim-1.0"),
            "sensores" to listOf("PIR", "HC-SR04"), "ultimaComunicacion" to old.getTimestamp("ultimaComunicacion"),
            "led" to (old.getBoolean("led") ?: false), "origenDatos" to "simulado"), "dispositivo")
    }
    private fun audit(uid: String, action: String, target: String) = mapOf(
        "actor" to uid, "accion" to action, "destino" to target, "fechaHora" to FieldValue.serverTimestamp()
    )
    private suspend fun mutate(collection: String, id: String, values: Map<String, Any?>, action: String) {
        val uid = requireNotNull(firebase.auth?.currentUser).uid
        db.collection("usuarios").document(uid).get(Source.SERVER).await()
        val auditId = UUID.randomUUID().toString()
        val batch = db.batch()
        batch.set(db.collection(collection).document(id), values + ("auditoriaId" to auditId))
        batch.set(db.collection("auditoria").document(auditId), audit(uid, action, id))
        batch.commit().await()
    }
    suspend fun network(enabled: Boolean) { if (enabled) db.enableNetwork().await() else db.disableNetwork().await() }
}
