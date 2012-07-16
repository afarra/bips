#include "images.h"
#include <SoftwareSerial.h>

#define FACES 6

// Constant Decalaraion
const short DBG_LED_PIN = 13;
const short LASER0_PIN = 5;
const short LASER1_PIN = 6;
const short LASER2_PIN = 7;
const short LASER3_PIN = 8;
const short LASER4_PIN = 9;
const short LASER5_PIN = 10;
const short LASER6_PIN = 11;
const short LASER7_PIN = 12;
const short IR_PIN = 2;

// Globals used in ISR
volatile unsigned long rotation_period = 0; // Ranges from 50k - 75k - 100k microseconds (face time: 8k - 12.5k - 16k)
volatile unsigned long last_trigger = 0;
volatile boolean triggered = false;

// Calibration globals
double duration_on_us = 6.20;
double duration_off_us = 0.80;
boolean face_toggle[] = {true, true, true, true, true, true};
double face_offset[] = {-112.00, -112.80, -123.96, -109.28, -113.44, -111.68};
// HIGH SPEED (50k us) {-122.00, -122.80, -133.96, -119.28, -123.44, -121.68};
// NORM SPEED (75k us) {-112.00, -112.80, -123.96, -109.28, -113.44, -111.68};
// LOW SPEED (100k us) {-102.00, -102.80, -113.96, -99.28, -103.44, -101.68};

// Debugging globals
boolean laser_on_override = false;
boolean ir_debug = false;
boolean dbg_led_state = false;
int selected_face = 6;
unsigned int num_rotations = 0;

// Serial setup for bluetooth
SoftwareSerial mySerial(3, 4); //rx, tx
byte image_buff[PAT_COLS] = {
  B01110111,
  B11111000,
  B10001000,
  B10001000,
  B10001000,
  B10001000,
  B11111000,
  B01110000,
  B00000000,
  B00000000,
  B00000000,
  B11111000,
  B11111000,
  B01000000,
  B01000000,
  B00100000,
  B00100000,
  B00010000,
  B11111000,
  B11111000,
};
short receive_byte_count = 0;
unsigned long image_time = (unsigned long) -1;
unsigned int image_time_buffer = 0;

// Board Setup
void setup(){
  pinMode(LASER0_PIN, OUTPUT);
  pinMode(LASER1_PIN, OUTPUT);
  pinMode(LASER2_PIN, OUTPUT);
  pinMode(LASER3_PIN, OUTPUT);
  pinMode(LASER4_PIN, OUTPUT);
  pinMode(DBG_LED_PIN, OUTPUT);
  attachInterrupt(0, handle_ir, FALLING);
  Serial.begin(9600);
  // set the data rate for the SoftwareSerial port
  mySerial.begin(9600);
}

// Infrared Sensor ISR
void handle_ir(){
  rotation_period = micros() - last_trigger;
  last_trigger = micros();
  triggered = true;
  // Debugging: (toggle debugging LED once per rotation)
  if (ir_debug){
    dbg_led_state = !dbg_led_state;
    digitalWrite(DBG_LED_PIN, dbg_led_state);
  }
}

// Output an array of pixels.
void output_pixel(const int pattern[PAT_ROWS][PAT_COLS], int rows, int cols, unsigned long period){
  unsigned long delay_off = duration_off_us * period / 1000;
  unsigned long delay_on = duration_on_us * period / 1000;
  
  if ((delay_off + delay_on) * cols > period) {
    return;
  }
  
  for (int col = cols-1; col >= 0; col--) {
    if (col != cols-1) {
      delayMicroseconds(delay_off);
    }
    digitalWrite(LASER0_PIN, pattern[0][col]);
    digitalWrite(LASER1_PIN, pattern[1][col]);
    digitalWrite(LASER2_PIN, pattern[2][col]);
    digitalWrite(LASER3_PIN, pattern[3][col]);
    digitalWrite(LASER4_PIN, pattern[4][col]);
    delayMicroseconds(delay_on);
    digitalWrite(LASER0_PIN, LOW);
    digitalWrite(LASER1_PIN, LOW);
    digitalWrite(LASER2_PIN, LOW);
    digitalWrite(LASER3_PIN, LOW);
    digitalWrite(LASER4_PIN, LOW);
  }
}

