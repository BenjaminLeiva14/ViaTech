// The JS process models ESP32 local detection. It is NOT firmware or a Wokwi execution.
import {createRequire} from 'node:module';
import {mkdir,readFile,writeFile,rename} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {measurement} from './domain.mjs';
const require=createRequire(new URL('../firebase/package.json',import.meta.url));
const {initializeApp}=require('firebase/app');
const {getAuth,connectAuthEmulator,signInWithEmailAndPassword}=require('firebase/auth');
const {getFirestore,connectFirestoreEmulator,doc,getDoc,setDoc,updateDoc,writeBatch,collection,query,where,getDocs,serverTimestamp,Timestamp,disableNetwork,enableNetwork}=require('firebase/firestore');
const cloud=process.argv.includes('--cloud');
const projectId=cloud?process.env.VIATECH_PROJECT_ID:'demo-viatech';
if(cloud && (!projectId || !process.env.VIATECH_API_KEY || !process.env.VIATECH_DEVICE_EMAIL || !process.env.VIATECH_DEVICE_PASSWORD)) throw Error('Falta configuración privada del dispositivo');
const app=initializeApp({projectId,apiKey:cloud?process.env.VIATECH_API_KEY:'demo-api-key'});
const auth=getAuth(app),db=getFirestore(app);
if(!cloud){connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});connectFirestoreEmulator(db,'127.0.0.1',8080);}
const dir=fileURLToPath(new URL('./.state/',import.meta.url));
await mkdir(dir,{recursive:true});
const path=dir+(cloud?'cloud-':'local-')+'outbox.json';
let state;
try {state=JSON.parse(await readFile(path,'utf8'));} catch(e){if(e.code!=='ENOENT')throw e;state={queue:[],led:false};}
let diskWrites=Promise.resolve();
function persist(){ const snapshot=JSON.stringify(state,null,2); diskWrites=diskWrites.then(async()=>{await writeFile(path+'.tmp',snapshot);await rename(path+'.tmp',path);}); return diskWrites; }
const device=process.env.VIATECH_DEVICE_ID||'esp32-demo';
let zone='paso-cebra',offline=false,busy=false,stopping=false,failures=0,nextAttempt=0;
function enqueue(distance,motion,id=crypto.randomUUID()){
 if(state.queue.length>=500) throw Error('Cola llena (500): se conservan eventos existentes; sincroniza antes de continuar.');
 const event=measurement(distance,motion,device,zone,id);
 state.queue.push(event);return event;
}
async function publish(event){
 const ref=doc(db,'eventos',event.eventoId);
 // A stable event ID makes retries idempotent even if the acknowledgement was lost.
 const payload={...event,detectadoEn:Timestamp.fromMillis(event.detectadoEn),fechaHora:serverTimestamp()};
 const batch=writeBatch(db);
 batch.set(ref,payload);batch.set(doc(db,'lecturas',event.eventoId),payload);
 try { await batch.commit(); } catch(error) {
  const existing=await getDoc(ref);
  if(!existing.exists() || existing.data().dispositivoId!==device || existing.data().detectadoEn.toMillis()!==event.detectadoEn) throw error;
 }
}
async function cycle(){
 if(busy||offline||stopping||failures>=5||Date.now()<nextAttempt)return;busy=true;
 try{
  const info=await getDoc(doc(db,'dispositivos',device));
  if(!info.exists()||!info.data().activo)throw Error('Dispositivo no autorizado');
  zone=info.data().zonaId;
  while(state.queue.length){await publish(state.queue[0]);state.queue.shift();await persist();}
  await updateDoc(doc(db,'dispositivos',device),{ultimaComunicacion:serverTimestamp(),led:state.led,versionFirmware:'sim-1.0'});
  const pending=await getDocs(query(collection(db,'comandos'),where('dispositivoId','==',device),where('estado','==','pendiente')));
  for(const command of pending.docs){
   const c=command.data();
   if(Date.now()-c.creacion.toMillis()>120000){
    await updateDoc(command.ref,{estado:'error',ejecucion:serverTimestamp(),resultado:'Comando expirado después de 120 segundos.'});continue;
   }
   if(c.tipo==='led_on'||c.tipo==='led_off'){state.led=c.tipo==='led_on';await persist();}
   else {
    const id='cmd-'+command.id;
    const event=state.queue.find(e=>e.eventoId===id)||measurement(c.distanciaCm,c.movimientoDetectado,device,zone,id,c.creacion.toMillis());
    if(!state.queue.some(e=>e.eventoId===id)){if(state.queue.length>=500)throw Error('Cola llena');state.queue.push(event);await persist();}
    await publish(event);state.queue=state.queue.filter(e=>e.eventoId!==id);await persist();
   }
   const batch=writeBatch(db);
   batch.update(doc(db,'dispositivos',device),{ultimaComunicacion:serverTimestamp(),led:state.led});
   batch.update(command.ref,{estado:'ejecutado',ejecucion:serverTimestamp(),resultado:'Ejecutado por simulador teórico ESP32; sin actuador real.'});
   await batch.commit();
  }
 failures=0;
 }catch(e){failures++;nextAttempt=Date.now()+Math.min(60000,1000*2**failures);console.error('Sin confirmación; cola conservada. Intento',failures,'de 5. Código:',e.code||e.message);}
 finally{busy=false;}
}
await signInWithEmailAndPassword(auth,cloud?process.env.VIATECH_DEVICE_EMAIL:'esp32@viatech.test',cloud?process.env.VIATECH_DEVICE_PASSWORD:'ViaTechLocal2026!');
if(process.argv.includes('--once')){
 enqueue(35,true);await persist();await cycle();
 if(state.queue.length) process.exitCode=1;
 await auth.signOut();process.exit(process.exitCode||0);
}
console.log('Simulador teórico conectado. Comandos consola: lectura 35 true | offline | online | salir');
await cycle();
const timer=setInterval(cycle,15000);
const {createInterface}=await import('node:readline');
const input=createInterface({input:process.stdin,output:process.stdout});
let inputChain=Promise.resolve();
input.on('line',line=>{inputChain=inputChain.then(async()=>{
 const [cmd,value,motion]=line.trim().split(/\s+/);
 if(cmd==='offline'){offline=true;await disableNetwork(db);console.log('Detección local activa; red pausada.');}
 else if(cmd==='online'){failures=0;nextAttempt=0;await enableNetwork(db);offline=false;await cycle();}
 else if(cmd==='lectura'){enqueue(Number(value),motion==='true');await persist();console.log('Evento guardado localmente.');await cycle();}
 else if(cmd==='salir'){stopping=true;clearInterval(timer);await persist();input.close();process.exit(0);}
}).catch(e=>console.error(e.message));});
process.on('SIGINT',async()=>{stopping=true;clearInterval(timer);await persist();process.exit(0);});
