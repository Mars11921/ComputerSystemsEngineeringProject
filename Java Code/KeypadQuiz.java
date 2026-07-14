package bombgame;

/**
 * Contains the keypad quiz logic.
 *
 * The character X in a sequence means that any key from A to D is accepted.
 */
public class KeypadQuiz {

    private final char[] sequence;
    private int progress;
    private boolean complete;

    public KeypadQuiz(int seed) {
        this.sequence = createSequence(seed);
        this.progress = 0;
        this.complete = false;
    }

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
    public boolean pressKey(char key) {
        if (complete) {
            return true;
        }

        char expected = sequence[progress];
        char normalizedKey = Character.toUpperCase(key);

        boolean correct;

        if (expected == 'X') {
            correct = normalizedKey >= 'A' && normalizedKey <= 'D';
        } else {
            correct = normalizedKey == expected;
        }

        if (!correct) {
            return false;
        }

        progress++;

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

    public String getProgressText() {
        return progress + "/" + sequence.length;
    }
}
