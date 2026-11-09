package cl.amigovirtual.app.viewmodel

import android.app.Application
import android.os.SystemClock
import cl.amigovirtual.app.alerts.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.amigovirtual.app.ViaTechApplication
import cl.amigovirtual.app.data.models.*
import cl.amigovirtual.app.data.repositories.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RiskNotice(val eventId: String, val device: String, val zone: String, val distance: Double, val receivedAt: Long)

data class AppState(
    val uid: String? = null, val profile: Profile? = null,
    val feeds: Map<String, Feed> = emptyMap(), val busy: Boolean = false,
    val message: String? = null, val configured: Boolean = true, val offline: Boolean = false,
    val now: Long = System.currentTimeMillis(), val compact: Boolean = false,
    val soundAlerts: Boolean = true, val vibrationAlerts: Boolean = true,
    val riskNotice: RiskNotice? = null
)
class ViaTechViewModel(app: Application) : AndroidViewModel(app) {
    private val provider = (app as ViaTechApplication).firebase
    private val auth = AuthRepository(provider)
    private val repository = MonitoringRepository(provider)
    private val preferences = app.getSharedPreferences("display", 0)
    private val alertGate = RiskAlertGate()
    private val feedback = AlertFeedback(app)
    private var foreground = false
    private val mutable = MutableStateFlow(AppState(configured = provider.configured,
        compact = preferences.getBoolean("compact", false),
        soundAlerts = preferences.getBoolean("soundAlerts", true),
        vibrationAlerts = preferences.getBoolean("vibrationAlerts", true)))
    val state = mutable.asStateFlow()
    private var profileJob: Job? = null
    private val feeds = mutableListOf<Job>()
    init {
        viewModelScope.launch { while (isActive) { delay(1000); mutable.update { it.copy(now = System.currentTimeMillis()) } } }
        viewModelScope.launch {
            auth.sessions.collect { uid ->
                profileJob?.cancel(); clearFeeds()
                mutable.update { AppState(uid = uid, configured = provider.configured, compact = it.compact,
                    soundAlerts = it.soundAlerts, vibrationAlerts = it.vibrationAlerts) }
                if (uid != null) profileJob = launch {
                    repository.profile(uid).distinctUntilChanged().collect { profile ->
                        clearFeeds()
                        mutable.update { it.copy(profile = profile, feeds = emptyMap(), riskNotice = null) }
                        if (profile.activo) {
                            val collections = mutableListOf("eventos", "zonas")
                            if (profile.telemetry) collections += listOf("lecturas", "dispositivos")
                            if (profile.admin) collections += listOf("usuarios", "comandos", "auditoria")
                            collections.forEach { collection -> feeds += launch {
                                repository.observe(collection).collect { feed ->
                                    mutable.update { it.copy(feeds = it.feeds + (collection to feed)) }
                                    if (collection == "eventos") processAlerts(feed)
                                }
                            } }
                        }
                    }
                }
            }
        }
    }
    private fun clearFeeds() {
        feeds.forEach(Job::cancel); feeds.clear()
        alertGate.reset(System.currentTimeMillis())
        feedback.stop()
    }
    private fun processAlerts(feed: Feed) {
        val current = state.value
        val event = alertGate.accept(feed, System.currentTimeMillis(), SystemClock.elapsedRealtime(),
            foreground && current.uid != null && current.profile?.activo == true && !current.offline) ?: return
        mutable.update { it.copy(riskNotice = RiskNotice(event.id, event.text("dispositivoId"),
            event.text("zonaId"), event.number("distanciaCm")!!, System.currentTimeMillis())) }
        feedback.play(current.soundAlerts, current.vibrationAlerts)
    }
    fun foreground(active: Boolean) {
        if (foreground == active) return
        foreground = active
        feedback.stop()
        alertGate.reset(System.currentTimeMillis())
        mutable.update { it.copy(riskNotice = null) }
        // Sembrar la línea base actual al volver a la pantalla; nunca reproducir historial.
        if (active) state.value.feeds["eventos"]?.let(::processAlerts)
    }
    fun soundAlerts(enabled: Boolean) {
        preferences.edit().putBoolean("soundAlerts", enabled).apply()
        feedback.stop()
        mutable.update { it.copy(soundAlerts = enabled) }
    }
    fun vibrationAlerts(enabled: Boolean) {
        preferences.edit().putBoolean("vibrationAlerts", enabled).apply()
        feedback.stop()
        mutable.update { it.copy(vibrationAlerts = enabled) }
    }
    fun dismissRisk() {
        feedback.stop()
        mutable.update { it.copy(riskNotice = null) }
    }
    override fun onCleared() {
        feedback.stop()
        super.onCleared()
    }
    fun message(value: String?) { mutable.update { it.copy(message = value) } }
    private fun action(block: suspend () -> String) {
        if (state.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, message = null) }
            try { message(withTimeout(20_000) { block() }) }
            catch (e: TimeoutCancellationException) { message("Sin confirmación del servidor. Revisa el historial antes de repetir la operación.") }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("No se pudo completar. Comprueba los datos, la conexión y tus permisos.") }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun login(email: String, password: String, register: Boolean = false) {
        if (!Validation.email(email.trim()) || password.length < (if (register) 8 else 1)) {
            message("Introduce un correo válido y una contraseña" + if (register) " de al menos 8 caracteres." else "."); return
        }
        action { if (register) auth.register(email,password) else auth.login(email,password)
            if (register) "Cuenta creada. Un administrador debe autorizar tu UID." else "Sesión iniciada." }
    }
    fun recover(email: String) {
        if (!Validation.email(email.trim())) { message("Introduce tu correo para recuperar el acceso."); return }
        action {
            try { auth.recover(email) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            "Si la cuenta permite recuperación, recibirás un correo. Revisa también spam."
        }
    }
    fun logout() {
        profileJob?.cancel(); clearFeeds()
        mutable.update { AppState(configured = provider.configured, compact = it.compact,
            soundAlerts = it.soundAlerts, vibrationAlerts = it.vibrationAlerts) }
        auth.logout()
        viewModelScope.launch { runCatching { repository.network(true) } }
    }
    fun command(device: String, type: String, distance: Double, motion: Boolean) = action {
        check(state.value.profile?.admin == true && !state.value.offline)
        repository.command(device,type,distance,motion); "Comando registrado; espera la confirmación del simulador."
    }
    fun saveUser(uid: String, role: String, active: Boolean) = action {
        check(state.value.profile?.admin == true && !state.value.offline)
        require(uid.isNotBlank() && uid != state.value.uid)
        repository.updateUser(uid.trim(),role,active); "Autorización actualizada."
    }
    fun saveZone(id: String, name: String, type: String, active: Boolean) = action {
        check(state.value.profile?.admin == true && !state.value.offline)
        repository.saveZone(id,name,type,active); "Zona guardada."
    }
    fun saveDevice(id: String, name: String, zone: String, active: Boolean) = action {
        check(state.value.profile?.admin == true && !state.value.offline)
        repository.saveDevice(id,name,zone,active); "Dispositivo guardado."
    }
    fun offline(enabled: Boolean) = action {
        feedback.stop()
        alertGate.reset(System.currentTimeMillis())
        repository.network(!enabled); mutable.update { it.copy(offline = enabled) }
        if (enabled) "Conexión Firestore pausada para la demostración." else "Reconexión solicitada."
    }
    fun compact(value: Boolean) { preferences.edit().putBoolean("compact", value).apply(); mutable.update { it.copy(compact = value) } }
}
