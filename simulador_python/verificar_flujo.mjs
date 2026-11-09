// Consultas equivalentes a MonitoringRepository.kt. No ejecuta Android.
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile,writeFile} from 'node:fs/promises';
const require=createRequire(new URL('../firebase/package.json',import.meta.url));
const {initializeApp,deleteApp}=require('firebase/app');
const {getAuth,signInWithEmailAndPassword}=require('firebase/auth');
const {getFirestore,collection,query,orderBy,limit,onSnapshot,doc,getDocFromServer}=require('firebase/firestore');
const apps=[],listeners=[];
let timer;
try {
 const [startedArg,casesPath,readyPath]=process.argv.slice(2);
 const started=Number(startedArg);
 assert.ok(Number.isFinite(started));
 const cases=JSON.parse(await readFile(casesPath,'utf8'));
 const sdk=JSON.parse(await readFile(new URL('../android/app/google-services.json',import.meta.url),'utf8'));
 const secrets=JSON.parse(await readFile(new URL('../firebase/.env.cloud-demo.json',import.meta.url),'utf8'));
 assert.equal(secrets.project,'viatech-inacap-bleiva-2026');
 assert.equal(sdk.project_info.project_id,secrets.project);
 const properties=await readFile(new URL('../firebase.local.properties',import.meta.url),'utf8');
 const prop=name=>properties.split(/\r?\n/).find(line=>line.startsWith(name+'='))?.slice(name.length+1).trim();
 assert.equal(prop('firebase.projectId'),secrets.project);
 const android=sdk.client.find(c=>c.client_info.android_client_info.package_name==='cl.amigovirtual.app');
 assert.equal(prop('firebase.appId'),android.client_info.mobilesdk_app_id);
 assert.equal(prop('firebase.apiKey'),android.api_key[0].current_key);
 const clients=[];
 for(const role of ['administrador','operador']) {
  const account=secrets.accounts.find(a=>a.rol===role);
  const app=initializeApp({projectId:secrets.project,apiKey:android.api_key[0].current_key},'flujo-'+role);
  apps.push(app);
  await signInWithEmailAndPassword(getAuth(app),account.email,account.password);
  const db=getFirestore(app);
  const profile=await getDocFromServer(doc(db,'usuarios',account.uid));
  assert.equal(profile.data().rol,role);
  assert.equal(profile.data().activo,true);
  clients.push({db,role});
 }
 const ready=new Set(),seen=new Map();
 let resolveDone,rejectDone;
 const done=new Promise((resolve,reject)=>{resolveDone=resolve;rejectDone=reject;});
 done.catch(()=>{});
 timer=setTimeout(()=>rejectDone(Error('Tiempo de espera agotado')),180000);
 for(const client of clients) for(const name of ['eventos','lecturas']) {
  const key=client.role+'/'+name;
  const received=new Map();seen.set(key,received);
  listeners.push(onSnapshot(query(collection(client.db,name),orderBy('fechaHora','desc'),limit(200)),
   {includeMetadataChanges:true},snapshot=>{
    try {
     if(snapshot.metadata.fromCache||snapshot.metadata.hasPendingWrites)return;
     ready.add(key);
     if(ready.size===4) {
      ready.add('ready-written');
      writeFile(readyPath,JSON.stringify({listo:true})).catch(rejectDone);
     }
     for(const item of snapshot.docs) {
      const d=item.data();
      if(d.dispositivoId!=='esp32-demo'||d.origenDatos!=='simulado'||!d.detectadoEn||d.detectadoEn.toMillis()<started)continue;
      const index=cases.findIndex(c=>d.distanciaCm===c.distanciaCm&&d.movimientoDetectado===c.movimientoDetectado);
      if(index<0)continue;
      assert.equal(d.eventoId,item.id);
      assert.equal(d.nivelRiesgo,cases[index].nivelRiesgo);
      assert.equal(d.lecturaValida,true);
      assert.equal(d.sensorOrigen,'PIR+HC-SR04');
      assert.ok(d.fechaHora.toMillis()>0);
      received.set(index,item.id);
     }
     if(seen.size===4&&[...seen.values()].every(m=>m.size===cases.length))resolveDone();
    }catch(error){rejectDone(error);}
   },rejectDone));
 }
 await done;
 const events=[];
 for(let i=0;i<cases.length;i++) {
  const id=seen.get('operador/eventos').get(i);
  for(const received of seen.values())assert.equal(received.get(i),id);
  const event=await getDocFromServer(doc(clients[1].db,'eventos',id));
  const reading=await getDocFromServer(doc(clients[1].db,'lecturas',id));
  assert.deepEqual(event.data(),reading.data());
  events.push({...cases[i],eventoId:id,confirmadoServidor:true});
 }
 console.log(JSON.stringify({proyecto:secrets.project,configuracionCloudAndroidCoincide:true,
  receptoresSDK:['administrador','operador'],colecciones:['eventos','lecturas'],
  consulta:'orderBy(fechaHora, desc).limit(200)',recepcionEnTiempoReal:true,
  androidVerificado:false,eventos:events}));
} catch(error) {
 console.error('Prueba remota fallida:',error.code||error.name||'Error');
 process.exitCode=1;
} finally {
 clearTimeout(timer);
 listeners.forEach(unsubscribe=>unsubscribe());
 await Promise.all(apps.map(deleteApp));
}
