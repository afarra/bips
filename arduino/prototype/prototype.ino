#include <SoftwareSerial.h>

SoftwareSerial mySerial(2, 3); //rx, tx

// these constants won't change:
const int pin1 = 12;     // X output of the accelerometer
const int pin2 = 13;     // Y output of the accelerometer
void setup()  
{
  Serial.begin(9600);
  Serial.println("Goodnight moon!");

  // set the data rate for the SoftwareSerial port
  mySerial.begin(9600);
  mySerial.println("Hello, world?");
  // initialize the pins connected to the accelerometer
  // as inputs:
  pinMode(pin1, INPUT);
  pinMode(pin2, INPUT);

  
}

void loop() // run over and over
{
  if (mySerial.available()) {

    // say what you got:
    Serial.println(mySerial.read(), DEC);
  }
  if (Serial.available())
    mySerial.write(Serial.read());
    
  // variables to read the pulse widths:
  int p1, p2;
  p1 = digitalRead(pin1);
  p2 = digitalRead(pin2);
  
  printTilt(p1,p2);
}

void printTilt(int p1, int p2)
{
  // print the pins
  Serial.print(p1);
  Serial.print(p2);
  Serial.print(" --> ");
  if (p1 == 0 && p2 == 0)
    Serial.print("A");
  if (p1 == 1 && p2 == 0)
    Serial.print("B");
  if (p1 == 1 && p2 == 1)
    Serial.print("C");
  if (p1 == 0 && p2 == 1)
    Serial.print("D");
  Serial.println();
}

/*
   Memsic2125
   
   Read the Memsic 2125 two-axis accelerometer.  Converts the
   pulses output by the 2125 into milli-g's (1/1000 of earth's
   gravity) and prints them over the serial connection to the
   computer.
   
   The circuit:
    * X output of accelerometer to digital pin 2
    * Y output of accelerometer to digital pin 3
    * +V of accelerometer to +5V
    * GND of accelerometer to ground
 
   http://www.arduino.cc/en/Tutorial/Memsic2125
   
   created 6 Nov 2008
   by David A. Mellis
   modified 30 Aug 2011
   by Tom Igoe
   
   This example code is in the public domain.

 */

