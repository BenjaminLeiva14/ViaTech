# Plan y trazabilidad de pruebas
Las pruebas JVM y Node son automáticas. Las instrumentadas requieren Android disponible. Los resultados se registran por separado; un caso preparado no equivale a aprobado.
| Caso | Acción | Resultado esperado | Evidencia |
|---|---|---|---|
| AUTH-1 | Correo/contraseña válidos | sesión Auth | auth.test.mjs |
| AUTH-2 | Contraseña inválida | rechazo sin detalles de cuenta | auth.test.mjs |
| AUTH-3 | Recuperar contraseña | código de recuperación en emulador | auth.test.mjs |
| AUTH-4 | Cerrar sesión | currentUser nulo | auth.test.mjs |
| AUTH-5 | Registro | sin autoasignación de rol | rules.test.mjs y LoginUiTest |
| SEC-1 | Listar sin sesión | denegación en siete colecciones | rules.test.mjs |
| SEC-2 | Escalar rol propio | denegación | rules.test.mjs |
| SEC-3 | Dispositivo ajeno | denegación de escritura | rules.test.mjs |
| SEC-4 | Modificar evento confirmado | denegación | rules.test.mjs |
| SEC-5 | Strings/tipos/rangos/esquemas inválidos | denegación | rules.test.mjs |
| SEC-6 | Auditoría faltante o alterada | denegación | rules.test.mjs |
| SEC-7 | Replay de comando | denegación | rules.test.mjs |
| SEC-8 | Dispositivo revocado | denegación inmediata | rules.test.mjs |
| DATA-1 | Simular movimiento y distancia | riesgo consistente | domain.test.mjs y ValidationTest |
| DATA-2 | Escribir lectura y evento | mismo ID y lote atómico | integration.test.mjs |
| DATA-3 | Dos clientes | mismo eventoId recibido | integration.test.mjs |
| DATA-4 | Desconectar y reconectar cliente | evento conservado | integration.test.mjs |
| IOT-1 | Pendiente → ejecutado | confirma identidad destino | integration.test.mjs |
| IOT-2 | Detección sin red, reiniciar proceso, reconectar | cola persiste y no duplica | cloud-queue.test.mjs, ejecutada (red SDK simulada) |
| UI-1 | Login inválido y registro | validación, sin selector admin | LoginUiTest (instrumentado) |
| UI-2 | Navegación por rol, rotación, fuente ampliada | vistas correctas sin cortes | Android manual pendiente |
| AND-1 | Dos Android Wi-Fi | mismo evento con evidencia de ambos | demostración pendiente |

## Ataques de reglas considerados
Lectura/listado público, acceso ajeno, escalación de roles, modificación de dueño, corrupción de tipos, campos extra/omitidos, texto excesivo, timestamp falso/futuro, rangos inválidos, zona inexistente, dispositivo revocado, replay de comandos y subcolecciones no definidas.
Contadores incrementales y URLs no forman parte del esquema (no aplica). Datos privados Auth no se replican en Firestore. El servicio se diseña para revisión adicional antes de distribución amplia.