// Output an array of pixels from a BIPS image format
// (first four bytes are the time, then the next 20 bytes are the columns)
void output_pixel_bips(const byte pattern[PAT_COLS], int rows, int cols, unsigned long period){
  unsigned long delay_off = duration_off_us * period / 1000;
  unsigned long delay_on = duration_on_us * period / 1000;
  
  if ((delay_off + delay_on) * cols > period) {
    return;
  }
  
  for (int col = cols-1; col >= 0; col--) {
    if (col != cols-1) {
      delayMicroseconds(delay_off);
    }
    digitalWrite(LASER0_PIN, pattern[col] & 128);	// binary 10000000
    digitalWrite(LASER1_PIN, pattern[col] & 64);	// binary 01000000
    digitalWrite(LASER2_PIN, pattern[col] & 32);	// binary 00100000
    digitalWrite(LASER3_PIN, pattern[col] & 16);
    digitalWrite(LASER4_PIN, pattern[col] & 8);
    digitalWrite(LASER5_PIN, pattern[col] & 4);
    digitalWrite(LASER6_PIN, pattern[col] & 2);
    digitalWrite(LASER7_PIN, pattern[col] & 1);
    delayMicroseconds(delay_on);
    digitalWrite(LASER0_PIN, LOW);
    digitalWrite(LASER1_PIN, LOW);
    digitalWrite(LASER2_PIN, LOW);
    digitalWrite(LASER3_PIN, LOW);
    digitalWrite(LASER4_PIN, LOW);
    digitalWrite(LASER5_PIN, LOW);
    digitalWrite(LASER6_PIN, LOW);
    digitalWrite(LASER7_PIN, LOW);
  }
}

void perf_secondary_tasks(int task_num){
  if (task_num == 0 || task_num == 2 || task_num == 4){
    while (mySerial.available() > 0) {
      byte read_data = mySerial.read();
      Serial.print(read_data);
      Serial.print("\t");
      if (receive_byte_count < PAT_COLS){
        image_buff[receive_byte_count] = read_data;
      }
      else
      {
        image_time_buffer = image_time_buffer << 8;
        image_time_buffer += read_data;
      }
      
      receive_byte_count++;
      
      if (receive_byte_count >= PAT_COLS + 3)
      {
        Serial.print("\n");
        Serial.print(image_time_buffer);
        Serial.print("\n");
        image_time = image_time_buffer + millis();
        image_time_buffer = 0;
        receive_byte_count = 0;
      }
    }
  }
  
  // perform tilt check
  if (task_num == 5)
  { 
  }

}

// Main loop
void loop(){
  // Handle diag commands that were sent over the serial interface
  handle_diag_cmd();
  
  if (triggered && micros() > last_trigger && !laser_on_override){
    triggered = false;
    num_rotations++;
    
    // calc period for each face and time in us when the next face will come up
    unsigned long face_period = rotation_period / FACES;
    
//    long times[8];
//    times[0] = micros();

    // project the pixel on every single face
    unsigned long last_trig = last_trigger;
    for (int i = 0; i < FACES; i++){
      // debug
      if (i != selected_face && selected_face != 6){
        continue; 
      }
      unsigned long next_face = last_trig + (face_period * (i + 1)) + (face_period * face_offset[i] / 1000);
//      times[i+1] = next_face - micros();
      
      // Perform other tasks beetween faces
      perf_secondary_tasks(i);
      
      while (micros() < next_face){
      }
      
      // skip this face if the task took to long (TODO)
      //if (micros() > next_face + rotation_period / FACES / 10000){
      //  continue; 
      //}
      
      if (face_toggle[i]) {
        // output blank if the projection time has elapsed
        // if (image_time < millis())
        // {
          // output_pixel_bips(pat_byte_empty, PAT_ROWS, PAT_COLS, face_period);
        // }
        // else
        {
          output_pixel_bips(image_buff, PAT_ROWS, PAT_COLS, face_period);
        }
      }
    }
//    times[7] = micros();
//    if (num_rotations % 50 == 0) {
//      Serial.println("#####################################");
//      for (int i = 0; i < 8; i++)
//        Serial.println(times[i]);
//    }
  }
  else if (laser_on_override){
    digitalWrite(LASER0_PIN, HIGH);
    digitalWrite(LASER1_PIN, HIGH);
    digitalWrite(LASER2_PIN, HIGH);
    digitalWrite(LASER3_PIN, HIGH);
    digitalWrite(LASER4_PIN, HIGH);
  }
}


