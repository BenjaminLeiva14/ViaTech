import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {execFile} from 'node:child_process';
import {promisify} from 'node:util';
import {fileURLToPath} from 'node:url';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,signInWithEmailAndPassword,signOut} from 'firebase/auth';
import {getFirestore,doc,collection,getDocs,getDocFromServer,writeBatch,serverTimestamp,onSnapshot,disableNetwork,enableNetwork} from 'firebase/firestore';
const exec=promisify(execFile);
// Explicit cloud test, excluded from npm test. Credentials are never logged.
test('ViaTech nube: roles, dos clientes, simulador, comando y reconexion',{timeout:240000},async()=>{
 const sdk=JSON.parse(await readFile(new URL('../../android/app/google-services.json',import.meta.url),'utf8'));
 const secrets=JSON.parse(await readFile(new URL('../.env.cloud-demo.json',import.meta.url),'utf8'));
 assert.equal(secrets.project,'viatech-inacap-bleiva-2026');assert.equal(secrets.project,sdk.project_info.project_id);
 const clients=[],cleanup=[];
 async function client(role){
  const account=secrets.accounts.find(x=>x.rol===role),app=initializeApp({projectId:secrets.project,apiKey:sdk.client[0].api_key[0].current_key},'cloud-'+role);
  clients.push(app);const auth=getAuth(app),db=getFirestore(app);
  await signInWithEmailAndPassword(auth,account.email,account.password);return{auth,db,account};
 }
 function observe(db,id){return new Promise((resolve,reject)=>{
  const timer=setTimeout(()=>reject(Error('No llego evento remoto')),120000);
  const unsub=onSnapshot(doc(db,'eventos',id),s=>{if(s.exists()&&!s.metadata.fromCache&&!s.metadata.hasPendingWrites){clearTimeout(timer);resolve(s.data());}},e=>{clearTimeout(timer);reject(e);});
  cleanup.push(()=>{clearTimeout(timer);unsub();});
 });}
 try{
  const a=await client('administrador'),b=await client('operador'),c=await client('consulta');
  await assert.rejects(signInWithEmailAndPassword(c.auth,c.account.email,'Incorrecta-'+crypto.randomUUID()));
  const id=crypto.randomUUID(),eventId='cmd-'+id;
  const command={comandoId:id,dispositivoId:'esp32-demo',tipo:'simular_lectura',estado:'pendiente',solicitante:a.account.uid,creacion:serverTimestamp(),ejecucion:null,distanciaCm:35,movimientoDetectado:true,resultado:'',origenDatos:'simulado'};
  const denied=writeBatch(b.db);denied.set(doc(b.db,'comandos','deny-'+id),{...command,comandoId:'deny-'+id,solicitante:b.account.uid});
  await assert.rejects(denied.commit(),e=>e.code==='permission-denied');
  const watched=Promise.all([observe(a.db,eventId),observe(b.db,eventId)]);
  const batch=writeBatch(a.db);batch.set(doc(a.db,'comandos',id),command);batch.set(doc(a.db,'auditoria',id),{actor:a.account.uid,accion:'comando',destino:id,fechaHora:serverTimestamp()});await batch.commit();
  await exec(process.execPath,[fileURLToPath(new URL('simulator-cloud.mjs',import.meta.url)),'--once'],{timeout:150000});
  const [ea,eb]=await watched;
  assert.equal(ea.eventoId,eb.eventoId);assert.equal(ea.nivelRiesgo,'riesgo');assert.equal(ea.origenDatos,'simulado');
  assert.equal((await getDocFromServer(doc(a.db,'comandos',id))).data().estado,'ejecutado');
  assert.equal((await getDocFromServer(doc(c.db,'eventos',eventId))).data().eventoId,eventId);
  await assert.rejects(getDocFromServer(doc(c.db,'lecturas',eventId)),e=>e.code==='permission-denied');
  await disableNetwork(b.db);await enableNetwork(b.db);assert.equal((await getDocFromServer(doc(b.db,'eventos',eventId))).data().eventoId,eventId);
  const counts={};for(const name of ['usuarios','dispositivos','zonas','lecturas','eventos','auditoria','comandos'])counts[name]=(await getDocs(collection(a.db,name))).size;
  for(const count of Object.values(counts))assert.ok(count>0);
  await Promise.all([a,b,c].map(async x=>{await signOut(x.auth);assert.equal(x.auth.currentUser,null);}));
  await writeFile(new URL('../../docs/evidencias/firebase-cloud.json',import.meta.url),JSON.stringify({executedAt:new Date().toISOString(),project:secrets.project,result:'passed',eventId,commandId:id,collections:counts,checks:['login valido/invalido','operador sin permiso de comando','consulta lee evento, no lectura','comando ejecutado por dispositivo','dos clientes SDK reciben evento remoto','reconexion y lectura servidor','logout'],twoAndroidVerified:false,physicalIoTVerified:false},null,2)+'\n');
  console.log('Nube aprobada; siete colecciones con datos. Evidencia sin credenciales guardada.');
 }finally{cleanup.forEach(fn=>fn());await Promise.all(clients.map(deleteApp));}
});
