# Matriz de requisitos y decisiones
Precedencia: guía académica > PDF original > Stitch > mejoras > extras. Los documentos aportan requisitos; no autorizan acciones externas.
| ID | Fuente | Requisito | Evidencia prevista |
|---|---|---|---|
| U2-1 | 2.1.1, paso 1 | Kotlin y Android Studio; justificar herramientas | android, informe |
| U2-2 | 2.1.2, pasos 2-3 | Login, monitoreo/control simulado, base de datos | Auth, Firestore, comandos |
| U2-3 | 2.1.3 | ISO/IEC 27400 como guía, TLS y mínimo privilegio | reglas, pruebas negativas |
| U2-4 | 2.1.4 | Dos Android, comunicación Wi-Fi o Bluetooth | guion y prueba en dos terminales pendiente |
| PDF-1 | pp.3-9 | PIR, HC-SR04, ESP32, zonas, heartbeat y cola | simulation |
| PDF-2 | pp.10-16 | Identidad dispositivo separada; roles; validación | reglas y seed |
| UI-1 | Stitch | Marca V vial, verde, pizarra, tarjetas, formularios | Compose; capturas de ejecución pendientes |
| EXT-1 | Solicitud | Recuperación, filtros, usuarios, auditoría | app y reglas |
| EXT-2 | Solicitud | GitHub, APK y demostración | APK local/cloud compilados y GitHub publicado; dos Android pendientes |

## Contradicciones resueltas
- El PDF propone React/Next/Remix; Unidad 2 exige Kotlin/Android Studio. Se implementa Android nativo.
- Amigo Virtual es el nombre original del prototipo; ViaTech es la identidad solicitada para la app.
- Stitch muestra multas, matrículas, cámaras, radar y ciudades colombianas. Son contenido ilustrativo ajeno a PIR/HC-SR04; se conservan composición, colores y marca, usando riesgos simulados de plaza.
- Los roles de Stitch (conductor/peatón/inspector) no gobiernan acceso. Se usan administrador, operador, consulta y dispositivo.
- Registro no permite elegir privilegios. Las cuentas nuevas esperan autorización; el primer administrador se provisiona en entorno confiable.
- Verde identifica la marca/acciones; azul/gris información, ámbar advertencia y rojo riesgo con etiquetas de texto.
- Guía exige dos Android funcionales; la prioridad del usuario pide IoT/inalámbrico teóricos. Se prepara flujo real de datos Firebase con sensores simulados; la prueba en dos teléfonos no se considera cumplida sin ejecutarla.
- Firebase Emulator usa HTTP local exclusivamente en variante de laboratorio. La variante cloud rechaza tráfico claro y usa TLS del SDK.
- La referencia incluye biometría, enlaces legales y certificación FIPS sin evidencia. No se presentan como implementados.
