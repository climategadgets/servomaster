package net.sf.servomaster.device.impl.serial.parallax;

import java.io.IOException;
import java.util.Iterator;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.serial.AbstractSerialServoController;
import net.sf.servomaster.device.impl.serial.SerialMeta;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * Generic driver for <a href="http://www.parallax.com/" target="_top">Parallax Serial Servo Controllers</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 * @author Copyright &copy; Scott L'Hommedieu 2006
 */
public abstract class ParallaxSerialServoController extends AbstractSerialServoController {

    private final Meta meta = createMeta();
    
    /**
     * Byte buffer being used for all communications without exception
     * in order to minimize chances of causing memory leaks.
     */
    private final byte[] serialBuffer = new byte[8];

    protected ParallaxSerialServoController() {
        // Can't invoke this(null) because this will blow up in doInit()
    }

    protected ParallaxSerialServoController(String portName) throws IOException {
        super(portName);
    }

    @Override
    public final Meta getMeta() {
        return meta;
    }

    @Override
    public final synchronized void reset() throws IOException {

        for (Iterator<Servo> i = getServos(); i.hasNext();) {

            ParallaxServo servo = (ParallaxServo) i.next();

            // Default active
            servo.setOn(true);

            // Default is move the servo instantly
            servo.setSpeed((byte) 0x00);
        }
    }

    @Override
    protected final synchronized Servo createServo(int id) throws IOException {

        return new ParallaxServo(this, id);
    }


    //TODO: this should really go into the properties and be used in the AbstractSerialServoController
    @Override
    protected void doInit(String portName) throws IOException {

        super.doInit(portName);

        try {

            port.setSerialPortParams(port.getBaudRate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_NONE);
            send(PacketBuilderNG.setParameters(serialBuffer, port.getBaudRate()));

        } catch (UnsupportedCommOperationException e) {
            throw new IllegalStateException("Failed to initialize " + portName, e);
        }
    }

    protected abstract Meta createMeta();

    protected abstract class ParallaxMeta extends SerialMeta {

        protected ParallaxMeta() {

            properties.put("manufacturer/name", "Parallax Corp.");
            properties.put("manufacturer/URL", "http://www.parallax.com/");

            // The subclass will have to take care of this
            //properties.put("manufacturer/model", "Serial 16-Servo");

            properties.put("controller/maxservos", Integer.toString(getServoCount()));

            //properties.put(META_SPEED, "38400");
            properties.put(META_SPEED, "2400");


            features.put(META_SILENT, Boolean.valueOf(true));

            // VT: FIXME

            properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
            //properties.put("controller/bandwidth", Integer.toString((38400 / 8) / 2));
            properties.put("controller/precision", "1000");

            // Silent timeout is five seconds

            properties.put("controller/silent", "5000");

            // Half milliseconds are default servo range units for the
            // protocol

            properties.put("servo/range/units", "\u03BCs/2");

            // Default range is (500/2)us to (5500/2)us

            properties.put("servo/range/min", "250");
            properties.put("servo/range/max", "1250");
        }
    }

    protected final class ParallaxServo extends SerialServo {

        /**
         * Minimal allowed absolute position for this device.
         */
        final short MIN_PULSE = 250;

        /**
         * Maximum allowed absolute position for this device.
         */
        final short MAX_PULSE = 1250;

        boolean enabled = true;
        boolean reverse = false;
        byte velocity = 0x00;
        short position = 750;
        short min_pulse = MIN_PULSE;
        short max_pulse = MAX_PULSE;

        ParallaxServo(ServoController sc, int id) {

            super(sc, id);
        }

        @Override
        public Meta createMeta() {

            return new ParallaxServoMeta();
        }

        @Override
        protected void sendPosition(double position) throws IOException {

            // This method doesn't need to be synchronized because send() is

            short units = (short) (min_pulse + position * (max_pulse - min_pulse));

            logger.debug("Units:" + units);
            logger.debug("Position:" + position);

            ParallaxSerialServoController.this.send(PacketBuilderNG.setAbsolutePosition(serialBuffer, (byte) id, velocity, units));
        }

        void setOn(boolean on) throws IOException {

            // This method doesn't need to be synchronized because send() is

            //ParallaxSerialServoController.this.send(PacketBuilder.setParameters(port.getBaudRate()));
        }

        void setSpeed(byte speed) throws IOException {

            velocity = speed;
        }

        protected final class ParallaxServoMeta extends AbstractMeta {

            protected ParallaxServoMeta() {

                // VT: NOTE: According to the documentation, valid values are 250-1250
                properties.put("servo/precision", "1000");

                PropertyWriter pwMin = new PropertyWriter() {

                    @Override
                    public void set(String key, Object value) {

                        short p = Short.parseShort(value.toString());

                        if (p < MIN_PULSE || p > MAX_PULSE) {
                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                        }

                        if (p >= max_pulse) {
                            throw new IllegalStateException("min_pulse (" + p + ") can't be set higher than current max_pulse (" + max_pulse + ")");
                        }

                        min_pulse = p;

                        try {

                            setActualPosition(actualPosition);

                        } catch (IOException ioex) {
                            logger.warn("Unhandled exception", ioex);
                        }

                        properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                    }
                };

                PropertyWriter pwMax = new PropertyWriter() {

                    @Override
                    public void set(String key, Object value) {

                        short p = Short.parseShort(value.toString());

                        if (p < MIN_PULSE || p > MAX_PULSE) {
                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                        }

                        if (p <= min_pulse) {
                            throw new IllegalStateException("max_pulse (" + p + ") can't be set lower than current min_pulse (" + min_pulse + ")");
                        }

                        max_pulse = p;

                        try {

                            setActualPosition(actualPosition);

                        } catch (IOException ioex) {
                            logger.warn("Unhandled exception", ioex);
                        }

                        properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                    }
                };

                PropertyWriter pwVelocity = new PropertyWriter() {

                    @Override
                    public void set(String key, Object value) {

                        velocity = Byte.parseByte(value.toString());

                        try {

                            setSpeed(velocity);

                        } catch (IOException ioex) {
                            logger.warn("Unhandled exception", ioex);
                        }

                        properties.put("servo/velocity", Byte.toString(velocity));
                    }
                };

                propertyWriters.put("servo/range/min", pwMin);
                propertyWriters.put("servo/range/max", pwMax);
                propertyWriters.put("servo/velocity", pwVelocity);
            }
        }
    }
}
