package net.sf.servomaster.device.impl.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import net.sf.servomaster.device.impl.AbstractServoController;
import net.sf.servomaster.device.impl.HardwareServo;
import net.sf.servomaster.device.model.ServoController;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Base class for all serial servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractSerialServoController extends AbstractServoController {

    /**
     * Open timeout.
     *
     * <p>
     *
     * Wait this much trying to open the serial port.
     */
    public static final int OPEN_TIMEOUT = 5000;

    /**
     * String key for retrieving the controller baud rate property.
     */
    public static final String META_SPEED = "controller/protocol/serial/speed";

    /**
     * The serial port.
     */
    protected SerialPort port = null;

    /**
     * The serial port output stream.
     */
    private OutputStream serialOut;

    protected AbstractSerialServoController(String portName) {
        super(portName);
    }

    /**
     * Initialize the controller.
     *
     * @throws IllegalStateException if the controller has already been initialized.
     */
    @Override
    protected void doInit() throws IOException {

        if (this.portName == null) {

            throw new IllegalArgumentException("null portName is invalid: serial controllers don't support automated discovery");
        }

        try {

            // This is a stupid way to do it, but oh well, "release early"

            var portsTried = new ArrayList<String>();

            for (Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers(); ports.hasMoreElements();) {

                CommPortIdentifier id = (CommPortIdentifier)ports.nextElement();

                // In case we fail, we'd like to tell the caller what's
                // available

                portsTried.add(id.getName());

                if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                    if (id.getName().equals(portName)) {

                        try {

                            port = (SerialPort)id.open(getClass().getName(), OPEN_TIMEOUT);

                        } catch (PortInUseException ex) {

                            // If the exception is thrown here, we won't be
                            // able to enumerate the rest of the ports - all
                            // we have to do is to log it.

                            logger.warn("Port in use, skipped", new IOException("Port in use, skipped", ex));
                        }

                        break;
                    }
                }
            }

            if (port == null) {
                throw new IllegalArgumentException("No suitable port found, tried: " + portsTried);
            }

            serialOut = port.getOutputStream();

            // A particular controller may have a different speed setting.
            // If there's none, we'll fall back to the default of 2400 baud.
            // And all of the controllers supported far use 8N1, so we'll
            // just leave that as a default.

            var portSpeed = 2400;
            var controllerMeta = getMeta();

            if (controllerMeta == null) {

                logger.warn("Driver doesn't support meta, port speed is 2400: {}", getClass().getName());

            } else {

                try {

                    // speedObject will never be null - we'll get
                    // UnsupportedOperationException instead

                    var speedObject = controllerMeta.getProperty(META_SPEED);

                    try {

                        var speedString = (String) speedObject;

                        portSpeed = Integer.parseInt(speedString);

                    } catch (Throwable t) { // NOSONAR Consequences have been considered

                        // This is serious enough to blow up - somebody did
                        // a bad job, the speed is hardcoded into the driver

                        throw (IllegalArgumentException) new IllegalArgumentException("Unable to parse property "
                                + META_SPEED + ", object class is " + speedObject.getClass().getName() + ", value is '" + speedObject + "'", t);
                    }

                } catch (UnsupportedOperationException ex) {
                    logger.error("Port speed is 2400, cause: ", ex);
                }
            }

            // Now, if someone has specified an insane speed, that's not my
            // problem... Hopefully, the port implementation will filter it
            // out.

            port.setSerialPortParams(portSpeed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

        } catch (UnsupportedCommOperationException ucoex) {
            throw (IOException) new IOException("Unsupported comm operation", ucoex);
        }
    }

    /**
     * Find out whether the controller is connected.
     *
     * So far, I haven't seen a single serial controller that can return
     * some status. Therefore, for the time being this method will always
     * return true, until a reliable way to deal with it will be found.
     *
     * @return Unconditional true.
     */
    @Override
    public final boolean isConnected() {
        checkInit();
        return true;
    }

    /**
     * Send the byte down the {@link #serialOut serial port stream}.
     *
     * @param b Byte to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected synchronized void send(byte b) throws IOException {
        serialOut.write(b);
        serialOut.flush();
    }

    /**
     * Send the data buffer down the {@link #serialOut serial port stream}.
     *
     * @param buffer Buffer to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected final synchronized void send(byte[] buffer) throws IOException {

        // VT: FIXME: Can be optimized

        for (byte b : buffer) {
            serialOut.write(b);
        }

        serialOut.flush();
    }

    protected abstract class SerialServo extends HardwareServo {

        protected SerialServo(ServoController sc, int id) {
            super(sc, id);
        }

        @Override
        protected final void setActualPosition(double position) throws IOException {

            checkInit();
            checkPosition(position);

            synchronized ( getController() ) {

                // The reason it is synchronized on the controller is that the
                // setActualPosition() calls the controller's synchronized methods
                // and the deadlock can occur if *this* method was made synchronized

                sendPosition(position);
                actualPosition = position;
            }

            actualPositionChanged(actualPosition);

            AbstractSerialServoController.this.touch();
        }

        /**
         * Send the position command to the controller.
         *
         * @param position Position to send.
         * @throws IOException if there's a hardware error.
         */
        protected abstract void sendPosition(double position) throws IOException;
    }
}
