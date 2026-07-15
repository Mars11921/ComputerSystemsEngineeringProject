package bombgame;

import jssc.SerialPortException;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Concrete game controller.
 *
 * This class contains the central game logic and is the concrete
 * implementation of the Template Method pattern.
 */
public class BombDefusalGame extends AbstractGame {

    private static final int STARTING_TIME_SECONDS = 99;

    private final String xbeePort;

    private XBeeConnection connection;
    private SimonSays simonSays;
    private KeypadQuiz keypadQuiz;
    private CutTheWire cutTheWire;
    private GameTimer timer;

    //   Volatile keyword ensures any thread-driven changes to the game result (e.g., from the timer thread) are immediately visible to the main execution loop thread.
    private volatile GameResult result;
    private int seed;

    public BombDefusalGame(String xbeePort) {
        this.xbeePort = xbeePort;
        this.result = GameResult.STOPPED;
    }

    @Override
    //   Overrides Template Method step. Initializes physical connection ports and uses a randomized seed (1-3) to coordinate puzzles on both platforms.
    protected void initializeGame() throws SerialPortException {
        System.out.println("Opening coordinator XBee on " + xbeePort + "...");

        connection = new XBeeConnection(xbeePort);

        seed = ThreadLocalRandom.current().nextInt(1, 4);

        simonSays = new SimonSays(seed);
        keypadQuiz = new KeypadQuiz(seed);
        cutTheWire = new CutTheWire(seed);

        result = GameResult.RUNNING;

        System.out.println("Selected seed: " + seed);
        System.out.println("All game validation will be performed by Java.");
    }

    @Override
    //   Overrides Template Method step. Starts background game timers and transmits startup directives ("SEED" and "GAME:START") over XBee to prime the Arduino.
    protected void startGameServices() throws SerialPortException {
        timer = new GameTimer(
                STARTING_TIME_SECONDS,
                this::sendTimeToArduino,
                this::handleTimerExpired
        );

        connection.writeLine("SEED:" + seed);
        connection.writeLine("GAME:START");

        timer.start();

        System.out.println("Game started.");
        System.out.println("Waiting for raw Arduino signals...");
    }

    @Override
    //   Overrides Template Method step. Continuous poll loop driving active runtime play. Reads inbound Xbee string events until state changes from RUNNING.
    protected void playGame() throws SerialPortException {
        while (result == GameResult.RUNNING) {
            String signal = connection.readLine();

            if (signal != null && !signal.isBlank()) {
                processSignal(signal.trim());
            }
        }
    }

    //   Parses inbound colon-delimited string messages from Arduino (e.g., "SIMON:R" or "KEY:4") and delegates them to their validation handlers.
    private void processSignal(String signal) {
        System.out.println("Arduino -> Java: " + signal);

        String[] parts = signal.split(":", 3);

        if (parts.length < 2) {
            System.out.println("Ignored malformed signal: " + signal);
            return;
        }

        String category = parts[0].trim().toUpperCase();
        String value = parts[1].trim().toUpperCase();

        switch (category) {
            case "ARDUINO" -> handleArduinoStatus(value);
            case "SIMON" -> handleSimon(value);
            case "KEY" -> handleKeypad(value);
            case "WIRE" -> handleWire(value);
            case "END" -> handleEndButton(value);
            default -> System.out.println(
                    "Ignored unknown signal category: " + category
            );
        }
    }

    private void handleArduinoStatus(String value) {
        if (value.equals("READY")) {
            System.out.println("Arduino confirmed that the hardware is ready.");
        } else if (value.equals("BOOTED")) {
            System.out.println("Arduino hardware has booted.");
        }
    }

    //   Validates Simon Says button press sequences sequentially. Triggers loss conditions instantly if an out-of-order button is pressed.
    private void handleSimon(String value) {
        if (value.length() != 1) {
            lose("Invalid Simon signal.");
            return;
        }

        char pressedButton = value.charAt(0);
        char expectedButton = simonSays.getExpectedButton();

        if (!simonSays.pressButton(pressedButton)) {
            lose(
                    "Wrong Simon button. Pressed "
                    + pressedButton
                    + " but expected "
                    + expectedButton
                    + "."
            );
            return;
        }

        System.out.println(
                "Simon progress: " + simonSays.getProgressText()
        );

        if (simonSays.isComplete()) {
            System.out.println("Simon Says module completed.");
            sendCommandSafely("SIMON:COMPLETE");
        }
    }

