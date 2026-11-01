package cl.amigovirtual.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.amigovirtual.app.ViaTechApplication
import cl.amigovirtual.app.data.models.*
import cl.amigovirtual.app.data.repositories.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AppState(
    val uid: String? = null, val profile: Profile? = null,
    val feeds: Map<String, Feed> = emptyMap(), val busy: Boolean = false,
    val message: String? = null, val configured: Boolean = true, val offline: Boolean = false,
    val now: Long = System.currentTimeMillis(), val compact: Boolean = false
)
class ViaTechViewModel(app: Application) : AndroidViewModel(app) {
    private val provider = (app as ViaTechApplication).firebase
    private val auth = AuthRepository(provider)
    private val repository = MonitoringRepository(provider)
    private val preferences = app.getSharedPreferences("display", 0)
    private val mutable = MutableStateFlow(AppState(configured = provider.configured, compact = preferences.getBoolean("compact", false)))
    val state = mutable.asStateFlow()
    private var profileJob: Job? = null
    private val feeds = mutableListOf<Job>()
    init {
        viewModelScope.launch { while (isActive) { delay(1000); mutable.update { it.copy(now = System.currentTimeMillis()) } } }
        viewModelScope.launch {
            auth.sessions.collect { uid ->
                profileJob?.cancel(); clearFeeds()
                mutable.update { AppState(uid = uid, configured = provider.configured, compact = it.compact) }
                if (uid != null) profileJob = launch {
                    repository.profile(uid).distinctUntilChanged().collect { profile ->
                        clearFeeds()
                        mutable.update { it.copy(profile = profile, feeds = emptyMap()) }
                        if (profile.activo) {
                            val collections = mutableListOf("eventos", "zonas")
                            if (profile.telemetry) collections += listOf("lecturas", "dispositivos")
                            if (profile.admin) collections += listOf("usuarios", "comandos", "auditoria")
                            collections.forEach { collection -> feeds += launch {
                                repository.observe(collection).collect { feed ->
                                    mutable.update { it.copy(feeds = it.feeds + (collection to feed)) }
                                }
                            } }
                        }
                    }
                }
            }
        }
    }
    private fun clearFeeds() { feeds.forEach(Job::cancel); feeds.clear() }
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
        mutable.update { AppState(configured = provider.configured, compact = it.compact) }
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
        repository.network(!enabled); mutable.update { it.copy(offline = enabled) }
        if (enabled) "Conexión Firestore pausada para la demostración." else "Reconexión solicitada."
    }
    fun compact(value: Boolean) { preferences.edit().putBoolean("compact", value).apply(); mutable.update { it.copy(compact = value) } }
}
