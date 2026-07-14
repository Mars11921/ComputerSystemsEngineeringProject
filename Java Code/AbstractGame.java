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
    public final void runGame() {
        try {
            initializeGame();
            startGameServices();
            playGame();
            finishGame();
        } catch (Exception exception) {
            handleUnexpectedError(exception);
        } finally {
            cleanUp();
        }
    }

    protected abstract void initializeGame() throws Exception;

    protected abstract void startGameServices() throws Exception;

    protected abstract void playGame() throws Exception;

    protected abstract void finishGame() throws Exception;

    protected abstract void handleUnexpectedError(Exception exception);

    protected abstract void cleanUp();
}
