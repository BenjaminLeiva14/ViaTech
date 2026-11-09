// Verificación explícita de un evento generado por la prueba Python.
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
const require=createRequire(new URL('../firebase/package.json',import.meta.url));
const {initializeApp,deleteApp}=require('firebase/app');
const {getAuth,signInWithEmailAndPassword,signOut}=require('firebase/auth');
const {getFirestore,collection,query,orderBy,limit,getDocsFromServer}=require('firebase/firestore');
const sdk=JSON.parse(await readFile(new URL('../android/app/google-services.json',import.meta.url),'utf8'));
const privateData=JSON.parse(await readFile(new URL('../firebase/.env.cloud-demo.json',import.meta.url),'utf8'));
if(sdk.project_info.project_id!=='viatech-inacap-bleiva-2026'||privateData.project!==sdk.project_info.project_id)throw Error('Proyecto no autorizado');
const account=privateData.accounts.find(a=>a.rol==='operador');
const app=initializeApp({projectId:privateData.project,apiKey:sdk.client[0].api_key[0].current_key});
const auth=getAuth(app),db=getFirestore(app);
try{
 await signInWithEmailAndPassword(auth,account.email,account.password);
 const started=Number(process.argv[2]),distance=Number(process.argv[3]);
 if(!Number.isFinite(started)||!Number.isFinite(distance))throw Error('Faltan parametros de prueba');
 let found;
 for(let i=0;i<20;i++){
  const snapshot=await getDocsFromServer(query(collection(db,'eventos'),orderBy('fechaHora','desc'),limit(20)));
  found=snapshot.docs.map(d=>d.data()).find(d=>d.dispositivoId==='esp32-demo'&&d.distanciaCm===distance&&d.movimientoDetectado===true&&d.origenDatos==='simulado'&&d.detectadoEn.toMillis()>=started);
  if(found)break;
  await new Promise(r=>setTimeout(r,1500));
 }
 if(!found)throw Error('No se encontro el evento de esta ejecucion');
 console.log(JSON.stringify({eventoId:found.eventoId,distanciaCm:found.distanciaCm,movimientoDetectado:found.movimientoDetectado,origenDatos:found.origenDatos,confirmadoServidor:true}));
 await signOut(auth);
}finally{await deleteApp(app);}
