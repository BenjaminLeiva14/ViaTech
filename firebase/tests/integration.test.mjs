import test from 'node:test';
import assert from 'node:assert/strict';
import {execFile} from 'node:child_process';
import {promisify} from 'node:util';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,signInWithEmailAndPassword,signOut} from 'firebase/auth';
import {getFirestore,connectFirestoreEmulator,doc,writeBatch,serverTimestamp,getDoc,onSnapshot,disableNetwork,enableNetwork} from 'firebase/firestore';
const exec=promisify(execFile);
test('integración: dispositivo, comando, dos clientes y reconexión',{timeout:600000},async()=>{
 console.log('INTEGRACION: cargando datos locales');
 await exec(process.execPath,['scripts/seed.mjs'],{timeout:300000});
 console.log('INTEGRACION: datos listos');
 const clients=[];
 async function client(name,email){
  const app=initializeApp({apiKey:'demo-api-key',projectId:'demo-viatech'},name);
  const auth=getAuth(app),db=getFirestore(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});connectFirestoreEmulator(db,'127.0.0.1',8080);
  await signInWithEmailAndPassword(auth,email,'ViaTechLocal2026!');clients.push(app);return{auth,db};
 }
 function observed(db,id){return new Promise((resolve,reject)=>{
  const timer=setTimeout(()=>{unsub();reject(Error('No llegó el evento al cliente'));},300000);
  const unsub=onSnapshot(doc(db,'eventos',id),snapshot=>{if(snapshot.exists()){clearTimeout(timer);unsub();resolve(snapshot.data());}},reject);
 });}
 try{
  const a=await client('client-a','admin@viatech.test'),b=await client('client-b','operador@viatech.test');
  const id=crypto.randomUUID(),eventId='cmd-'+id;
  const watchA=observed(a.db,eventId),watchB=observed(b.db,eventId);
  const batch=writeBatch(a.db);
  batch.set(doc(a.db,'comandos',id),{comandoId:id,dispositivoId:'esp32-demo',tipo:'simular_lectura',estado:'pendiente',solicitante:'admin-demo',creacion:serverTimestamp(),ejecucion:null,distanciaCm:35,movimientoDetectado:true,resultado:'',origenDatos:'simulado'});
  batch.set(doc(a.db,'auditoria',id),{actor:'admin-demo',accion:'comando',destino:id,fechaHora:serverTimestamp()});
  await batch.commit(); console.log('INTEGRACION: comando confirmado');
  await exec(process.execPath,['../simulation/simulator.mjs','--once'],{timeout:300000});
  console.log('INTEGRACION: simulador terminado');
  const [ea,eb]=await Promise.all([watchA,watchB]);
  assert.equal(ea.eventoId,eb.eventoId);assert.equal(ea.origenDatos,'simulado');assert.equal(ea.nivelRiesgo,'riesgo');
  assert.equal((await getDoc(doc(a.db,'comandos',id))).data().estado,'ejecutado');
  await disableNetwork(b.db);await enableNetwork(b.db);
  assert.equal((await getDoc(doc(b.db,'eventos',eventId))).data().eventoId,eventId);
  await signOut(a.auth);await signOut(b.auth);
 }finally{await Promise.all(clients.map(deleteApp));}
});