/***************************************************************
 * DEBUGGING STUFF
 ***************************************************************/

void handle_diag_cmd() {
  while (Serial.available() > 0) {
    byte b = Serial.read();
    if (b == '/') {
      Serial.print("T-rot: ");
      Serial.print(rotation_period);
      Serial.print("; T-face: ");
      Serial.print(rotation_period/FACES);
      Serial.print("; rot#: ");
      Serial.println(num_rotations);
    }
    else if (b == '?') {
      Serial.print("ON time: ");
      Serial.println(duration_on_us);
      Serial.print("OFF time: ");
      Serial.println(duration_off_us);
      Serial.print("Face offset: ");
      for (int i = 0; i < FACES; i++) {
        Serial.print(face_offset[i]);
        Serial.print(", ");
      }
      Serial.print("\n");
    }
    else if (b == '=') {
      Serial.print("Increasing ON duration.\n");
      duration_on_us += 0.1;
      Serial.println(duration_on_us);
    }
    else if (b == '-') {
      Serial.print("Decreasing ON duration.\n");
      if (duration_on_us != 0){
        duration_on_us -= 0.1;
        Serial.println(duration_on_us);
      }
    }
    else if (b == '+') {
      Serial.print("Increasing OFF duration.\n");
      duration_off_us += 0.1;
      Serial.println(duration_off_us);
    }
    else if (b == '-') {
      Serial.print("Decreasing OFF duration.\n");
      if (duration_off_us != 0){
        duration_off_us -= 0.1;
        Serial.println(duration_off_us);
      }
    }
    else if (b == ';') {
      Serial.print("Stopping laser ON override.\n");
      laser_on_override = false;
      digitalWrite(LASER0_PIN, LOW);
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
    
    //  ~~~~~~~~~~~~~~~~~~~~~~~   FACE SPECIFIC   ~~~~~~~~~~~~~~~~~~~~~~~
    else if (b >= '1' && b <= '7') {
      selected_face = b-49;
      Serial.print("Selected face #");
      Serial.println(selected_face + 1);
    }
    else if (b == 't') {
      face_toggle[selected_face] = !face_toggle[selected_face];
      Serial.println("Face Toggled");
      for (int i = 0; i < FACES; i++) {
        Serial.print(face_toggle[i]);
        Serial.print(", ");
      }
      Serial.print("\n");
    }
    else if (b == '[') {
      Serial.print("Increasing Offset for face #");
      Serial.println(selected_face+1);
      face_offset[selected_face] += 0.5;
      Serial.println(face_offset[selected_face]);
    }
    else if (b == ']') {
      Serial.print("Decreasing Offset for face #");
      Serial.println(selected_face+1);                            
      face_offset[selected_face] -= 0.5;
      Serial.println(face_offset[selected_face]);
    }
    else if (b == ',') {
      Serial.println("Increasing Offset for all faces ");
      for (int i = 0; i < FACES; i++) {
        face_offset[i] += 10.0;
        Serial.print(face_offset[i]);
        Serial.print(", ");
      }
      Serial.print("\n");
    }
    else if (b == '.') {
      Serial.println("Decreasing Offset for all faces ");
      for (int i = 0; i < FACES; i++) {
        face_offset[i] -= 10.0;
        Serial.print(face_offset[i]);
        Serial.print(", ");
      }
      Serial.print("\n");
    }
  }
}
