# Bomb Defusal Game

## Overview

This project is a wireless Bomb Defusal Game developed using an Arduino Mega, two XBee ZigBee modules, and a Java application. The Arduino reads the hardware inputs, while the Java application acts as the main controller that manages the game logic, validates player actions, and determines whether the player wins or loses.

Communication between the Arduino and Java application is performed wirelessly using ZigBee.

---

# Hardware Responsibilities

The Arduino Mega is responsible for:

- Reading the Simon Says push buttons.
- Reading keypad input.
- Detecting wire cuts.
- Reading the final push button.
- Updating the RGB LED.
- Updating the seven-segment display.
- Sending hardware events to Java through the XBee module.
- Receiving commands from the Java application.

## Arduino → Java Messages

```text
SIMON:R
SIMON:G
SIMON:B
SIMON:Y

KEY:1
KEY:A

WIRE:RED
WIRE:BROWN
WIRE:GREEN

END:PRESSED
```

## Java → Arduino Messages

```text
SEED:1
GAME:START
TIME:90
SIMON:COMPLETE
WIRE:COMPLETE
QUIZ:COMPLETE
GAME:WON
GAME:LOST
```

---

# Java Responsibilities

The Java application is the central controller of the project.

It is responsible for:

- Selecting the game seed.
- Running the countdown timer.
- Validating Simon Says inputs.
- Validating keypad inputs.
- Validating wire-cut events.
- Tracking module progress.
- Determining whether the player wins or loses.
- Sending commands back to the Arduino.

The Java project implements the **Template Method** design pattern.

---

# Java Classes

| Class | Responsibility |
|--------|----------------|
| Main | Starts the application. |
| AbstractGame | Defines the Template Method. |
| BombDefusalGame | Main game controller. |
| SimonSays | Validates Simon Says inputs. |
| KeypadQuiz | Validates keypad inputs. |
| CutTheWire | Validates wire cuts. |
| GameTimer | Runs the timer on a separate thread. |
| XBeeConnection | Handles ZigBee communication. |
| GameResult | Stores the current game state. |

---

# ZigBee Communication

- Arduino Mega ↔ End Device XBee
- Wireless ZigBee Connection
- Coordinator XBee ↔ Computer (USB)
- Java Application

Communication uses:
- Serial communication
- 9600 baud
- Newline-terminated text messages

---

# Timer Configuration

The timer starts at **90** and decreases by **1 every 2 seconds**, resulting in a total game time of approximately **3 minutes**.

---

# Notes

- Both XBee modules must use the same baud rate (9600).
- Java communicates with the coordinator XBee.
- The Arduino only handles hardware interaction; all game logic is implemented in Java.
