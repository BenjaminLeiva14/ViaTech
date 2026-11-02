// Maqueta local TEÓRICA. No envía datos a Firebase. El simulador JS realiza esa función.
#include <Arduino.h>
constexpr int PIR=27, TRIG=5, ECHO=18, LED=2;
void setup(){Serial.begin(115200);pinMode(PIR,INPUT);pinMode(TRIG,OUTPUT);pinMode(ECHO,INPUT);pinMode(LED,OUTPUT);}
void loop(){
 digitalWrite(TRIG,LOW);delayMicroseconds(2);digitalWrite(TRIG,HIGH);delayMicroseconds(10);digitalWrite(TRIG,LOW);
 unsigned long duration=pulseIn(ECHO,HIGH,25000);
 float cm=duration*0.0343f/2.0f;bool valid=duration>0 && cm>=2 && cm<=400;bool motion=digitalRead(PIR)==HIGH;
 digitalWrite(LED,valid && cm<50 && motion);
 Serial.printf("SIMULACION LOCAL PIR=%d distancia=%.1f valida=%d\n",motion,cm,valid);delay(1000);
}