    //   Validates sequential keypad password inputs. If the wrong key is keyed, the bomb immediately triggers a loss sequence.
    private void handleKeypad(String value) {
        if (value.length() != 1) {
            lose("Invalid keypad signal.");
            return;
        }

        char key = value.charAt(0);

        if (!keypadQuiz.pressKey(key)) {
            lose("Wrong keypad key: " + key);
            return;
        }

        System.out.println(
                "Keypad progress: " + keypadQuiz.getProgressText()
        );

        if (keypadQuiz.isComplete()) {
            System.out.println("Keypad module completed.");
            sendCommandSafely("QUIZ:COMPLETE");
        }
    }

    //   Validates the sequence of cut wires (analog pins going HIGH on Arduino side). Mismatched wire cuts result in explosion/loss.
    private void handleWire(String value) {
        if (!cutTheWire.cut(value)) {
            lose("Wrong wire cut: " + value);
            return;
        }

        System.out.println(
                "Wire progress: " + cutTheWire.getProgressText()
        );

        if (cutTheWire.isComplete()) {
            System.out.println("Wire module completed.");
            sendCommandSafely("WIRE:COMPLETE");
        }
    }

    //   Evaluates victory condition. The user must complete Simon, Keypad, and Wire puzzles completely BEFORE triggering the main end button.
    private void handleEndButton(String value) {
        if (!value.equals("PRESSED")) {
            return;
        }

        boolean allModulesComplete =
                simonSays.isComplete()
                && keypadQuiz.isComplete()
                && cutTheWire.isComplete();

        if (allModulesComplete) {
            win();
        } else {
            lose("The final button was pressed before all modules were complete.");
        }
    }

    //   Synchronized state transition to declare game victory, modifying status flags and stopping active timers cleanly.
    private synchronized void win() {
        if (result != GameResult.RUNNING) {
            return;
        }

        result = GameResult.WON;

        if (timer != null) {
            timer.stop();
        }
    }

    //   Synchronized state transition to declare game failure, stopping active timers and documenting the failure reason.
    private synchronized void lose(String reason) {
        if (result != GameResult.RUNNING) {
            return;
        }

        System.out.println("Loss reason: " + reason);
        result = GameResult.LOST;

        if (timer != null) {
            timer.stop();
        }
    }

    private void handleTimerExpired() {
        lose("The timer reached zero.");
    }

    private void sendTimeToArduino(int secondsRemaining) {
        sendCommandSafely("TIME:" + secondsRemaining);
    }

    //   Transmits status commands to the Arduino via XBee. Handles port disconnections gracefully without crashing the app.
    private void sendCommandSafely(String command) {
        try {
            if (connection != null && connection.isOpen()) {
                connection.writeLine(command);
            }
        } catch (SerialPortException exception) {
            lose("Could not send command to Arduino: " + exception.getMessage());
        }
    }

    @Override
    //   Overrides Template Method step. Resolves final state and communicates success/failure packets directly to the physical system.
    protected void finishGame() throws SerialPortException {
        if (result == GameResult.WON) {
            System.out.println("BOMB DEFUSED — PLAYER WON!");
            connection.writeLine("GAME:WON");
        } else if (result == GameResult.LOST) {
            System.out.println("BOMB EXPLODED — PLAYER LOST.");
            connection.writeLine("GAME:LOST");
        }
    }

    @Override
    //   Overrides Template Method step. Safely catches background runtime exceptions and alerts the Arduino to clean up.
    protected void handleUnexpectedError(Exception exception) {
        System.err.println(
                "Unexpected game error: " + exception.getMessage()
        );

        result = GameResult.LOST;
        sendCommandSafely("GAME:LOST");
    }

    @Override
    //   Overrides Template Method step. Executed via final block to safely stop running timers and release serial hardware ports.
    protected void cleanUp() {
        if (timer != null) {
            timer.stop();
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SerialPortException exception) {
                System.err.println(
                        "Could not close XBee port: "
                        + exception.getMessage()
                );
            }
        }

        System.out.println("Game resources closed.");
    }
}
