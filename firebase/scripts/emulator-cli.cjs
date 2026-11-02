// Slow Windows disks may exceed the CLI's fixed 60-second Java startup timeout.
// Only the local emulator port wait is extended. Rules and authentication are unchanged.
const ports=require('firebase-tools/lib/emulator/portUtils.js');
const wait=ports.waitForPortUsed;
ports.waitForPortUsed=(port,host,timeout)=>wait(port,host,Math.max(timeout||0,300000));
process.argv=[process.argv[0],require.resolve('firebase-tools/lib/bin/firebase.js'),...process.argv.slice(2)];
require('firebase-tools/lib/bin/firebase.js');
