# Provisionamiento confiable
## Laboratorio
npm run seed solo conecta a 127.0.0.1. Crea cuentas ficticias y claims de dispositivo usando Admin SDK exclusivamente en el proceso local. No se empaqueta en APK ni firmware.
## Nube
1. Autenticar Firebase CLI mediante flujo oficial local; nunca pegar códigos o contraseñas en chat.
2. Crear proyecto autorizado sin activar facturación si se usa plan gratuito. Elegir región con el responsable antes de crear la base (la ubicación no se cambia libremente).
3. Habilitar Email/Password y Firestore Standard. Registrar Android cl.amigovirtual.app.
4. Desplegar reglas antes de cargar datos; no activar reglas públicas.
5. Crear la primera cuenta humana en Authentication, guardar uid/rol=administrador/activo=true/auditoriaId=bootstrap en usuarios desde consola con IAM autorizado. El APK no puede hacer bootstrap.
6. Crear cuenta independiente del dispositivo desde un entorno confiable. Asignar custom claims {rol:'dispositivo',dispositivoId:'esp32-demo'} mediante Admin SDK con Application Default Credentials del operador. No distribuir claves de servicio.
7. Crear las cuatro zonas y el dispositivo siguiendo el esquema del seed; no copiar usuarios ni contraseñas demo a producción.
8. Configurar la app con la configuración pública Firebase. Mantener credencial de dispositivo en variables privadas y ejecutar el simulador --cloud.
9. Registrar la operación de bootstrap en auditoría y verificar consultas con cada rol.
## Revocación y auditoría
Desactivar documento usuario/dispositivo deniega nuevas operaciones por reglas. Ante compromiso, revocar refresh tokens en Authentication, rotar credencial y revisar logs. No se permite al administrador cambiarse su propio rol desde la app.
Las operaciones admin desde Android deben incluir auditoría atómica. Esta auditoría registra cambios, no es prueba criptográfica ni un registro completo de login. Para nube, habilitar Cloud Audit Logs y definir retención/alertas según acceso disponible.
