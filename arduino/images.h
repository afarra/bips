#include "Arduino.h"
#define PAT_ROWS 5
#define PAT_COLS 20

const int pat_all[PAT_ROWS][PAT_COLS] = {
{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
};

const int pat_none[PAT_ROWS][PAT_COLS] = {
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
};

const int pat_alt[PAT_ROWS][PAT_COLS] = {
{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0},
{0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1},
{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0},
{0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1},
{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0},
};

const int pat_cool[PAT_ROWS][PAT_COLS] = {
{1,1,1,1,0,1,1,1,1,0,1,1,1,1,0,0,1,0,0,0},
{1,0,0,0,0,1,0,0,1,0,1,0,0,1,0,0,1,0,0,0},
{1,0,0,0,0,1,0,0,1,0,1,0,0,1,0,0,1,0,0,0},
{1,1,1,1,0,1,1,1,1,0,1,1,1,1,0,0,1,1,1,1},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
};

const int pat_bips[PAT_ROWS][PAT_COLS] = {
{1,1,1,1,0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,1},
{1,0,1,0,0,0,1,1,0,0,1,0,0,1,0,1,1,0,0,0},
{1,0,1,1,0,0,1,1,0,0,1,1,1,0,0,1,1,1,1,1},
{1,1,1,1,0,1,1,1,1,0,1,0,0,0,0,0,0,0,0,0},
{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
};

// uses five of the rows, 20 columns
// shows BIPS
const byte pat_byte_bips[PAT_COLS] = {
  B11111000,
  B10101000,
  B10101000,
  B01010000,
  B00000000,
  B10001000,
  B11111000,
  B10001000,
  B00000000,
  B11111000,
  B10100000,
  B10100000,
  B01000000,
  B00000000,
  B01000000,
  B10101000,
  B10101000,
  B10101000,
  B00010000,
  B00000000,
};

// empty image buffer
const byte pat_byte_empty[PAT_COLS] = {
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
  B00000000,
};
