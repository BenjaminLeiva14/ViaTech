import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {spawn,execFile} from 'node:child_process';
import {promisify} from 'node:util';
import {fileURLToPath} from 'node:url';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,signInWithEmailAndPassword,signOut} from 'firebase/auth';
import {getFirestore,doc,getDocFromServer,collection,query,where,getDocs} from 'firebase/firestore';
const exec=promisify(execFile);
test('Cola nube: desconexion simulada, terminacion del proceso y recuperacion sin duplicar',{timeout:180000},async()=>{
 const sdk=JSON.parse(await readFile(new URL('../../android/app/google-services.json',import.meta.url),'utf8'));
 const secrets=JSON.parse(await readFile(new URL('../.env.cloud-demo.json',import.meta.url),'utf8'));
 assert.equal(secrets.project,'viatech-inacap-bleiva-2026');
 const device=secrets.accounts.find(x=>x.rol==='dispositivo'),admin=secrets.accounts.find(x=>x.rol==='administrador');
 const env={...process.env,VIATECH_PROJECT_ID:secrets.project,VIATECH_API_KEY:sdk.client[0].api_key[0].current_key,VIATECH_DEVICE_EMAIL:device.email,VIATECH_DEVICE_PASSWORD:device.password};
 const simulator=fileURLToPath(new URL('../../simulation/simulator.mjs',import.meta.url)),queue=new URL('../../simulation/.state/cloud-outbox.json',import.meta.url);
 const existing=JSON.parse(await readFile(queue,'utf8'));assert.equal(existing.queue.length,0,'No ejecutar sobre una cola con trabajo pendiente');
 const app=initializeApp({projectId:secrets.project,apiKey:env.VIATECH_API_KEY},'queue-verifier'),auth=getAuth(app),db=getFirestore(app);
 const child=spawn(process.execPath,[simulator,'--cloud'],{env,stdio:['pipe','pipe','pipe']});let output='';
 child.stdout.on('data',data=>output+=data.toString());child.stderr.on('data',()=>{});
 async function until(text){const end=Date.now()+60000;while(!output.includes(text)){if(child.exitCode!==null)throw Error('Simulador termino antes del punto esperado');if(Date.now()>end)throw Error('Timeout esperando '+text);await new Promise(r=>setTimeout(r,100));}}
 try{
  await signInWithEmailAndPassword(auth,admin.email,admin.password);
  await until('Simulador teórico conectado');child.stdin.write('offline\n');await until('red pausada');
  child.stdin.write('lectura 80 false\n');await until('Evento guardado localmente');
  const pending=JSON.parse(await readFile(queue,'utf8'));assert.equal(pending.queue.length,1);const id=pending.queue[0].eventoId;
  assert.equal((await getDocFromServer(doc(db,'eventos',id))).exists(),false);
  const exited=new Promise(resolve=>child.once('exit',resolve));child.kill('SIGKILL');await exited;
  assert.equal(JSON.parse(await readFile(queue,'utf8')).queue[0].eventoId,id);
  await exec(process.execPath,[simulator,'--cloud','--once'],{env,timeout:90000});
  const saved=await getDocFromServer(doc(db,'eventos',id));assert.equal(saved.data().distanciaCm,80);
  assert.equal((await getDocs(query(collection(db,'eventos'),where('eventoId','==',id)))).size,1);
  assert.equal(JSON.parse(await readFile(queue,'utf8')).queue.length,0);
  await writeFile(new URL('../../docs/evidencias/cola-reinicio.json',import.meta.url),JSON.stringify({executedAt:new Date().toISOString(),result:'passed',eventId:id,offlineMode:'SDK disableNetwork',processTerminated:true,persistentQueueRecovered:true,duplicateCount:0,physicalNetworkInterrupted:false},null,2)+'\n');
  await signOut(auth);
 }finally{if(child.exitCode===null)child.kill('SIGKILL');await deleteApp(app);}
});
