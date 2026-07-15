#include <SoftwareSerial.h>
#include <Keypad.h>

/*
 * Bomb Defusal Hardware Controller
 *
 * RESPONSIBILITY OF THIS ARDUINO:
 * 1. Read physical inputs.
 * 2. Send raw input events to Java through XBee.
 * 3. Receive commands from Java.
 * 4. Control the seed LED and seven-segment timer.
 *
 * RESPONSIBILITY OF JAVA:
 * 1. Choose the seed.
 * 2. Validate Simon Says, keypad, and wire inputs.
 * 3. Run the timer.
 * 4. Decide whether the player wins or loses.
 */

// XBee connection: SoftwareSerial(rxPin, txPin)
SoftwareSerial xbeeSerial(52, 50);
// ---------------------------- Keypad ----------------------------

const byte ROWS = 4;
const byte COLS = 4;
char hexaKeys[ROWS][COLS] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};
byte rowPins[ROWS] = {47, 45, 43, 41};
byte colPins[COLS] = {33, 35, 37, 39};

// Instantiates the Keypad library handler with the specified row/column matrices and pin layouts.
Keypad customKeypad =
    Keypad(makeKeymap(hexaKeys), rowPins, colPins, ROWS, COLS);
// ------------------------- Seven segment ------------------------

//  Truth table mapping decimal digits (0-9) to active-LOW (0) or active-HIGH (1) segment patterns.
// Each row represents a digit, and each index in the nested array corresponds to an individual physical segment (A-G + DP).
const bool digitPatterns[10][8] = {
  {0, 0, 0, 0, 0, 0, 1, 1}, // 0
  {1, 0, 0, 1, 1, 1, 1, 1}, // 1
  {0, 0, 1, 0, 0, 1, 0, 1}, // 2
  {0, 0, 0, 0, 1, 1, 0, 1}, // 3
  {1, 0, 0, 1, 1, 0, 0, 1}, // 4
  {0, 1, 0, 0, 1, 0, 0, 1}, // 5
  {0, 1, 0, 0, 0, 0, 0, 1}, // 6
  {0, 0, 0, 1, 1, 1, 1, 1}, // 7
  {0, 0, 0, 0, 0, 0, 0, 1}, // 8
  {0, 0, 0, 0, 1, 0, 0, 1}  // 9
};

//  Pin arrays mapped to the seven-segment displays. 
// M_timerPins controls the Most Significant Digit (tens place); L_timerPins controls the Least Significant Digit (ones place).
int M_timerPins[8] = {11, 10, 7, 8, 9, 12, 13, 6};
int L_timerPins[8] = {3, 2, 16, 15, 14, 4, 5, 17};
// ---------------------------- Inputs ----------------------------

#define SIMON_R 20
#define SIMON_G 22
#define SIMON_B 21
#define SIMON_Y 23

#define BROWN_WIRE 31
#define RED_WIRE 29
#define GREEN_WIRE 27

#define END_BUTTON 19

// ---------------------------- LEDs ------------------------------

#define RED_LED 51
#define GREEN_LED 49
#define BLUE_LED 53

// -------------------------- Game state --------------------------

bool gameRunning = false;
bool gameOver = false;
int currentSeed = 0;
int displayedTime = 99;
// Previous input states are used for edge detection.
bool previousSimonR = false;
bool previousSimonG = false;
bool previousSimonB = false;
bool previousSimonY = false;
bool previousEndButton = false;

bool previousBrownWire = false;
bool previousRedWire = false;
bool previousGreenWire = false;

// Buffer used to receive one text command from Java.
String commandBuffer = "";
// ---------------------- Seven-segment methods -------------------

//  Updates a single 7-segment display by pulling pins HIGH/LOW according to the pattern matching the desired digit.
void print71(int pins[8], int output) {
  if (output < 0 || output > 9) {
    return;
  }

  for (int i = 0; i < 8; i++) {
    digitalWrite(pins[i], digitPatterns[output][i]);
  }
}

//  Splices a two-digit integer into individual tens and ones digits, then updates both displays.
void print72(int MSPs[8], int LSPs[8], int output) {
  if (output < 0) {
    output = 0;
  }

  if (output > 99) {
    output = 99;
  }

  print71(MSPs, output / 10);
  print71(LSPs, output % 10);
}

