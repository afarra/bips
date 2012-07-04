/*
 * Constant Decalaraion
 */
const short DBG_LED_PIN = 13;
const short LASER1_PIN = 12;
const short LASER2_PIN = 11;
const short LASER3_PIN = 10;
const short LASER4_PIN = 9;
const short IR_PIN = 2;

/*
 * Globals used in ISR
 */
volatile unsigned long us_per_rotation = 0;
volatile unsigned long last_trigger = 0;
volatile boolean triggered = false;

/*
 * Other Globals
 */
unsigned int duration_on_us = 80;
short num_faces = 4;
// Debugging globals
boolean laser_on_override = false;
boolean ir_debug = false;
boolean dbg_led_state = false;

/*
 * Board Setup
 */ 
void setup(){
  pinMode(LASER1_PIN, OUTPUT);
  pinMode(LASER2_PIN, OUTPUT);
  pinMode(LASER3_PIN, OUTPUT);
  pinMode(LASER4_PIN, OUTPUT);
  attachInterrupt(0, handle_ir, FALLING);
  Serial.begin(9600);
}

/*
 * Infrared Sensor ISR
 */ 
void handle_ir(){
  // update microseconds per rotation and time of the last trigger
  // also, indicate that we got a trigger.
  us_per_rotation = micros() - last_trigger;
  last_trigger = micros();
  triggered = true;
  // Debugging stuff:
  // toggle debugging LED once per rotation.
  if (ir_debug){
    if (dbg_led_state){
      digitalWrite(DBG_LED_PIN, HIGH);
    }
    else{
      digitalWrite(DBG_LED_PIN, LOW);
    }
    dbg_led_state = !dbg_led_state;
  }
}

/*
 * Output a single pixel.
 */ 
void output_pixel(int num_pix){
  for (int i = 0; i < num_pix; i++) {
    if (i != 0) {
      delayMicroseconds(duration_on_us);
    }
    digitalWrite(LASER1_PIN, HIGH);
    digitalWrite(LASER2_PIN, HIGH);
    digitalWrite(LASER3_PIN, HIGH);
    digitalWrite(LASER4_PIN, HIGH);
    delayMicroseconds(duration_on_us);
    digitalWrite(LASER1_PIN, LOW);
    digitalWrite(LASER2_PIN, LOW);
    digitalWrite(LASER3_PIN, LOW);
    digitalWrite(LASER4_PIN, LOW);
  }
}

/*
 * Main loop
 */ 
void loop(){
  // Handle diag commands that were sent over the serial interface
  handle_diag_cmd();
  if (triggered && !laser_on_override){
    triggered = false;
    // calc period for each face and time in us when the next face will come up
    unsigned long face_period = us_per_rotation / 4;
    unsigned long next_face = last_trigger + face_period;
    // project the pixel on every single face
    for (int i = 1; i <= num_faces; i++){
      output_pixel(i);
      if (i == 4){
        break; 
      }
      while (micros() < next_face){     
      }    
      next_face = last_trigger + (face_period * (i + 1));
    }
  }
  else if (laser_on_override){
    digitalWrite(LASER1_PIN, HIGH);
    digitalWrite(LASER2_PIN, HIGH);
    digitalWrite(LASER3_PIN, HIGH);
    digitalWrite(LASER4_PIN, HIGH);
  }
}


/***************************************************************
 * DEBUGGING STUFF
 ***************************************************************/


/*
 * Handle diag commands over Serial
 */  
void handle_diag_cmd() {
  while (Serial.available() > 0) {
    byte b = Serial.read();
    if (b == 'r') {
      Serial.print("Microseconds per rotation: ");
      Serial.print(us_per_rotation);
      Serial.print("\n");
    }
    else if (b == 'i') {
      Serial.print("Increasing ON duration.\n");
      duration_on_us += 10;
      Serial.print(duration_on_us);
      Serial.print("\n");
    }
    else if (b == 'd') {
      Serial.print("Decreasing ON duration.\n");
      if (duration_on_us != 0){
        duration_on_us -= 10;
        Serial.print(duration_on_us);
        Serial.print("\n");
      }
    }
    else if (b == '.') {
      Serial.print("Increasing number of faces.\n");
      num_faces++;
      Serial.print(num_faces);
      Serial.print("\n");
    }
    else if (b == ',') {
      Serial.print("Decreasing number of faces.\n");
      num_faces--;
      Serial.print(num_faces);
      Serial.print("\n");
    }
    else if (b == ';') {
      Serial.print("Stopping laser ON override.\n");
      laser_on_override = false;
    }
    else if (b == '\'') {
      Serial.print("Starting laser ON override.\n");
      laser_on_override = true;
    }
    else if (b == 'l') {
      Serial.print("Turning on debug LED.\n");
      ir_debug = true;
    }
    else if (b == 'k') {
      Serial.print("Turning off debug LED.\n");
      ir_debug = false;
    }
  }
}




