// Loads private local demo credentials without placing them in command arguments.
import {readFile} from 'node:fs/promises';
import {spawn} from 'node:child_process';
const sdk=JSON.parse(await readFile(new URL('../../android/app/google-services.json',import.meta.url),'utf8'));
const secrets=JSON.parse(await readFile(new URL('../.env.cloud-demo.json',import.meta.url),'utf8'));
if(secrets.project!==sdk.project_info.project_id || secrets.project!=='viatech-inacap-bleiva-2026')throw Error('Proyecto no autorizado');
const device=secrets.accounts.find(x=>x.rol==='dispositivo');
const child=spawn(process.execPath,[new URL('../../simulation/simulator.mjs',import.meta.url).pathname.replace(/^\/([A-Za-z]:)/,'$1'),'--cloud',...process.argv.slice(2)],{stdio:'inherit',env:{...process.env,VIATECH_PROJECT_ID:secrets.project,VIATECH_API_KEY:sdk.client[0].api_key[0].current_key,VIATECH_DEVICE_EMAIL:device.email,VIATECH_DEVICE_PASSWORD:device.password}});
child.on('error',e=>{console.error(e.message);process.exitCode=1;});
child.on('exit',code=>process.exitCode=code??1);
