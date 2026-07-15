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

    // Holds the correct sequence of color characters ('R', 'G', 'B', 'Y') for this game instance
    private final char[] sequence;
    // Tracks the current step/index in the sequence that the user needs to press next
    private int progress;
    // Flag indicating whether the entire sequence has been successfully matched
    private boolean complete;

    /**
     * Constructor initializes the game with a sequence determined by the seed.
     * Sets progress to 0 and complete flag to false.
     */
    public SimonSays(int seed) {
        this.sequence = createSequence(seed);
        this.progress = 0;
        this.complete = false;
    }

    /**
     * Generates a predefined sequence of colors based on the provided seed.
     * Uses a switch expression to return the matching array.
     */
    private char[] createSequence(int seed) {
        return switch (seed) {
            // Seed 1 sequence: Red, Green, Yellow, Red, Blue, Blue, Blue
            case 1 -> new char[] {'R', 'G', 'Y', 'R', 'B', 'B', 'B'};
            // Seed 2 sequence: Red, Green, Blue, Yellow, Red, Green, Yellow
            case 2 -> new char[] {'R', 'G', 'B', 'Y', 'R', 'G', 'Y'};
            // Seed 3 sequence: Blue, Blue, Blue, Blue, Green, Red, Red
            case 3 -> new char[] {'B', 'B', 'B', 'B', 'G', 'R', 'R'};
            // Throws an exception if the seed is invalid (not 1, 2, or 3)
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
        // If the game is already fully solved, any further input is accepted automatically
        if (complete) {
            return true;
        }

        // Convert input to uppercase to allow case-insensitive comparisons (e.g., 'r' -> 'R')
        char normalizedButton = Character.toUpperCase(button);

        // Guard clause: ensure the pressed button is one of the four valid color keys
        if (normalizedButton != 'R'
                && normalizedButton != 'G'
                && normalizedButton != 'B'
                && normalizedButton != 'Y') {
            return false;
        }

        // Check if the pressed button matches the expected button at the current progress index
        if (normalizedButton != sequence[progress]) {
            return false;
        }

        // Advance to the next step in the sequence
        progress++;

        // If the user has matched all steps in the sequence, mark the game as complete
        if (progress == sequence.length) {
            complete = true;
        }

        return true;
    }

    // Returns the current step/index the user has successfully reached
    public int getProgress() {
        return progress;
    }

    // Returns the total number of correct button presses required to complete the sequence
    public int getTotalSteps() {
        return sequence.length;
    }

    // Returns whether the game has been fully solved
    public boolean isComplete() {
        return complete;
    }

    // Returns the next expected color character, or '-' if the game is already complete
    public char getExpectedButton() {
        return complete ? '-' : sequence[progress];
    }

    // Returns a formatted string representing progress (e.g., "3/7")
    public String getProgressText() {
        return progress + "/" + sequence.length;
    }
}
