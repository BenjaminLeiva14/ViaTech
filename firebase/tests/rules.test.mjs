import {test,before,after,beforeEach} from 'node:test';
import {readFile} from 'node:fs/promises';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,getDocs,collection,updateDoc,deleteDoc,writeBatch,serverTimestamp,Timestamp,query,orderBy,limit,where} from 'firebase/firestore';
import {measurement} from '../../simulation/domain.mjs';
let env;
const ctx=(uid,claims={})=>env.authenticatedContext(uid,claims).firestore();
const zone={zonaId:'paso-cebra',nombre:'Paso de cebra',tipo:'paso_cebra',activo:true,sensores:['PIR','HC-SR04'],auditoriaId:'bootstrap'};
const device={dispositivoId:'esp32-demo',nombre:'Simulador',zonaId:'paso-cebra',activo:true,versionFirmware:'sim-1.0',sensores:['PIR','HC-SR04'],ultimaComunicacion:null,led:false,origenDatos:'simulado',auditoriaId:'bootstrap'};
const event=(id='e1')=>({...measurement(35,true,'esp32-demo','paso-cebra',id),detectadoEn:Timestamp.now(),fechaHora:serverTimestamp()});
const deviceDb=()=>ctx('device',{rol:'dispositivo',dispositivoId:'esp32-demo'});
const command=(id='c1')=>({comandoId:id,dispositivoId:'esp32-demo',tipo:'led_on',estado:'pendiente',solicitante:'admin',creacion:serverTimestamp(),ejecucion:null,distanciaCm:35,movimientoDetectado:true,resultado:'',origenDatos:'simulado'});
async function audited(db,path,data,action,target,id='a1'){
 const batch=writeBatch(db);
 batch.set(doc(db,path),{...data,auditoriaId:id});
 batch.set(doc(db,'auditoria',id),{actor:'admin',accion:action,destino:target,fechaHora:serverTimestamp()});
 return batch.commit();
}
async function sendCommand(db,id='c1',overrides={}){
 const batch=writeBatch(db);
 batch.set(doc(db,'comandos',id),{...command(id),...overrides});
 batch.set(doc(db,'auditoria',id),{actor:'admin',accion:'comando',destino:id,fechaHora:serverTimestamp()});
 return batch.commit();
}
before(async()=>{
 env=await initializeTestEnvironment({projectId:'demo-viatech-rules',firestore:{host:'127.0.0.1',port:8080,rules:await readFile(new URL('../firestore.rules',import.meta.url),'utf8')}});
});
beforeEach(async()=>{
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(async c=>{
  const db=c.firestore();
  for(const [uid,rol] of [['admin','administrador'],['op','operador'],['view','consulta']])
   await setDoc(doc(db,'usuarios',uid),{uid,rol,activo:true,auditoriaId:'bootstrap'});
  await setDoc(doc(db,'zonas/paso-cebra'),zone);
  await setDoc(doc(db,'dispositivos/esp32-demo'),device);
 });
});
after(async()=>env?.cleanup());
test('anónimo no puede listar ninguna colección',async()=>{
 const db=env.unauthenticatedContext().firestore();
 for(const c of ['usuarios','eventos','lecturas','zonas','dispositivos','comandos','auditoria']) await assertFails(getDocs(collection(db,c)));
});
test('cuenta nueva no se asigna administrador',async()=>assertFails(setDoc(doc(ctx('new'),'usuarios/new'),{uid:'new',rol:'administrador',activo:true,auditoriaId:'a1'})));
test('operador no cambia su rol ni otro usuario',async()=>{
 await assertFails(updateDoc(doc(ctx('op'),'usuarios/op'),{rol:'administrador'}));
 await assertFails(updateDoc(doc(ctx('op'),'usuarios/view'),{rol:'administrador'}));
});
test('administrador no altera su propia autorización',async()=>assertFails(audited(ctx('admin'),'usuarios/admin',{uid:'admin',rol:'administrador',activo:false},'usuario','admin')));
test('administrador autoriza otra cuenta con auditoría atómica',async()=>{
 await assertSucceeds(audited(ctx('admin'),'usuarios/new',{uid:'new',rol:'operador',activo:true},'usuario','new'));
 await assertSucceeds(getDocs(collection(ctx('new'),'dispositivos')));
});
test('actualización administrativa sin auditoría es rechazada',async()=>assertFails(updateDoc(doc(ctx('admin'),'usuarios/op'),{activo:false})));
test('consulta lee historial pero no telemetría ni usuarios ajenos',async()=>{
 await assertSucceeds(getDocs(query(collection(ctx('view'),'eventos'),orderBy('fechaHora','desc'),limit(200))));
 for(const path of ['usuarios/op','dispositivos/esp32-demo']) await assertFails(getDoc(doc(ctx('view'),path)));
 await assertFails(getDocs(collection(ctx('view'),'lecturas')));
});
test('operador consulta sensores y no auditoría',async()=>{
 await assertSucceeds(getDocs(collection(ctx('op'),'lecturas')));
 await assertFails(getDocs(collection(ctx('op'),'auditoria')));
});
test('dispositivo escribe evento válido y lectura',async()=>{
 const db=deviceDb(),b=writeBatch(db),e=event();
 b.set(doc(db,'eventos/e1'),e);b.set(doc(db,'lecturas/e1'),e);
 await assertSucceeds(b.commit());
});
test('usuario humano no falsifica evento',async()=>assertFails(setDoc(doc(ctx('admin'),'eventos/e1'),event())));
test('dispositivo no suplanta otro dispositivo',async()=>assertFails(setDoc(doc(ctx('other',{rol:'dispositivo',dispositivoId:'other'}),'eventos/e1'),event())));
test('evento confirmado es inmutable y no se borra',async()=>{
 const db=deviceDb();await assertSucceeds(setDoc(doc(db,'eventos/e1'),event()));
 await assertFails(updateDoc(doc(db,'eventos/e1'),{descripcion:'modificado'}));
 await assertFails(deleteDoc(doc(db,'eventos/e1')));
});
for(const [name,patch] of [
 ['campo extra',{extra:'injection'}],['distancia negativa',{distanciaCm:-1}],['distancia excesiva',{distanciaCm:401}],
 ['PIR con tipo incorrecto',{movimientoDetectado:1}],['fecha falsa',{fechaHora:Timestamp.fromMillis(0)}],
 ['tiempo futuro',{detectadoEn:Timestamp.fromMillis(Date.now()+86400000)}],['texto enorme',{descripcion:'x'.repeat(4000)}],
 ['zona inexistente',{zonaId:'missing'}],['riesgo falsificado',{nivelRiesgo:'informacion'}],['sensor desconocido',{sensorOrigen:'camara'}],
 ['origen real falsificado',{origenDatos:'real'}],['propietario modificado',{dispositivoId:'otro'}]
]) test('rechaza '+name,async()=>assertFails(setDoc(doc(deviceDb(),'eventos/e1'),{...event(),...patch})));
test('rechaza campos obligatorios omitidos',async()=>{const e=event();delete e.descripcion;await assertFails(setDoc(doc(deviceDb(),'eventos/e1'),e));});
test('lectura inválida se registra con distancia nula',async()=>{
 const e={...measurement(NaN,true,'esp32-demo','paso-cebra','e1'),detectadoEn:Timestamp.now(),fechaHora:serverTimestamp()};
 await assertSucceeds(setDoc(doc(deviceDb(),'eventos/e1'),e));
});
test('comando admin requiere auditoría y propietario correcto',async()=>{
 await assertFails(setDoc(doc(ctx('admin'),'comandos/c1'),command()));
 await assertFails(sendCommand(ctx('admin'),'c1',{solicitante:'op'}));
 await assertSucceeds(sendCommand(ctx('admin')));
});
test('operador no envía comandos',async()=>assertFails(sendCommand(ctx('op'))));
test('dispositivo confirma pendiente una sola vez y conserva destinatario',async()=>{
 await sendCommand(ctx('admin'));
 const ref=doc(deviceDb(),'comandos/c1');
 await assertFails(updateDoc(ref,{estado:'ejecutado',ejecucion:serverTimestamp(),resultado:'ok',solicitante:'other'}));
 await assertSucceeds(updateDoc(ref,{estado:'ejecutado',ejecucion:serverTimestamp(),resultado:'ok'}));
 await assertFails(updateDoc(ref,{estado:'pendiente',ejecucion:null,resultado:''}));
});
test('dispositivo consulta exclusivamente comandos de su ID',async()=>{
 await sendCommand(ctx('admin'));
 await assertSucceeds(getDocs(query(collection(deviceDb(),'comandos'),where('dispositivoId','==','esp32-demo'),where('estado','==','pendiente'))));
 await assertFails(getDocs(collection(deviceDb(),'comandos')));
});
test('heartbeat propio permitido y cambio de zona prohibido',async()=>{
 const ref=doc(deviceDb(),'dispositivos/esp32-demo');
 await assertSucceeds(updateDoc(ref,{ultimaComunicacion:serverTimestamp()}));
 await assertFails(updateDoc(ref,{zonaId:'otra'}));
 await assertFails(updateDoc(ref,{activo:false}));
});
test('dispositivo revocado pierde escritura inmediatamente',async()=>{
 await env.withSecurityRulesDisabled(c=>updateDoc(doc(c.firestore(),'dispositivos/esp32-demo'),{activo:false}));
 await assertFails(setDoc(doc(deviceDb(),'eventos/e1'),event()));
});
test('zona valida y defensa de actualización de esquema',async()=>{
 await assertSucceeds(audited(ctx('admin'),'zonas/paso-cebra',zone,'zona','paso-cebra'));
 await assertFails(audited(ctx('admin'),'zonas/paso-cebra',{...zone,nombre:'x'.repeat(1000)},'zona','paso-cebra','a2'));
 await assertFails(audited(ctx('admin'),'zonas/paso-cebra',{...zone,sensores:['PIR',{bad:true}]},'zona','paso-cebra','a3'));
});
test('auditoría inmutable y subcolecciones denegadas',async()=>{
 await sendCommand(ctx('admin'));
 await assertFails(updateDoc(doc(ctx('admin'),'auditoria/c1'),{accion:'usuario'}));
 await assertFails(setDoc(doc(ctx('admin'),'usuarios/admin/private/x'),{secret:'bad'}));
});
