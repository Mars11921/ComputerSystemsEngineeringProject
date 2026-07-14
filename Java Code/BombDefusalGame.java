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

    private volatile GameResult result;
    private int seed;

    public BombDefusalGame(String xbeePort) {
        this.xbeePort = xbeePort;
        this.result = GameResult.STOPPED;
    }

    @Override
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
    protected void playGame() throws SerialPortException {
        while (result == GameResult.RUNNING) {
            String signal = connection.readLine();

            if (signal != null && !signal.isBlank()) {
                processSignal(signal.trim());
            }
        }
    }

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

    private synchronized void win() {
        if (result != GameResult.RUNNING) {
            return;
        }

        result = GameResult.WON;

        if (timer != null) {
            timer.stop();
        }
    }

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
    protected void handleUnexpectedError(Exception exception) {
        System.err.println(
                "Unexpected game error: " + exception.getMessage()
        );

        result = GameResult.LOST;
        sendCommandSafely("GAME:LOST");
    }

    @Override
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
