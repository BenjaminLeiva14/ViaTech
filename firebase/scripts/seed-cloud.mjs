// Trusted workstation provisioning only. Never packaged in the Android app.
import {createRequire} from 'node:module';
import {readFile,writeFile} from 'node:fs/promises';
import {randomBytes} from 'node:crypto';
const require=createRequire(import.meta.url);
const {command}=require('firebase-tools/lib/commands/projects-addfirebase');
const {requireAuth}=require('firebase-tools/lib/requireAuth');
const {Client}=require('firebase-tools/lib/apiv2');
const project='viatech-inacap-bleiva-2026';
const sdk=JSON.parse(await readFile(new URL('../../android/app/google-services.json',import.meta.url),'utf8'));
if(sdk.project_info.project_id!==project)throw Error('Proyecto distinto del autorizado');
const options={project,nonInteractive:true};
await command.prepare(options);await requireAuth(options);
const iam=new Client({urlPrefix:'https://identitytoolkit.googleapis.com',apiVersion:'v1'});
const firestore=new Client({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
const hidden={skipLog:{body:true,resBody:true,queryParams:true}};
const secretPath=new URL('../.env.cloud-demo.json',import.meta.url);
let credentials;
try{credentials=JSON.parse(await readFile(secretPath,'utf8'));}catch(e){
 if(e.code!=='ENOENT')throw e;
 const existing=await iam.post('projects/'+project+'/accounts:lookup',{localId:['admin-demo','operador-demo','consulta-demo','device-demo']},hidden);
 if(existing.body.users?.length)throw Error('Ya existen cuentas demo. Recupera el archivo privado original; este script no regenera sus contrasenas.');
 credentials={project,accounts:[
  ['admin-demo','admin@viatech.invalid','administrador'],
  ['operador-demo','operador@viatech.invalid','operador'],
  ['consulta-demo','consulta@viatech.invalid','consulta'],
  ['device-demo','esp32@viatech.invalid','dispositivo']
 ].map(([uid,email,rol])=>({uid,email,rol,password:randomBytes(24).toString('base64url')}))};
 await writeFile(secretPath,JSON.stringify(credentials,null,2)+'\n',{flag:'wx',mode:0o600});
}
if(credentials.project!==project)throw Error('Credenciales de otro proyecto');
function value(v){
 if(v===null)return{nullValue:null};
 if(Array.isArray(v))return{arrayValue:{values:v.map(value)}};
 if(v instanceof Date)return{timestampValue:v.toISOString()};
 if(typeof v==='boolean')return{booleanValue:v};
 if(typeof v==='number')return{doubleValue:v};
 return{stringValue:v};
}
async function createOnly(collection,id,data){
 const root='projects/'+project+'/databases/(default)/documents/'+collection;
 try{await firestore.get(root+'/'+id);return;}catch(e){if(e.status!==404&&e.context?.response?.statusCode!==404&&!e.message.includes('404'))throw e;}
 await firestore.post(root,{fields:Object.fromEntries(Object.entries(data).map(([k,v])=>[k,value(v)]))},{queryParams:{documentId:id}});
}
for(const account of credentials.accounts){
 const found=await iam.post('projects/'+project+'/accounts:lookup',{localId:[account.uid]},hidden);
 if(found.body.users?.length){
  if(found.body.users[0].email!==account.email)throw Error('UID existente pertenece a otra cuenta');
 }else{
  await iam.post('projects/'+project+'/accounts',{localId:account.uid,email:account.email,password:account.password,displayName:'ViaTech demo '+account.rol},hidden);
 }
 if(account.rol==='dispositivo')await iam.post('projects/'+project+'/accounts:update',{localId:account.uid,customAttributes:JSON.stringify({rol:'dispositivo',dispositivoId:'esp32-demo'})},hidden);
 else await createOnly('usuarios',account.uid,{uid:account.uid,rol:account.rol,activo:true,auditoriaId:'bootstrap-cloud'});
}
for(const [zonaId,nombre,tipo] of [
 ['paso-cebra','Paso de cebra','paso_cebra'],['acceso-peatonal','Acceso peatonal','acceso_peatonal'],
 ['zona-vehicular','Zona vehicular','zona_vehicular'],['lomo-toro','Lomo de toro','lomo_toro']
])await createOnly('zonas',zonaId,{zonaId,nombre,tipo,activo:true,sensores:['PIR','HC-SR04'],auditoriaId:'bootstrap-cloud'});
await createOnly('dispositivos','esp32-demo',{dispositivoId:'esp32-demo',nombre:'ESP32 Plaza · Simulado',zonaId:'paso-cebra',activo:true,versionFirmware:'sim-1.0',sensores:['PIR','HC-SR04'],ultimaComunicacion:null,led:false,origenDatos:'simulado',auditoriaId:'bootstrap-cloud'});
await createOnly('auditoria','bootstrap-cloud',{actor:'bootstrap-iam',accion:'bootstrap',destino:project,fechaHora:new Date()});
console.log('Bootstrap nube listo: 3 roles humanos, identidad dispositivo, 4 zonas, dispositivo y auditoria. Credenciales aleatorias solo en .env.cloud-demo.json (ignorado).');