// ---------------------- Communication methods ------------------

//  Sends telemetry to the Java backend via Software Serial (XBee) and echoes to the hardware USB Serial for debugging.
void sendEvent(const String &eventMessage) {
  // Send to Java through the XBee.
  xbeeSerial.println(eventMessage);

  // Also print locally for debugging.
  Serial.print("Arduino -> Java: ");
  Serial.println(eventMessage);
}

//  Dictates the status light color matching the active game puzzle seed. RGB values are inverted (active-LOW).
void setSeedLed(int seed) {
  // The RGB LED in the original circuit is active LOW.
  digitalWrite(RED_LED, HIGH);
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);

  if (seed == 1) {
    digitalWrite(RED_LED, LOW);
  } else if (seed == 2) {
    digitalWrite(GREEN_LED, LOW);
  } else if (seed == 3) {
    digitalWrite(BLUE_LED, LOW);
  }
}

//  Halts active hardware routines and illuminates the Green status LED to signify game completion.
void showWin() {
  gameRunning = false;
  gameOver = true;

  // Green indicates success.
  digitalWrite(RED_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);
  digitalWrite(GREEN_LED, LOW);

  Serial.println("Java decided: GAME WON");
}

//  Halts active routines, zeroes the hardware clocks, and displays Red to signify game failure.
void showLoss() {
  gameRunning = false;
  gameOver = true;

  displayedTime = 0;
  print72(M_timerPins, L_timerPins, displayedTime);

  // Red indicates failure.
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);
  digitalWrite(RED_LED, LOW);

  Serial.println("Java decided: GAME LOST");
}

//  Restores default timer values and primes button state variables to prevent immediate registration of pre-held triggers.
void resetHardwareState() {
  gameOver = false;
  displayedTime = 99;
  print72(M_timerPins, L_timerPins, displayedTime);

  // Read the current states so old inputs are not treated as new events.
  previousSimonR = digitalRead(SIMON_R);
  previousSimonG = digitalRead(SIMON_G);
  previousSimonB = digitalRead(SIMON_B);
  previousSimonY = digitalRead(SIMON_Y);
  previousEndButton = digitalRead(END_BUTTON);

  previousBrownWire = digitalRead(BROWN_WIRE);
  previousRedWire = digitalRead(RED_WIRE);
  previousGreenWire = digitalRead(GREEN_WIRE);
}

//  Command parser for incoming serial instructions from the Java host application.
void processJavaCommand(String command) {
  command.trim();

  if (command.length() == 0) {
    return;
  }

  Serial.print("Java -> Arduino: ");
  Serial.println(command);

  if (command.startsWith("SEED:")) {
    int seed = command.substring(5).toInt();
    if (seed >= 1 && seed <= 3) {
      currentSeed = seed;
      setSeedLed(currentSeed);
    }
  }
  else if (command == "GAME:START") {
    resetHardwareState();
    gameRunning = true;
    sendEvent("ARDUINO:READY");
  }
  else if (command.startsWith("TIME:")) {
    int newTime = command.substring(5).toInt();
    displayedTime = constrain(newTime, 0, 99);
    print72(M_timerPins, L_timerPins, displayedTime);
  }
  else if (command == "SIMON:COMPLETE") {
    Serial.println("Simon module completed.");
  }
  else if (command == "WIRE:COMPLETE") {
    Serial.println("Wire module completed.");
  }
  else if (command == "QUIZ:COMPLETE") {
    Serial.println("Quiz module completed.");
  }
  else if (command == "GAME:WON") {
    showWin();
  }
  else if (command == "GAME:LOST") {
    showLoss();
  }
  else if (command == "GAME:RESET") {
    gameRunning = false;
    currentSeed = 0;
    resetHardwareState();

    digitalWrite(RED_LED, HIGH);
    digitalWrite(GREEN_LED, HIGH);
    digitalWrite(BLUE_LED, HIGH);
  }
}

