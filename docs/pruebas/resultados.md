# Resultados ejecutados
Fecha local: 2026-11-01. Los logs completos permanecen locales y excluidos de Git para evitar datos de sesiones.

| Prueba | Esperado | Obtenido | Estado |
|---|---|---|---|
| Kotlin ValidationTest | Validaciones y heartbeat | 4 casos, 0 fallos, 0 errores | aprobada |
| assembleLocalDebug | APK instalable generado | BUILD SUCCESSFUL, 45 tareas, 37 min 4 s | compilación aprobada |
| Dominio Node | Riesgo y lecturas inválidas | 4 casos aprobados | aprobada |
| Auth emulado | Login válido/inválido, recuperación, logout | 1 caso compuesto aprobado | aprobada |
| Reglas Firestore | Autorización y esquemas | 34 casos aprobados | aprobada |
| Integración inicial | Dos clientes, evento y comando | Timeout 240 s | cancelada |
| Integración corregida | Seed finaliza; evento y comando sincronizados | 1 caso aprobado en 54,97 s | aprobada en repetición individual |
| Reconexión Firestore | Mismo evento después de desactivar/activar red SDK | Assert correcto dentro de integración | aprobada con alcance limitado |
| Cola con caída/reinicio | Mantener evento en disco y no duplicar | Implementado; sin prueba de fallo/reinicio | pendiente |
| Instalación/navegación Android | App ejecutada sin crash | Sin destino ADB; hipervisor ausente | no ejecutada |
| Dos Android | Evento visible en ambos | Solo dos clientes SDK Node comprobados | no ejecutada |
| Nube Firebase | Base, Auth, despliegue y escritura | GCP creado sin facturación; activación Firebase 403 | pendiente |
| Wokwi/sensores físicos | Funcionamiento experimental | Diseño teórico | no ejecutada |

## Evidencia reproducible
- android/app/build/test-results/testLocalDebugUnitTest/TEST-cl.amigovirtual.app.ValidationTest.xml.
- docs/pruebas/android-build.log, firebase-test-run.log y firebase-integration-rerun.log (archivos locales ignorados).
- Resumen portable: docs/evidencias/verificacion-local.json.
- npm-audit-production.json: seis alertas moderadas, cero altas/críticas; requiere evaluación antes de distribución.
- SHA256 APK local: 79e140e0e3ee6f2739a053f300609472ce533dafd55e3325505cac5c1123140e.

La ejecución inicial Node tuvo 40 casos: 39 aprobados, 0 fallos y 1 cancelado. Se corrigieron el cierre de conexiones del seed y los límites de espera; la repetición de ese caso pasó. Esto no afirma una segunda ejecución completa ni validación de la UI Android.

## Repetir
Desde raíz: ./gradlew.bat :app:assembleLocalDebug :app:testLocalDebugUnitTest.
Desde firebase: npm ci y npm run verify, con JAVA_HOME apuntando al JDK.
Para UI: conectar Android/AVD con ADB y ejecutar :app:connectedLocalDebugAndroidTest con emuladores Firebase activos.
