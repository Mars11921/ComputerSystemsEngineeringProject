package bombgame;

/**
 * Driver class.
 *
 * Run with the coordinator XBee serial port as the first argument:
 *
 * Windows:
 *   java bombgame.Main COM8
 *
 * macOS:
 *   java bombgame.Main /dev/cu.usbserial-A601EREF
 */
public class Main {

    public static void main(String[] args) {
        String portPath;

        if (args.length >= 1) {
            portPath = args[0];
        } else {
            /*
             * Change this default value to the coordinator XBee Explorer
             * port shown in Arduino IDE under Tools -> Port.
             */
            portPath = "COM8";

            System.out.println(
                    "No port argument was provided. Using default: "
                    + portPath
            );
        }

        BombDefusalGame game =
                new BombDefusalGame(portPath);

        game.runGame();
    }
}
