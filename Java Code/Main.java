package bombgame;

/**
 * Driver class.
 *
 * Run with the coordinator XBee serial port as the first argument:
 *
 * Windows:
 * java bombgame.Main COM8
 *
 * macOS:
 * java bombgame.Main /dev/cu.usbserial-A601EREF
 */
//   Entry point for the application. Coordinates the initialization and launch sequence.
public class Main {

    public static void main(String[] args) {
        String portPath;

        //   Checks if a custom serial port path (such as "COM3" or "/dev/ttyUSB0") 
        // was provided as a command-line argument.
        if (args.length >= 1) {
            portPath = args[0];
        } else {
            /*
             * Change this default value to the coordinator XBee Explorer
             * port shown in Arduino IDE under Tools -> Port.
             */
            //   Fallback default port. This matches the standard default configuration 
            // of the development environment.
            portPath = "COM8";

            System.out.println(
                    "No port argument was provided. Using default: "
                    + portPath
            );
        }

        //   Instantiates the concrete game controller using the targeted serial port connection.
        BombDefusalGame game =
                new BombDefusalGame(portPath);

        //   Triggers the immutable template method lifecycle defined in AbstractGame.
        game.runGame();
    }
}
