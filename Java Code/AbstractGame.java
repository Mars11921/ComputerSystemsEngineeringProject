package bombgame;

/**
 * Template Method parent class.
 *
 * runGame() defines the fixed game procedure. Subclasses provide
 * the game-specific initialization, input loop, and final actions.
 */
public abstract class AbstractGame {

    /**
     * Template method.
     *
     * The final keyword prevents subclasses from changing the required
     * sequence of the game lifecycle.
     */
    //   This is the core 'Template Method'. By making it final, we enforce 
    // a strict, immutable execution algorithm across all concrete game variations.
    public final void runGame() {
        try {
            //   Step 1: Pre-game setup (e.g., allocating memory, loading configurations).
            initializeGame();
            
            //   Step 2: Booting background threads, listeners, or hardware controllers.
            startGameServices();
            
            //   Step 3: Entering the main gameplay, interactive loop, or state-machine.
            playGame();
            
            //   Step 4: Wrapping up the game state, saving high scores, or displaying win/loss banners.
            finishGame();
        } catch (Exception exception) {
            //   Standardized error capture to prevent a crash from leaving hardware/connections in a bad state.
            handleUnexpectedError(exception);
        } finally {
            //   Guaranteed execution block to release resources (like ports or threads) regardless of success or failure.
            cleanUp();
        }
    }

    //   Abstract Hook: Subclasses must implement this to prepare their specific game variables.
    protected abstract void initializeGame() throws Exception;

    //   Abstract Hook: Subclasses must implement this to spin up their essential services (e.g., XBee serial listener).
    protected abstract void startGameServices() throws Exception;

    //   Abstract Hook: Subclasses must implement this to handle the active playing loop and user inputs.
    protected abstract void playGame() throws Exception;

    //   Abstract Hook: Subclasses must implement this to define actions when the game finishes normally.
    protected abstract void finishGame() throws Exception;

    //   Abstract Hook: Subclasses must implement this to log or gracefully recover from runtime errors.
    protected abstract void handleUnexpectedError(Exception exception);

    //   Abstract Hook: Subclasses must implement this to safely close serial streams, threads, and files.
    protected abstract void cleanUp();
}
