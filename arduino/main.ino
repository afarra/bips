int val;
int laserPin = 13;
int encoder0PinA = 11;
int encoder0PinB = 9;
int encoder0Pos = 0;
int encoder0PinALast = LOW;
int n = LOW;
int r = 0;
int ticks = 127;
int divs = 32;
int time_taken = 0;
int last_rotation = 0;
int decrement_freq = 2; // how many rotations before decrementing 1 from encoder0Pos

void setup() {
  pinMode (laserPin,OUTPUT);
  pinMode (encoder0PinA,INPUT);
  pinMode (encoder0PinB,INPUT);
  digitalWrite(laserPin, HIGH);
  Serial.begin (9600);
}

void loop() { 
  n = digitalRead(encoder0PinA);
  if ((encoder0PinALast == LOW) && (n == HIGH)) {
    if (digitalRead(encoder0PinB) == LOW) {
      encoder0Pos--;
    } else {
      encoder0Pos++;
    }
    //Serial.print (encoder0Pos);
    //Serial.print ("\n");
    if (encoder0Pos == ticks) {
      r--;
      fullRotation();
    }
    else if (encoder0Pos == -ticks) {
      r++;
      fullRotation();
    }
  }
  encoder0PinALast = n;
  static long laser_on = 0;
  static long last_turn_on = micros();
  long now = micros();
  int rem = (-encoder0Pos) % divs;
  if (rem == 0) {
    if (laser_on == 0) {
      last_turn_on = now;
      laser_on = 1;
      digitalWrite(laserPin, HIGH);
      //Serial.println("LASER ON");
    }
  } else if (laser_on == 1 && (now - last_turn_on > 30)) {
      laser_on = 0;
      digitalWrite(laserPin, LOW);
      last_turn_on = now;
      //Serial.println("LASER OFF");
  }
  handleCmd();
}

void fullRotation() {
  int now = millis();
  time_taken = now - last_rotation;
  last_rotation = now;
  encoder0Pos = 0;
  if (r % decrement_freq == 0) {
    encoder0Pos += 2;
  }
}

void handleCmd() {
 while (Serial.available() > 0) {
   byte b = Serial.read();
   if (b == 'r') {
    Serial.print ("r=");
    Serial.print (r);
    Serial.print (", time=");
    Serial.print (time_taken);
    Serial.print ("\n");
   }
   else if (b == 'w') {
     ticks += 1;
     Serial.println(ticks);
   }
   else if (b == 'q') {
     decrement_freq += 1;
     Serial.println(decrement_freq);
   }
   else if (b == 's') {
     ticks -= 1;
     Serial.println(ticks);
   }
   else if (b == 'a') {
     decrement_freq -= 1;
     Serial.println(decrement_freq);
   }
 }
}
