package bombgame;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import java.nio.charset.StandardCharsets;

/**
 * Wraps the JSSC serial library used in the course.
 *
 * Java opens the serial port of the coordinator XBee Explorer.
 * The coordinator receives wireless messages from the XBee attached
 * to the Arduino Mega.
 */
public class XBeeConnection implements AutoCloseable {

    // Standard baud rate for Arduino serial communication (9600 bits per second)
    private static final int BAUD_RATE = 9600;
    // Timeout duration in milliseconds when waiting to read an incoming byte
    private static final int READ_TIMEOUT_MS = 250;

    // The system path identifier for the serial port (e.g., "COM3" on Windows or "/dev/ttyUSB0" on Linux)
    private final String portPath;
    // The JSSC library object representing the physical serial connection
    private final SerialPort serialPort;
    // Accumulates incoming characters over multiple reads until a full newline (\n) is detected
    private final StringBuilder incomingLine;

    /**
     * Constructor setup: stores the port path, instantiates the serial port,
     * initializes the buffer, and attempts to open/configure the connection.
     */
    public XBeeConnection(String portPath) throws SerialPortException {
        this.portPath = portPath;
        this.serialPort = new SerialPort(portPath);
        this.incomingLine = new StringBuilder();

        open();
    }

    /**
     * Opens the physical serial port and configures standard transmission parameters.
     */
    private void open() throws SerialPortException {
        if (!serialPort.openPort()) {
            throw new SerialPortException(
                    portPath,
                    "open",
                    "Could not open serial port"
            );
        }

        // Configure connection settings: 9600 Baud, 8 Data Bits, 1 Stop Bit, No Parity
        serialPort.setParams(
                BAUD_RATE,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE
        );
    }

    /**
     * Reads one complete newline-terminated message.
     *
     * A null return value means that no full line was available during
     * the timeout period. Partial data is preserved for the next call.
     */
    public String readLine() throws SerialPortException {
        // Continue reading loop as long as the hardware connection remains open
        while (serialPort.isOpened()) {
            try {
                // Read exactly 1 byte at a time with a timeout to keep the UI/main thread responsive
                byte[] data = serialPort.readBytes(1, READ_TIMEOUT_MS);

                // If no data arrived within the timeout period, exit and try again next cycle
                if (data == null || data.length ==
