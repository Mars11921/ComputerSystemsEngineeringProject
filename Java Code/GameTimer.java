package bombgame;

import java.util.function.IntConsumer;

/**
 * Runs the game countdown on a separate thread.
 *
 * This prevents timer operations from blocking XBee input processing.
 */
public class GameTimer implements Runnable {

    private final IntConsumer tickHandler;
    private final Runnable expirationHandler;
    private final Thread timerThread;

    private volatile boolean running;
    private volatile int secondsRemaining;

    public GameTimer(
            int startingSeconds,
            IntConsumer tickHandler,
            Runnable expirationHandler) {

        if (startingSeconds < 1 || startingSeconds > 99) {
            throw new IllegalArgumentException(
                    "Starting time must be between 1 and 99 seconds."
            );
        }

        this.secondsRemaining = startingSeconds;
        this.tickHandler = tickHandler;
        this.expirationHandler = expirationHandler;
        this.timerThread = new Thread(this, "Bomb-Game-Timer");
    }

    public void start() {
        if (timerThread.isAlive()) {
            return;
        }

        running = true;
        tickHandler.accept(secondsRemaining);
        timerThread.start();
    }

    @Override
    public void run() {
        try {
            while (running && secondsRemaining > 0) {
                Thread.sleep(2000);

                if (!running) {
                    break;
                }

                secondsRemaining--;
                tickHandler.accept(secondsRemaining);
            }

            if (running && secondsRemaining == 0) {
                expirationHandler.run();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
        timerThread.interrupt();
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }
}
