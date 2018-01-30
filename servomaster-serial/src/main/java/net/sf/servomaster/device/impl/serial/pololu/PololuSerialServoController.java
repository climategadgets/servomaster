package net.sf.servomaster.device.impl.serial.pololu;

import java.io.IOException;
import java.util.Iterator;

import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.serial.AbstractSerialServoController;
import net.sf.servomaster.device.impl.serial.SerialMeta;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * Generic driver for <a
 * href="http://www.pololu.com/products/pololu/#servcon"
 * target="_top">Pololu Serial Servo Controllers</a>.
 *
 * This code supports <a href="http://www.pololu.com/products/pololu/0290/"
 * target="_top">8-Servo Serial</a>, <a
 * href="http://www.pololu.com/products/pololu/0290/" target="_top">16-Servo
 * Serial</a>, and <a href="http://www.pololu.com/products/pololu/0390/"
 * target="_top">16-Servo USB</a> connected via serial interface.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
abstract public class PololuSerialServoController extends AbstractSerialServoController {

    public PololuSerialServoController(String portName) {
        super(portName);
    }

    @Override
    public synchronized final void reset() throws IOException {

        checkInit();

        for (Iterator<Servo> i = getServos(); i.hasNext(); ) {

            PololuServo servo = (PololuServo) i.next();

            // Default active
            servo.setOn(true);

            // Default is move the servo instantly
            servo.setSpeed((byte) 0x00);
        }
    }

    @Override
    protected final synchronized Servo createServo(int id) throws IOException {

        return new PololuServo(this, id);
    }

    abstract protected Meta createMeta();

    abstract protected class PololuMeta extends SerialMeta {

        public PololuMeta() {

            properties.put("manufacturer/name", "Pololu Corp.");
            properties.put("manufacturer/URL", "http://www.pololu.com/");

            // The subclass will have to take care of this
            //properties.put("manufacturer/model", "Serial 16-Servo");

            properties.put("controller/maxservos", Integer.toString(getServoCount()));
            properties.put("controller/protocol/serial/speed", "38400");

            features.put("controller/silent", Boolean.valueOf(true));

            // VT: FIXME

            properties.put("controller/bandwidth", Integer.toString((38400 / 8) / 2));
            properties.put("controller/precision", "5000");

            // Silent timeout is five seconds

            properties.put("controller/silent", "5000");

            // Half milliseconds are default servo range units for the
            // protocol

            properties.put("servo/range/units", "\u03BCs/2");

            // Default range is (500/2)us to (5500/2)us

            properties.put("servo/range/min", "500");
            properties.put("servo/range/max", "5500");
        }
    }

    protected final class PololuServo extends SerialServo {

        /**
         * Minimal allowed absolute position for this device.
         */
        final short MIN_PULSE = 500;

        /**
         * Maximum allowed absolute position for this device.
         */
        final short MAX_PULSE = 5500;

        boolean enabled = true;
        boolean reverse = false;
        byte velocity = 0x00;
        short position = 3000;
        short min_pulse = MIN_PULSE;
        short max_pulse = MAX_PULSE;

        PololuServo(ServoController sc, int id) {

            super(sc, id);
        }

        @Override
        public final Meta createMeta() {

            return new PololuServoMeta();
        }

        protected final void sendPosition(double position) throws IOException {

            // This method doesn't need to be synchronized because send() is

            short units = (short)(min_pulse + (position * (max_pulse - min_pulse)));

            PololuSerialServoController.this.send(PacketBuilder.setAbsolutePosition((byte)id, units));
        }

        protected final void setOn(boolean on) throws IOException {

            // This method doesn't need to be synchronized because send() is

            PololuSerialServoController.this.send(PacketBuilder.setParameters((byte)id, on));
        }

        protected final void setSpeed(byte speed) throws IOException {

            // This method doesn't need to be synchronized because send() is

            PololuSerialServoController.this.send(PacketBuilder.setSpeed((byte)id, speed));
        }

        protected final class PololuServoMeta extends AbstractMeta {

            public PololuServoMeta() {

                // VT: NOTE: According to the documentation, valid values are 500-5500

                properties.put("servo/precision", "5000");

                PropertyWriter pwMin = new PropertyWriter() {

                    public void set(String key, Object value) {

                        short p = Short.parseShort(value.toString());

                        if ( p < MIN_PULSE || p > MAX_PULSE ) {

                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                        }

                        if ( p >= max_pulse ) {

                            throw new IllegalStateException("min_pulse (" + p + ") can't be set higher than current max_pulse (" + max_pulse + ")");
                        }

                        min_pulse = p;

                        try {

                            setActualPosition(actualPosition);

                        } catch ( IOException ioex ) {

                            logger.warn("Unhandled exception", ioex);
                        }

                        properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                    }
                };

                PropertyWriter pwMax = new PropertyWriter() {

                    public void set(String key, Object value) {

                        short p = Short.parseShort(value.toString());

                        if ( p < MIN_PULSE || p > MAX_PULSE ) {

                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                        }

                        if ( p <= min_pulse ) {

                            throw new IllegalStateException("max_pulse (" + p + ") can't be set lower than current min_pulse (" + min_pulse + ")");
                        }

                        max_pulse = p;

                        try {

                            setActualPosition(actualPosition);

                        } catch ( IOException ioex ) {

                            logger.warn("Unhandled exception", ioex);
                        }

                        properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                    }
                };

                PropertyWriter pwVelocity = new PropertyWriter() {

                    public void set(String key, Object value) {

                        velocity = Byte.parseByte(value.toString());

                        try {

                            setSpeed(velocity);

                        } catch ( IOException ioex ) {

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
