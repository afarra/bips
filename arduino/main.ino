#define PAT_ROWS 4
#define PAT_COLS 10
#define FACES 4

/*
 * Constant Decalaraion
 */
const short DBG_LED_PIN = 13;
const short LASER1_PIN = 9;
const short LASER2_PIN = 10;
const short LASER3_PIN = 11;
const short LASER4_PIN = 12;
const short IR_PIN = 2;

int selected_face = 0;

int offset_us = 14000;
int face_offset[] = {13500, 14300, 15600, 17000};

int num_rotations = 0;

const int pattern0[] = {0,0,0,0,0,0,0,0,0,0};
const int pattern1[] = {1,1,1,1,1,1,1,1,1,1};
const int pattern2[] = {1,0,1,0,1,0,1,0,1,0};
const int pattern3[] = {0,1,0,1,0,1,0,1,0,1};
const int pattern4[] = {1,1,0,1,1,0,1,1,0,1};

const int pat_all[PAT_ROWS][PAT_COLS] = {
{1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1}
};

const int pat_i[PAT_ROWS][PAT_COLS] = {
{0,0,1,1,1,1,1,1,0,0},
{0,0,0,0,1,1,0,0,0,0},
{0,0,0,0,1,1,0,0,0,0},
{0,0,1,1,1,1,1,1,0,0}
};

const int pat_c[PAT_ROWS][PAT_COLS] = {
{0,0,1,1,1,1,0,0,0,0},
{0,0,1,0,0,0,0,0,0,0},
{0,0,1,0,0,0,0,0,0,0},
{0,0,1,1,1,1,0,0,0,0}
};

const int pat_o[PAT_ROWS][PAT_COLS] = {
{0,0,1,1,1,1,1,0,0,0},
{0,0,1,0,0,0,1,0,0,0},
{0,0,1,0,0,0,1,0,0,0},
{0,0,1,1,1,1,1,0,0,0}
};

const int pat_l[PAT_ROWS][PAT_COLS] = {
{0,0,0,0,0,1,0,0,0,0},
{0,0,0,0,0,1,0,0,0,0},
{0,0,0,0,0,1,0,0,0,0},
{0,0,0,0,0,1,1,1,1,0}
};

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
unsigned int duration_off_us = 80;
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
  pinMode(DBG_LED_PIN, OUTPUT);
  attachInterrupt(0, handle_ir, FALLING);
  Serial.begin(9600);
}

/*
 * Infrared Sensor ISR
 */ 
void handle_ir(){
  // update microseconds per rotation and time of the last trigger
  // also, indicate that we got a trigger.
  //delayMicroseconds(offset_us);
  us_per_rotation = micros() - last_trigger;
  last_trigger = micros();
//  Serial.print("Handling IR\n");
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
void output_pixel(const int pattern[PAT_ROWS][PAT_COLS], int rows, int cols){
  for (int col = cols-1; col >= 0; col--) {
    if (col != cols-1) {
      delayMicroseconds(duration_off_us);
    }
    digitalWrite(LASER1_PIN, pattern[0][col]);
    digitalWrite(LASER2_PIN, pattern[1][col]);
    digitalWrite(LASER3_PIN, pattern[2][col]);
    digitalWrite(LASER4_PIN, pattern[3][col]);
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
  if (triggered && micros() > last_trigger && !laser_on_override){
    triggered = false;
    num_rotations++;
    // calc period for each face and time in us when the next face will come up
    unsigned long face_period = us_per_rotation / 4;
    // project the pixel on every single face
    unsigned long last_trig = last_trigger;
    for (int i = 0; i < num_faces; i++){
      unsigned long next_face = last_trig + (face_period * (i + 1)) + face_offset[i];
      while (micros() < next_face){
      }
//      Serial.print("Starting Face#: ");
//      Serial.print(i);
//      Serial.print("\n");
      if (i==3)
        output_pixel(pat_c, PAT_ROWS, PAT_COLS);
      if (i==2 || i==1)
        output_pixel(pat_o, PAT_ROWS, PAT_COLS);
      if (i==0)
        output_pixel(pat_l, PAT_ROWS, PAT_COLS);
      if (i == FACES){
        break; 
      }
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
      Serial.print("; total rotations: ");
      Serial.print(num_rotations);
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
    else if (b == 'q') {
      Serial.print("Increasing OFF duration.\n");
      duration_off_us += 10;
      Serial.print(duration_off_us);
      Serial.print("\n");
    }
    else if (b == 'a') {
      Serial.print("Decreasing OFF duration.\n");
      if (duration_off_us != 0){
        duration_off_us -= 10;
        Serial.print(duration_off_us);
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
    else if (b == '1') {
      Serial.println("Selected face #1");
      selected_face = 0;
    }
    else if (b == '2') {
      Serial.println("Selected face #2");
      selected_face = 1;
    }
    else if (b == '3') {
      Serial.println("Selected face #3");
      selected_face = 2;
    }
    else if (b == '4') {
      Serial.println("Selected face #4");
      selected_face = 3;
    }
    else if (b == '=') {
      Serial.print("Increasing Offset for face #");
      Serial.print(selected_face+1);
      Serial.print("\n");
      face_offset[selected_face] += 100;
      Serial.print(face_offset[selected_face]);
      Serial.print("\n");
    }
    else if (b == '-') {
      Serial.print("Decreasing Offset for face #");
      Serial.print(selected_face+1);
      Serial.print("\n");
      face_offset[selected_face] -= 100;
      Serial.print(face_offset[selected_face]);
      Serial.print("\n");
    }
  }
}
