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

    private static final int BAUD_RATE = 9600;
    private static final int READ_TIMEOUT_MS = 250;

    private final String portPath;
    private final SerialPort serialPort;
    private final StringBuilder incomingLine;

    public XBeeConnection(String portPath) throws SerialPortException {
        this.portPath = portPath;
        this.serialPort = new SerialPort(portPath);
        this.incomingLine = new StringBuilder();

        open();
    }

    private void open() throws SerialPortException {
        if (!serialPort.openPort()) {
            throw new SerialPortException(
                    portPath,
                    "open",
                    "Could not open serial port"
            );
        }

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
        while (serialPort.isOpened()) {
            try {
                byte[] data = serialPort.readBytes(1, READ_TIMEOUT_MS);

                if (data == null || data.length == 0) {
                    return null;
                }

                char received = (char) (data[0] & 0xFF);

                if (received == '\n') {
                    String line = incomingLine.toString().trim();
                    incomingLine.setLength(0);
                    return line.isEmpty() ? null : line;
                }

                if (received != '\r') {
                    incomingLine.append(received);

                    if (incomingLine.length() > 256) {
                        incomingLine.setLength(0);
                    }
                }
            } catch (SerialPortTimeoutException timeout) {
                return null;
            }
        }

        return null;
    }

    /**
     * Sends one command to Arduino through the coordinator XBee.
     */
    public synchronized void writeLine(String message)
            throws SerialPortException {

        if (!serialPort.isOpened()) {
            throw new SerialPortException(
                    portPath,
                    "writeLine",
                    "Serial port is closed"
            );
        }

        byte[] bytes =
                (message + "\n").getBytes(StandardCharsets.UTF_8);

        serialPort.writeBytes(bytes);
    }

    public boolean isOpen() {
        return serialPort.isOpened();
    }

    @Override
    public void close() throws SerialPortException {
        if (serialPort.isOpened()) {
            serialPort.closePort();
        }
    }
}
