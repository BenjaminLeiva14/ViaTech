// This bootstrap is restricted to local emulators. No production credentials are loaded.
import { initializeApp, getApp, deleteApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore, Timestamp } from 'firebase-admin/firestore';
const projectId = 'demo-viatech';
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST || '127.0.0.1:9099';
const dbHost = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
if (!/^127\.0\.0\.1:\d+$/.test(authHost) || !/^127\.0\.0\.1:\d+$/.test(dbHost)) throw Error('Seed autorizado solo en loopback');
process.env.FIREBASE_AUTH_EMULATOR_HOST = authHost;
process.env.FIRESTORE_EMULATOR_HOST = dbHost;
initializeApp({projectId});
const auth = getAuth(), db = getFirestore();
const users = [
 ['admin-demo','admin@viatech.test','administrador'],
 ['operador-demo','operador@viatech.test','operador'],
 ['consulta-demo','consulta@viatech.test','consulta'],
 ['device-demo','esp32@viatech.test','dispositivo']
];
for (const [uid,email,rol] of users) {
 try { await auth.getUser(uid); } catch(e) { if(e.code!=='auth/user-not-found') throw e; await auth.createUser({uid,email,password:'ViaTechLocal2026!'}); }
 if(rol==='dispositivo') await auth.setCustomUserClaims(uid,{rol,dispositivoId:'esp32-demo'});
 else if(!(await db.doc('usuarios/'+uid).get()).exists) await db.doc('usuarios/'+uid).set({uid,rol,activo:true,auditoriaId:'bootstrap'});
}
for (const [zonaId,nombre,tipo] of [
 ['paso-cebra','Paso de cebra','paso_cebra'],['acceso-peatonal','Acceso peatonal','acceso_peatonal'],
 ['zona-vehicular','Zona vehicular','zona_vehicular'],['lomo-toro','Lomo de toro','lomo_toro']
]) {
 const ref=db.doc('zonas/'+zonaId);
 if(!(await ref.get()).exists) await ref.set({zonaId,nombre,tipo,activo:true,sensores:['PIR','HC-SR04'],auditoriaId:'bootstrap'});
}
const device=db.doc('dispositivos/esp32-demo');
if(!(await device.get()).exists) await device.set({dispositivoId:'esp32-demo',nombre:'ESP32 Plaza · Simulado',zonaId:'paso-cebra',
 activo:true,versionFirmware:'sim-1.0',sensores:['PIR','HC-SR04'],ultimaComunicacion:null,led:false,origenDatos:'simulado',auditoriaId:'bootstrap'});
if(!(await db.doc('auditoria/bootstrap').get()).exists) await db.doc('auditoria/bootstrap').set({actor:'bootstrap-local',accion:'bootstrap',destino:'demo-viatech',fechaHora:Timestamp.now()});
console.log('Seed local listo: 3 usuarios autorizados, identidad ESP32, 4 zonas y dispositivo. No se sobrescribieron registros existentes.');

await db.terminate();
await deleteApp(getApp());
