package bombgame;

/**
 * Represents the final state of the game.
 */
//   This enum serves as the central state machine driver, allowing both the main execution loop and background timer threads to coordinate the lifecycle of a play session.
public enum GameResult {
    //   Active state. The game loop is actively polling input signals, validating actions, and counting down the clock.
    RUNNING,
    
    //   Terminal success state. The player solved all modules and pressed the disarm button before time expired.
    WON,
    
    //   Terminal failure state. Triggered by an incorrect button/key sequence, a forbidden wire cut, or the countdown reaching zero.
    LOST,
    
    //   Default idle state. Active before the game officially starts or after a full system reset is completed.
    STOPPED
}
