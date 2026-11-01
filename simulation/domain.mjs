export function measurement(distance, motion, device='esp32-demo', zone='paso-cebra', id=crypto.randomUUID(), detected=Date.now()) {
 if(typeof motion !== 'boolean') throw Error('PIR inválido');
 const valid=typeof distance==='number' && Number.isFinite(distance) && distance>=2 && distance<=400;
 const risk=!valid?'informacion':distance<50&&motion?'riesgo':distance<100?'advertencia':'informacion';
 return {eventoId:id,dispositivoId:device,zonaId:zone,tipoEvento:!valid?'lectura_invalida':distance<100?'aproximacion_peligrosa':motion?'presencia_peatonal':'sin_riesgo',
 sensorOrigen:'PIR+HC-SR04',distanciaCm:valid?distance:null,movimientoDetectado:motion,nivelRiesgo:risk,
 detectadoEn:detected,estadoSincronizacion:'sincronizado',versionFirmware:'sim-1.0',lecturaValida:valid,origenDatos:'simulado',
 descripcion:!valid?'Lectura fuera del rango de la maqueta.':'Indicio simulado de presencia y proximidad; no constituye una infracción confirmada.'};
}
