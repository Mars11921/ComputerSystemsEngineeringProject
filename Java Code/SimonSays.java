package bombgame;

/**
 * Contains the complete Simon Says game logic.
 *
 * Arduino sends only raw button signals:
 * SIMON:R, SIMON:G, SIMON:B, or SIMON:Y.
 *
 * Java checks each signal against the sequence selected by the seed.
 */
public class SimonSays {

    private final char[] sequence;
    private int progress;
    private boolean complete;

    public SimonSays(int seed) {
        this.sequence = createSequence(seed);
        this.progress = 0;
        this.complete = false;
    }

    private char[] createSequence(int seed) {
        return switch (seed) {
            case 1 -> new char[] {'R', 'G', 'Y', 'R', 'B', 'B', 'B'};
            case 2 -> new char[] {'R', 'G', 'B', 'Y', 'R', 'G', 'Y'};
            case 3 -> new char[] {'B', 'B', 'B', 'B', 'G', 'R', 'R'};
            default -> throw new IllegalArgumentException(
                    "Simon seed must be 1, 2, or 3."
            );
        };
    }

    /**
     * Processes one physical Simon button press.
     *
     * @return true if the input is correct; false if it is wrong
     */
    public boolean pressButton(char button) {
        if (complete) {
            return true;
        }

        char normalizedButton = Character.toUpperCase(button);

        if (normalizedButton != 'R'
                && normalizedButton != 'G'
                && normalizedButton != 'B'
                && normalizedButton != 'Y') {
            return false;
        }

        if (normalizedButton != sequence[progress]) {
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

    public char getExpectedButton() {
        return complete ? '-' : sequence[progress];
    }

    public String getProgressText() {
        return progress + "/" + sequence.length;
    }
}