//  Non-blocking buffered read of characters arriving over Software Serial. Delivers commands on newline characters.
void receiveJavaCommands() {
  while (xbeeSerial.available() > 0) {
    char received = (char)xbeeSerial.read();
    if (received == '\n') {
      processJavaCommand(commandBuffer);
      commandBuffer = "";
    }
    else if (received != '\r') {
      commandBuffer += received;
      // Protect the Arduino from an excessively long malformed message.
      if (commandBuffer.length() > 80) {
        commandBuffer = "";
      }
    }
  }
}

// ------------------------- Input methods ------------------------

//  Implements rising-edge detection (LOW to HIGH transition) to register physical button-down actions exactly once.
void checkButtonEvent(int pin, bool &previousState, const String &eventMessage) {
  bool currentState = digitalRead(pin);
  // Send only on a LOW-to-HIGH transition.
  if (gameRunning && currentState && !previousState) {
    sendEvent(eventMessage);
  }

  previousState = currentState;
}

void readSimonButtons() {
  checkButtonEvent(SIMON_R, previousSimonR, "SIMON:R");
  checkButtonEvent(SIMON_G, previousSimonG, "SIMON:G");
  checkButtonEvent(SIMON_B, previousSimonB, "SIMON:B");
  checkButtonEvent(SIMON_Y, previousSimonY, "SIMON:Y");
}

//  Polls the keypad module array state and broadcasts any registered keypress to the Java interface.
void readKeypad() {
  if (!gameRunning) {
    return;
  }

  char key = customKeypad.getKey();
  if (key) {
    String eventMessage = "KEY:";
    eventMessage += key;
    sendEvent(eventMessage);
  }
}

//  : Monitors physical wire connections. Uses internal pullups so cut wires change state from Ground (LOW) to High-Impedance (HIGH).
void checkWireEvent(int pin, bool &previousState, const String &wireName) {
  bool currentState = digitalRead(pin);
  /*
   * The wire pins use INPUT_PULLUP.
   * Connected-to-ground is normally LOW and a cut/disconnected wire becomes HIGH.
   * Therefore, a LOW-to-HIGH transition represents a wire cut.
   */
  if (gameRunning && currentState && !previousState) {
    sendEvent("WIRE:" + wireName);
  }

  previousState = currentState;
}

void readWires() {
  checkWireEvent(BROWN_WIRE, previousBrownWire, "BROWN");
  checkWireEvent(RED_WIRE, previousRedWire, "RED");
  checkWireEvent(GREEN_WIRE, previousGreenWire, "GREEN");
}

void readEndButton() {
  checkButtonEvent(END_BUTTON, previousEndButton, "END:PRESSED");
}

// ----------------------- Arduino lifecycle ----------------------

//  Assigns pin configurations for all display networks, inputs, pullups, and indicators.
void initializePins() {
  // Seven-segment outputs.
  for (int pin = 2; pin < 18; pin++) {
    pinMode(pin, OUTPUT);
    digitalWrite(pin, HIGH);
  }

  // Seed/status LED.
  pinMode(RED_LED, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(BLUE_LED, OUTPUT);

  digitalWrite(RED_LED, HIGH);
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);

  // Simon buttons.
  pinMode(SIMON_R, INPUT);
  pinMode(SIMON_G, INPUT);
  pinMode(SIMON_B, INPUT);
  pinMode(SIMON_Y, INPUT);

  // Final button.
  pinMode(END_BUTTON, INPUT);

  // Wire inputs.
  pinMode(BROWN_WIRE, INPUT_PULLUP);
  pinMode(RED_WIRE, INPUT_PULLUP);
  pinMode(GREEN_WIRE, INPUT_PULLUP);
}

void setup() {
  initializePins();

  // Local USB debugging.
  Serial.begin(9600);

  // XBee communication.
  // This must match the configured XBee baud rate.
  xbeeSerial.begin(9600);

  resetHardwareState();

  Serial.println("Bomb hardware ready.");
  sendEvent("ARDUINO:BOOTED");
}

// The core execution loop. Repeatedly scans serial buffer and parses inputs while game state allows.
void loop() {
  receiveJavaCommands();
  if (gameRunning && !gameOver) {
    readSimonButtons();
    readKeypad();
    readWires();
    readEndButton();
  }

  // Small delay reduces switch bounce and unnecessary CPU use.
  delay(20);
}
