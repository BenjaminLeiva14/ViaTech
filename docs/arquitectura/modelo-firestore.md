# Modelo Firestore
Instancia de laboratorio: demo-viatech/(default), API nativa compatible con reglas Standard. La edición de un proyecto de nube no se ha verificado sin sesión autorizada.

| Colección | ID | Datos principales | Lectura | Escritura |
|---|---|---|---|---|
| usuarios | UID Auth | uid, rol, activo, auditoriaId | propietario y administrador | administrador distinto del propietario; auditoría |
| dispositivos | ID ESP32 | zona, firmware, sensores, heartbeat, LED, activo | operador/admin; dispositivo propio | admin configuración; dispositivo heartbeat/LED |
| zonas | ID zona | nombre, tipo, sensores, activo | usuarios autorizados | admin con auditoría |
| lecturas | UUID | esquema validado de medición | operador/admin y dispositivo propio | dispositivo propio, solo crear |
| eventos | UUID | medición, riesgo, descripción, fechas, origen | usuarios autorizados y dispositivo propio | dispositivo propio, solo crear |
| comandos | UUID | destinatario, solicitante, tipo, estado, creación/ejecución | admin y dispositivo destinatario | admin crea; dispositivo confirma una vez |
| auditoria | UUID | actor, acción, destino, fecha | admin | admin crea, inmutable |

Los usuarios no contienen correo, nombre ni contraseña. Authentication administra información privada. Un UID puede ser pseudónimo identificable y solo se muestra al propietario/admin.
Todos los campos permitidos son explícitos; strings y listas están acotados. Evento y lectura comparten esquema para validar un único contrato. Distancia válida 2–400 cm (rango asumido para maqueta, no calibrado); si inválida, null. PIR bool. Riesgo: distancia <50 y movimiento → riesgo; <100 → advertencia; resto → información. No equivale a inferir velocidad.
Comandos: led_on, led_off, solicitar_lectura, simular_lectura. Estado pendiente → ejecutado/error; no replay ni cambio de solicitante.
Índice compuesto: comandos(dispositivoId asc, estado asc). Android usa índices de campo único por fechaHora/creacion, límites 200/100. Filtros sobre la muestra cargada se identifican explícitamente; histórico completo/paginación pendiente.
No hay borrado desde la app. Desactivar usuarios/dispositivos revoca acceso por documento. Reglas no pueden verificar existencia de UID en Authentication: administración debe copiar UID existente, y la provisión inicial es confiable.
