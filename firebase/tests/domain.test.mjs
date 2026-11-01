import test from 'node:test';
import assert from 'node:assert/strict';
import {measurement} from '../../simulation/domain.mjs';
test('riesgo: movimiento y distancia menor a 50',()=>{assert.equal(measurement(35,true).nivelRiesgo,'riesgo');assert.equal(measurement(35,false).nivelRiesgo,'advertencia');});
test('límites e inválidos',()=>{for(const d of [NaN,Infinity,-1,0,401]){const e=measurement(d,true);assert.equal(e.lecturaValida,false);assert.equal(e.distanciaCm,null);assert.equal(e.nivelRiesgo,'informacion');}for(const d of [2,400])assert.equal(measurement(d,false).lecturaValida,true);});
test('IDs y detección preservados para reintento',()=>{assert.deepEqual(measurement(30,true,'d','z','fixed',123),measurement(30,true,'d','z','fixed',123));});
test('PIR rechaza tipo incorrecto',()=>assert.throws(()=>measurement(30,'true')));
