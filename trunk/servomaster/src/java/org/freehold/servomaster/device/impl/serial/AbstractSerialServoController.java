package org.freehold.servomaster.device.impl.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.HardwareServo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;

/**
 * Base class for all serial servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: AbstractSerialServoController.java,v 1.14 2006-12-14 09:17:08 vtt Exp $
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

    protected AbstractSerialServoController() {

        // Can't invoke this(null) because this will blow up in doInit()
    }

    protected AbstractSerialServoController(String portName) throws IOException {

        super(portName);
    }

    /**
     * Initialize the controller.
     *
     * @param portName Serial port name recognized by <a
     * href="http://java.sun.com/products/javacomm/" target="_top">Java
     * Communications API</a>.
     *
     * @exception IllegalStateException if the controller has already been initialized.
     */
    @Override
    protected void doInit(String portName) throws IOException {

        this.portName = portName;

        if ( this.portName == null ) {

            throw new IllegalArgumentException("null portName is invalid: serial controllers don't support automated discovery");
        }

        servoSet = new Servo[getServoCount()];

        try {

            // This is a stupid way to do it, but oh well, "release early"

            List<String> portsTried = new LinkedList<String>();

            for ( Enumeration ports = CommPortIdentifier.getPortIdentifiers(); ports.hasMoreElements(); ) {

                CommPortIdentifier id = (CommPortIdentifier)ports.nextElement();

                // In case we fail, we'd like to tell the caller what's
                // available

                portsTried.add(id.getName());

                if ( id.getPortType() == CommPortIdentifier.PORT_SERIAL ) {

                    if ( id.getName().equals(portName) ) {

                        try {

                            port = (SerialPort)id.open(getClass().getName(), OPEN_TIMEOUT);

                        } catch ( PortInUseException piuex ) {

                            // If the exception is thrown here, we won't be
                            // able to enumerate the rest of the ports - all
                            // we have to do is to log it.

                            System.err.println(new IOException("Port in use, skipped").initCause(piuex));
                        }

                        break;
                    }
                }
            }

            if ( port == null ) {

                throw new IllegalArgumentException("No suitable port found, tried: " + portsTried);
            }

            serialOut = port.getOutputStream();

            // A particular controller may have a different speed setting.
            // If there's none, we'll fall back to the default of 2400 baud.
            // And all of the controllers supported far use 8N1, so we'll
            // just leave that as a default.

            int portSpeed = 2400;

            Meta controllerMeta = getMeta();

            if (controllerMeta == null) {

                System.err.println("Driver doesn't support meta, port speed is 2400: " + getClass().getName());

            } else {

                try {

                    // speedObject will never be null - we'll get
                    // UnsupportedOperationException instead

                    Object speedObject = controllerMeta.getProperty(META_SPEED);

                    try {

                        String speedString = (String) speedObject;

                        portSpeed = Integer.parseInt(speedString);

                    } catch (Throwable t) {

                        // This is serious enough to blow up - somebody did
                        // a bad job, the speed is hardcoded into the driver

                        throw (IllegalArgumentException) new IllegalArgumentException("Unable to parse property "
                            + META_SPEED + ", object class is " + speedObject.getClass().getName() + ", value is '" + speedObject + "'").initCause(t);
                    }

                } catch (UnsupportedOperationException uoex) {

                    System.err.println("Port speed is 2400, cause: " + uoex.getMessage());
                }
            }

            // Now, if someone has specified an insane speed, that's not my
            // problem... Hopefully, the port implementation will filter it
            // out.

            port.setSerialPortParams(portSpeed,
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);

        } catch ( UnsupportedCommOperationException ucoex ) {

            throw (IOException) new IOException("Unsupported comm operation").initCause(ucoex);
        }

        reset();
    }

    @Override
    protected void checkInit() {

        if ( portName == null ) {

            throw new IllegalStateException("Not initialized");
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

        for ( int offset = 0; offset < buffer.length; offset++ ) {

            serialOut.write(buffer[offset]);
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

            actualPositionChanged();

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
