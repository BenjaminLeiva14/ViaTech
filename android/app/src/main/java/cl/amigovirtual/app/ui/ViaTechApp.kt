package cl.amigovirtual.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import cl.amigovirtual.app.BuildConfig
import cl.amigovirtual.app.data.models.*
import cl.amigovirtual.app.viewmodel.*
import java.time.*
import java.time.format.DateTimeFormatter

private val Green = Color(0xFF15803D)
private val Slate = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)
private val Ground = Color(0xFFF7F9FB)
private fun date(ms: Long?) = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")) } ?: "Sin confirmar"
private fun riskColor(risk: String) = when(risk) { "riesgo" -> Color(0xFFB91C1C); "advertencia" -> Color(0xFF92400E); else -> Color(0xFF1D4ED8) }

@Composable
fun ViaTechApp(vm: ViaTechViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Slate, background = Ground, surface = Color.White, onSurface = Slate),
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp))) {
        Surface(Modifier.fillMaxSize(), color = Ground) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.message?.let { msg ->
                    Surface(color = Color(0xFFE8F0FE)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(msg, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { vm.message(null) }) { Text("Cerrar") }
                        }
                    }
                }
                if (!state.configured) {
                    Column(Modifier.padding(24.dp)) {
                        Brand("Configuración pendiente")
                        Text("Falta configurar el proyecto Firebase. Completa firebase.local.properties o utiliza la variante localDebug con los emuladores.")
                    }
                } else if (state.uid == null) AuthScreen(state, vm)
                else if (state.profile?.activo != true) {
                    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Brand("Autorización de acceso")
                        Text("Tu sesión está iniciada. Necesitas una cuenta autorizada para consultar la plaza.")
                        SelectionContainerText("UID: " + state.uid.orEmpty())
                        Text("Comparte este UID con el administrador. Nunca compartas tu contraseña.", color = Muted)
                        OutlinedButton(onClick = vm::logout) { Text("Cerrar sesión") }
                    }
                } else key(state.uid, state.profile?.rol) { MainNavigation(state, vm) }
            }
        }
    }
}

