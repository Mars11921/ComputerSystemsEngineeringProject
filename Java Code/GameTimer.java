package bombgame;

import java.util.function.IntConsumer;

/**
 * Runs the game countdown on a separate thread.
 *
 * This prevents timer operations from blocking XBee input processing.
 */
//   Implements Runnable so this timer can run concurrently on its own execution thread,
// preventing standard Thread.sleep() delays from freezing the XBee serial interface.
public class GameTimer implements Runnable {

    //   Functional interfaces used to decouple the timer from UI or communication logic.
    // tickHandler handles the updated second count, and expirationHandler executes the "game lost" logic on timeout.
    private final IntConsumer tickHandler;
    private final Runnable expirationHandler;
    private final Thread timerThread;

    //   Marked volatile to guarantee that when the main thread modifies these flags (e.g., calling stop()),
    // the change is instantly visible to the active countdown thread.
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
        //   Initializes but does not start the background thread, giving the caller control over exact trigger timing.
        this.timerThread = new Thread(this, "Bomb-Game-Timer");
    }

    //   Safely activates the countdown sequence and starts executing the run() loop in the concurrent thread.
    public void start() {
        if (timerThread.isAlive()) {
            return;
        }

        running = true;
        tickHandler.accept(secondsRemaining);
        timerThread.start();
    }

    @Override
    //   The concurrent thread's main runtime loop. It sleeps for the configured interval,
    // decrements time, and fires updates until stopped or zero is reached.
    public void run() {
        try {
            while (running && secondsRemaining > 0) {
                //   In this system configuration, each game unit of time represents 2 real-world seconds.
                Thread.sleep(2000);

                if (!running) {
                    break;
                }

                secondsRemaining--;
                tickHandler.accept(secondsRemaining);
            }

            //   Exits normally and fires the failure callback once the clock cleanly bottoms out at zero.
            if (running && secondsRemaining == 0) {
                expirationHandler.run();
            }
        } catch (InterruptedException exception) {
            //   Restores interrupted status so upstream handlers are aware that this thread was stopped/reset.
            Thread.currentThread().interrupt();
        }
    }

    //   Signals the countdown loop to terminate and immediately wakes the sleeping thread to tear down resources.
    public void stop() {
        running = false;
        timerThread.interrupt();
    }

    public int getSeconds
