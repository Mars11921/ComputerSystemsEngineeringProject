package bombgame;

/**
 * Contains the keypad quiz logic.
 *
 * The character X in a sequence means that any key from A to D is accepted.
 */
//   This class encapsulates the validation logic and step-by-step progress tracking for the keypad puzzle module.
public class KeypadQuiz {

    private final char[] sequence;
    private int progress;
    private boolean complete;

    //   Constructor builds the correct keycode sequence based on the active seed selected at game start.
    public KeypadQuiz(int seed) {
        this.sequence = createSequence(seed);
        this.progress = 0;
        this.complete = false;
    }

    //   Generates the specific puzzle sequence for the keypad module.
    // Notice that 'X' acts as a wildcard key character that allows custom validations.
    private char[] createSequence(int seed) {
        return switch (seed) {
            case 1 -> new char[] {'2', '0', '1', '8', 'X'};
            case 2 -> new char[] {'1', '3', '0', 'X'};
            case 3 -> new char[] {'1', '1', '8', 'X'};
            default -> throw new IllegalArgumentException(
                    "Quiz seed must be 1, 2, or 3."
            );
        };
    }

    /**
     * Processes one keypad signal received from Arduino.
     *
     * @return true if the key is correct; false if it is wrong
     */
    //   Handles sequential input checking. Returns 'false' (triggering a loss) if an incorrect key is pressed,
    // and 'true' if the input matches the sequence or satisfies the wildcard criteria.
    public boolean pressKey(char key) {
        if (complete) {
            return true;
        }

        char expected = sequence[progress];
        //   Case-insensitivity ensures that lowercase inputs sent through serial streams still validate correctly.
        char normalizedKey = Character.toUpperCase(key);

        boolean correct;

        //   Evaluates the wildcard rule. If the sequence expects 'X', then any character from 'A' to 'D' is accepted.
        if (expected == 'X') {
            correct = normalizedKey >= 'A' && normalizedKey <= 'D';
        } else {
            correct = normalizedKey == expected;
        }

        if (!correct) {
            return false;
        }

        //   Advances the user to the next step in the sequence upon a successful keypress.
        progress++;

        //   Once progress matches the length of the predefined sequence, the module is marked completed.
        if (progress == sequence.length) {
            complete = true;
        }

        return true;
    }

    public int getProgress() {
        return progress;
    }

    public int getTotalSteps() {
        return sequence.length;
    }

    public boolean isComplete() {
        return complete;
    }

    //   Formats the live keypad