@Composable private fun SelectionContainerText(text: String) {
    androidx.compose.foundation.text.selection.SelectionContainer { Text(text) }
}
@Composable private fun Brand(subtitle: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Slate)) {
            drawLine(Color.White, Offset(size.width*.28f,size.height*.30f),Offset(size.width*.5f,size.height*.74f),5f,StrokeCap.Round)
            drawLine(Color.White, Offset(size.width*.5f,size.height*.74f),Offset(size.width*.72f,size.height*.30f),5f,StrokeCap.Round)
            for (i in 0..3) drawLine(Color(0xFF22C55E),Offset(size.width*.5f,size.height*(.18f+i*.13f)),Offset(size.width*.5f,size.height*(.24f+i*.13f)),3f)
        }
        Text("ViaTech", fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Slate)
        Badge("MOVILIDAD VIAL", Green)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable private fun Badge(text: String, color: Color = Green) {
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelMedium)
    }
}
@Composable private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title.isNotBlank()) Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
@Composable private fun Field(value: String, label: String, change: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}
@Composable private fun Password(value: String, change: (String) -> Unit, label: String = "Contraseña") {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ocultar" else "Ver") } })
}
@Composable private fun AuthScreen(state: AppState, vm: ViaTechViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are transient and deliberately excluded from saved state/preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Brand("Monitoreo de riesgos alrededor de la plaza")
        Text(if(register) "Crear cuenta" else "Inicio de sesión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (BuildConfig.USE_EMULATORS) Badge("LABORATORIO LOCAL · DATOS SIMULADOS", Color(0xFF92400E))
        Field(email, "Correo electrónico", { email = it }, KeyboardType.Email)
        Password(password, { password = it })
        if (register) {
            Password(confirmation, { confirmation = it }, "Confirmar contraseña")
            Text("La cuenta se crea sin permisos. Un administrador autorizará tu acceso.", color = Muted)
        } else TextButton(onClick = { vm.recover(email) }, enabled = !state.busy) { Text("¿Olvidaste tu contraseña?") }
        Button(onClick = {
            if (register && password != confirmation) vm.message("Las contraseñas no coinciden.")
            else { vm.login(email,password,register); password = ""; confirmation = "" }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
            Text(if(register) "Crear cuenta" else "Iniciar sesión")
        }
        TextButton(onClick = { register = !register; password = ""; confirmation = "" }, enabled = !state.busy) {
            Text(if(register) "Ya tengo cuenta · Iniciar sesión" else "Solicitar acceso · Crear cuenta")
        }
        Text("Amigo Virtual · Prototipo académico INACAP", style = MaterialTheme.typography.bodySmall, color = Muted)
        Text("La app usa Internet para autenticar y sincronizar. No necesita ubicación, cámara ni Bluetooth.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable private fun MainNavigation(state: AppState, vm: ViaTechViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "Inicio"
    val profile = requireNotNull(state.profile)
    val routes = listOf("Inicio","Eventos","Zonas") + (if(profile.telemetry) listOf("Sensores","Dispositivos") else emptyList()) +
        (if(profile.admin) listOf("Control IoT","Usuarios","Auditoría") else emptyList()) + "Configuración"
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ViaTech", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Badge(profile.rol.uppercase())
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            routes.forEach { page ->
                FilterChip(selected = route == page, onClick = { nav.navigate(page) { popUpTo("Inicio"); launchSingleTop = true } }, label = { Text(page) })
            }
        }
        NavHost(nav, startDestination = "Inicio", modifier = Modifier.weight(1f)) {
            routes.forEach { page -> composable(page) { Page(page,state,vm) } }
        }
    }
}
@Composable private fun Page(page: String, state: AppState, vm: ViaTechViewModel) {
    val relevant = when(page) { "Eventos","Inicio" -> "eventos"; "Control IoT" -> "comandos"; "Sensores" -> "lecturas"; "Auditoría" -> "auditoria"; else -> page.lowercase() }
    val feed = state.feeds[relevant]
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(page, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Badge(if(BuildConfig.USE_EMULATORS) "SIMULACIÓN · FIREBASE LOCAL" else "FIREBASE NUBE · SENSORES SIMULADOS", Color(0xFF92400E))
            if (state.offline || feed?.cached == true) Text("Datos en caché / conexión sin confirmar. Pueden estar desactualizados.", color = Color(0xFF92400E))
            feed?.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        when(page) {
            "Inicio" -> {
                item { Dashboard(state) }
                item { Text("Eventos recientes", fontWeight = FontWeight.Bold) }
                items(state.feeds["eventos"]?.records.orEmpty().take(5), key = { it.id }) { EventCard(it,state.compact) }
            }
            "Eventos" -> item { Events(state.feeds["eventos"]?.records.orEmpty(),state.compact) }
            "Sensores" -> {
                items(feed?.records.orEmpty().take(40), key = { it.id }) { r -> Panel(r.text("dispositivoId")) {
                    Badge("SIMULADO", Muted)
                    Text("PIR · " + if(r.flag("movimientoDetectado")) "Movimiento detectado" else "Sin movimiento")
                    Text("HC-SR04 · " + (r.number("distanciaCm")?.let { "$it cm" } ?: "Sin lectura"))
                    Text("Validez: " + if(r.flag("lecturaValida")) "Válida" else "Inválida")
                    Text(date(r.time("fechaHora")), color = Muted)
                } }
            }
            "Dispositivos" -> {
                if(state.profile?.admin == true) item { DeviceEditor(state,vm) }
                items(feed?.records.orEmpty(), key = { it.id }) { r -> Panel(r.text("nombre")) {
                    Badge(if(Validation.connected(r.time("ultimaComunicacion"),state.now) && r.flag("activo")) "Conectado · heartbeat reciente" else "Desconectado / desactualizado", Muted)
                    Text("ID ESP32: " + r.id)
                    Text("Zona: " + r.text("zonaId"))
                    Text("Última comunicación: " + date(r.time("ultimaComunicacion")))
                    Text("Firmware: " + r.text("versionFirmware") + " · PIR + HC-SR04")
                    Text("LED de prueba: " + if(r.flag("led")) "Encendido" else "Apagado")
                    Text("Origen: " + r.text("origenDatos"), color = Muted)
                } }
            }
            "Zonas" -> {
                if(state.profile?.admin == true) item { ZoneEditor(vm) }
                items(feed?.records.orEmpty(), key = { it.id }) { r -> Panel(r.text("nombre")) {
                    Text(r.id + " · " + r.text("tipo"))
                    Badge(if(r.flag("activo")) "Activa" else "Inactiva", Muted)
                    Text("Sensores asociados: PIR · HC-SR04")
                    Text("Eventos en la muestra: " + state.feeds["eventos"]?.records.orEmpty().count { it.text("zonaId") == r.id })
                } }
            }
            "Control IoT" -> {
                item { Control(state,vm) }
                items(feed?.records.orEmpty(), key = { it.id }) { r -> Panel(r.text("tipo")) {
                    Badge(r.text("estado").uppercase(), if(r.text("estado") == "error") Color(0xFFB91C1C) else Green)
                    Text("Destinatario: " + r.text("dispositivoId"))
                    Text("Creación: " + date(r.time("creacion")) + "\nEjecución: " + date(r.time("ejecucion")))
                    Text(r.text("resultado"))
                    SelectionContainerText(r.id)
                } }
            }
            "Usuarios" -> {
                item { UserEditor(state,vm) }
                items(feed?.records.orEmpty(), key = { it.id }) { r -> Panel(r.text("rol")) {
                    SelectionContainerText("UID: " + r.id)
                    Badge(if(r.flag("activo")) "Autorizado" else "Desactivado", Muted)
                } }
            }
            "Auditoría" -> items(feed?.records.orEmpty(), key = { it.id }) { r -> Panel(r.text("accion")) {
                Text("Actor: " + r.text("actor"))
                Text("Destino: " + r.text("destino"))
                Text(date(r.time("fechaHora")), color = Muted)
            } }
            "Configuración" -> item { Settings(state,vm) }
        }
        if (feed != null && feed.records.isEmpty() && feed.error == null) item {
            Panel("Sin registros") { Text("Aún no hay datos disponibles. Inicia el simulador o ajusta los filtros.") }
        }
        item { Text("Prototipo académico · No genera multas ni controla infraestructura vial.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}
@Composable private fun Dashboard(state: AppState) {
    val events = state.feeds["eventos"]?.records.orEmpty()
    val devices = state.feeds["dispositivos"]?.records.orEmpty()
    val online = devices.count { it.flag("activo") && Validation.connected(it.time("ultimaComunicacion"),state.now) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel("Plaza · Amigo Virtual") {
            Text("Monitoreo de presencia y proximidad. Todos los indicios se interpretan como situaciones simuladas.")
            Text("Último evento confirmado: " + date(events.mapNotNull { it.time("fechaHora") }.maxOrNull()), color = Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(events.size.toString(), "Eventos recientes", Modifier.weight(1f))
            Metric(events.count { it.text("nivelRiesgo") == "riesgo" }.toString(),"Riesgo",Modifier.weight(1f),Color(0xFFB91C1C))
        }
        if(state.profile?.telemetry == true) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(online.toString(),"Conectados",Modifier.weight(1f))
            Metric((devices.size-online).toString(),"Desconectados",Modifier.weight(1f),Muted)
        }
        Panel("Distribución de riesgos · muestra reciente") {
            listOf("informacion","advertencia","riesgo").forEach { risk ->
                val count = events.count { it.text("nivelRiesgo") == risk }
                Text("$risk · $count")
                LinearProgressIndicator(progress = { if(events.isEmpty()) 0f else count.toFloat()/events.size }, modifier = Modifier.fillMaxWidth(), color = riskColor(risk))
            }
            Text("Resumen de hasta 200 eventos; no representa el total histórico.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
@Composable private fun Metric(value: String, label: String, modifier: Modifier, color: Color = Green) {
    Surface(modifier, color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp,Color(0xFFE2E8F0))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Muted); Text(value,fontSize = 30.sp,fontWeight = FontWeight.Bold,color = color)
        }
    }
}
@Composable private fun EventCard(r: Record, compact: Boolean) {
    Panel(r.text("tipoEvento").replace('_',' ')) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(r.text("nivelRiesgo").uppercase(),riskColor(r.text("nivelRiesgo")))
            Badge(r.text("origenDatos").uppercase(),Muted)
        }
        Text(r.text("descripcion"))
        Text(r.text("zonaId") + " · " + r.text("sensorOrigen"))
        Text(date(r.time("fechaHora")),color = Muted)
        if(!compact) {
            Text("Dispositivo: " + r.text("dispositivoId"))
            Text("Distancia: " + (r.number("distanciaCm")?.let { "$it cm" } ?: "Inválida") + " · PIR: " + if(r.flag("movimientoDetectado")) "Sí" else "No")
            Text("Lectura " + if(r.flag("lecturaValida")) "válida" else "inválida")
            SelectionContainerText("ID: " + r.id)
        }
        Text(if(r.pending) "Pendiente de confirmación" else "Confirmado en Firebase",color = Green,style = MaterialTheme.typography.labelMedium)
    }
}
@Composable private fun Events(records: List<Record>, compact: Boolean) {
    var zone by rememberSaveable { mutableStateOf("") }
    var device by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var sensor by rememberSaveable { mutableStateOf("") }
    var risk by rememberSaveable { mutableStateOf("todos") }
    var day by rememberSaveable { mutableStateOf("") }
    val parsed = if(day.isBlank()) null else runCatching { LocalDate.parse(day) }.getOrNull()
    val matches = records.filter { r -> r.text("zonaId").contains(zone,true) && r.text("dispositivoId").contains(device,true) &&
        r.text("tipoEvento").contains(type,true) && r.text("sensorOrigen").contains(sensor,true) &&
        (risk == "todos" || r.text("nivelRiesgo") == risk) &&
        (day.isBlank() || (parsed != null && r.time("fechaHora")?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } == parsed)) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel("Filtros de historial") {
            Field(zone,"Zona",{zone=it}); Field(device,"Dispositivo",{device=it})
            Field(type,"Tipo de evento",{type=it}); Field(sensor,"Sensor",{sensor=it})
            Field(day,"Fecha local AAAA-MM-DD",{day=it})
            if(day.isNotBlank() && parsed == null) Text("Fecha inválida",color = MaterialTheme.colorScheme.error)
            Choice(listOf("todos","informacion","advertencia","riesgo"),risk) {risk=it}
            Text("Resultados: ${matches.size} de ${records.size} eventos cargados (máximo 200).",color = Muted)
        }
        if(matches.isEmpty()) Text("No hay eventos que coincidan.")
        matches.forEach { EventCard(it,compact) }
    }
}
@Composable private fun Choice(options: List<String>, selected: String, change: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(selected == it,onClick = {change(it)},label = {Text(it)}) }
    }
}
@Composable private fun Toggle(text: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
        Text(text,Modifier.weight(1f)); Switch(value,change)
    }
}
@Composable private fun Control(state: AppState, vm: ViaTechViewModel) {
    val devices = state.feeds["dispositivos"]?.records.orEmpty().filter { it.flag("activo") }
    var device by rememberSaveable { mutableStateOf("esp32-demo") }
    var distance by rememberSaveable { mutableStateOf("35") }
    var motion by rememberSaveable { mutableStateOf(true) }
    val valid = distance.toDoubleOrNull()?.let(Validation::distance) == true
    Panel("Control de la maqueta simulada") {
        Text("El simulador debe estar ejecutándose para confirmar comandos. No hay actuadores viales reales.")
        Choice(devices.map { it.id },device) {device=it}
        Field(distance,"HC-SR04 · distancia de prueba (2–400 cm)",{distance=it},KeyboardType.Decimal)
        Toggle("PIR · movimiento",motion) {motion=it}
        listOf("simular_lectura" to "Generar evento simulado","solicitar_lectura" to "Solicitar lectura","led_on" to "Encender LED de prueba","led_off" to "Apagar LED de prueba").forEach { (type,label) ->
            Button(onClick = {vm.command(device,type,distance.toDouble(),motion)},enabled = valid && devices.any {it.id==device} && !state.busy && !state.offline,modifier = Modifier.fillMaxWidth()) {Text(label)}
        }
    }
}
@Composable private fun UserEditor(state: AppState, vm: ViaTechViewModel) {
    var uid by rememberSaveable {mutableStateOf("")}
    var role by rememberSaveable {mutableStateOf("consulta")}
    var active by rememberSaveable {mutableStateOf(true)}
    Panel("Autorizar o actualizar cuenta") {
        Text("Introduce el UID de una cuenta existente en Authentication. No se permite modificar tu propia autorización.")
        Field(uid,"UID de usuario",{uid=it}); Choice(listOf("consulta","operador","administrador"),role){role=it}
        Toggle("Acceso activo",active){active=it}
        Button(onClick={vm.saveUser(uid,role,active)},enabled=uid.isNotBlank() && uid!=state.uid && !state.busy && !state.offline){Text("Guardar autorización")}
    }
}
@Composable private fun ZoneEditor(vm: ViaTechViewModel) {
    var id by rememberSaveable {mutableStateOf("")}
    var name by rememberSaveable {mutableStateOf("")}
    var type by rememberSaveable {mutableStateOf("paso_cebra")}
    var active by rememberSaveable {mutableStateOf(true)}
    Panel("Crear o actualizar zona") {
        Field(id,"ID (letras minúsculas, números, guion)",{id=it}); Field(name,"Nombre",{name=it})
        Choice(listOf("paso_cebra","acceso_peatonal","zona_vehicular","lomo_toro"),type){type=it}
        Toggle("Zona activa",active){active=it}
        Button(onClick={vm.saveZone(id,name,type,active)},enabled=id.isNotBlank()&&name.isNotBlank()){Text("Guardar zona")}
    }
}
@Composable private fun DeviceEditor(state: AppState, vm: ViaTechViewModel) {
    var id by rememberSaveable {mutableStateOf("")}
    var name by rememberSaveable {mutableStateOf("")}
    var zone by rememberSaveable {mutableStateOf("paso-cebra")}
    var active by rememberSaveable {mutableStateOf(true)}
    Panel("Registrar o actualizar dispositivo") {
        Field(id,"ID ESP32",{id=it}); Field(name,"Nombre",{name=it})
        Choice(state.feeds["zonas"]?.records.orEmpty().map {it.id},zone){zone=it}
        Toggle("Dispositivo autorizado",active){active=it}
        Text("La identidad del dispositivo se provisiona por separado desde un entorno confiable.",color=Muted)
        Button(onClick={vm.saveDevice(id,name,zone,active)},enabled=id.isNotBlank()&&name.isNotBlank()&&!state.busy&&!state.offline){Text("Guardar dispositivo")}
    }
}
@Composable private fun Settings(state: AppState, vm: ViaTechViewModel) {
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Panel("Tu sesión") {
            SelectionContainerText("UID: "+state.uid.orEmpty())
            Text("Rol: "+state.profile?.rol)
            Text("Proyecto: "+BuildConfig.FIREBASE_PROJECT)
            Text("ViaTech 1.0 · Kotlin + Firebase")
        }
        Panel("Preferencias y conectividad") {
            Toggle("Tarjetas de eventos compactas",state.compact,vm::compact)
            Toggle("Simular desconexión de Firestore",state.offline,vm::offline)
            Text("La desconexión afecta la consulta Android. El simulador conserva su propia cola local. La autenticación requiere conexión.",color=Muted)
            Text(if(BuildConfig.USE_EMULATORS) "Laboratorio local: conexión sin TLS al emulador. No usar con cuentas reales." else "Conexión Firebase mediante TLS del SDK; tráfico HTTP bloqueado.",color=Muted)
        }
        Panel("Permisos y privacidad") {
            Text("INTERNET y ACCESS_NETWORK_STATE son permisos normales concedidos por Android al instalar. No se solicitan permisos peligrosos innecesarios.")
            Text("Las contraseñas las administra Firebase Authentication. No se guardan manualmente en el dispositivo.")
            Text("ISO/IEC 27400 se utiliza como guía de diseño; no es una certificación.")
        }
        OutlinedButton(onClick=vm::logout,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Cerrar sesión")}
    }
}
