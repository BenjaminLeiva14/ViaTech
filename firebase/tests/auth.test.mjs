import test from 'node:test';
import assert from 'node:assert/strict';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,createUserWithEmailAndPassword,signInWithEmailAndPassword,sendPasswordResetEmail,signOut} from 'firebase/auth';
test('Authentication: registro, login inválido/válido, recuperación y cierre',async()=>{
 const app=initializeApp({apiKey:'demo-api-key',projectId:'demo-viatech'},'auth-test-'+Date.now());
 const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
 const email='test-'+Date.now()+'@viatech.test',password='LocalTest2026!';
 try{
  await createUserWithEmailAndPassword(auth,email,password);
  assert.ok(auth.currentUser);await signOut(auth);assert.equal(auth.currentUser,null);
  await assert.rejects(signInWithEmailAndPassword(auth,email,'Incorrecta2026!'));
  await signInWithEmailAndPassword(auth,email,password);assert.equal(auth.currentUser.email,email);
  await sendPasswordResetEmail(auth,email);
  const codes=await fetch('http://127.0.0.1:9099/emulator/v1/projects/demo-viatech/oobCodes').then(r=>r.json());
  assert.ok(codes.oobCodes.some(c=>c.email===email && c.requestType==='PASSWORD_RESET'));
  await signOut(auth);assert.equal(auth.currentUser,null);
 }finally{await deleteApp(app);}
});
