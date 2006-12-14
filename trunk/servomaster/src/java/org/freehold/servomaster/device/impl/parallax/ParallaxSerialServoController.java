package org.freehold.servomaster.device.impl.parallax;

import java.io.IOException;
import java.util.Iterator;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.freehold.servomaster.device.impl.serial.AbstractSerialServoController;
import org.freehold.servomaster.device.impl.serial.SerialMeta;
import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: ParallaxSerialServoController.java,v 1.1 2006-12-14 12:38:28 vtt Exp $
 */
abstract public class ParallaxSerialServoController extends AbstractSerialServoController {

    private final Meta meta = createMeta();

    public ParallaxSerialServoController() {

        // Can't invoke this(null) because this will blow up in doInit()
    }

    public ParallaxSerialServoController(String portName) throws IOException {

        super(portName);
    }

    public final Meta getMeta() {

        return meta;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized final void reset() throws IOException {

        for (Iterator i = getServos(); i.hasNext(); ) {

            ParallaxServo servo = (ParallaxServo) i.next();

            // Default active
            servo.setOn(true);

            // Default is move the servo instantly
            servo.setSpeed((byte) 0x00);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected final SilentProxy createSilentProxy() {

        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * {@inheritDoc}
     */
    protected final synchronized Servo createServo(int id) throws IOException {

        return new ParallaxServo(this, id);
    }


    //TODO: this should really go into the properties and be used in the AbstractSerialServoController
    protected void doInit(String portName) throws IOException {

    	super.doInit(portName);
    	try{
    		port.setSerialPortParams(port.getBaudRate(),SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_2,
                    SerialPort.PARITY_NONE);


    		ParallaxSerialServoController.this.send(PacketBuilder.setParameters(port.getBaudRate()));
    	}catch (UnsupportedCommOperationException e) {
			// TODO: handle exception
		}

    }

    abstract protected Meta createMeta();

    abstract protected class ParallaxMeta extends SerialMeta {

        public ParallaxMeta() {

            properties.put("manufacturer/name", "Parallax Corp.");
            properties.put("manufacturer/URL", "http://www.Parallax.com/");

            // The subclass will have to take care of this
            //properties.put("manufacturer/model", "Serial 16-Servo");

            properties.put("controller/maxservos", Integer.toString(getServoCount()));

            //properties.put(META_SPEED, "38400");
            properties.put(META_SPEED, "2400");


            features.put(META_SILENT, new Boolean(true));

            // VT: FIXME

            properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
            //properties.put("controller/bandwidth", Integer.toString((38400 / 8) / 2));
            properties.put("controller/precision", "2000");

            // Silent timeout is five seconds

            properties.put("controller/silent", "5000");

            // Half milliseconds are default servo range units for the
            // protocol

            properties.put("servo/range/units", "\u03BCs/2");

            // Default range is (500/2)us to (5500/2)us

            properties.put("servo/range/min", "100");
            properties.put("servo/range/max", "1175");
        }
    }

    protected final class ParallaxServo extends SerialServo {

        /**
         * Minimal allowed absolute position for this device.
         */
        final short MIN_PULSE = 100;

        /**
         * Maximum allowed absolute position for this device.
         */
        final short MAX_PULSE = 1175;

        boolean enabled = true;
        boolean reverse = false;
        byte velocity = 0x00;
        short position = 750;
        short min_pulse = MIN_PULSE;
        short max_pulse = MAX_PULSE;

        ParallaxServo(ServoController sc, int id) {

            super(sc, id);
        }

        public final Meta createMeta() {

            return new PololuServoMeta();
        }

        /**
         * {@inheritDoc}
         */
        protected final void sendPosition(double position) throws IOException {

            // This method doesn't need to be synchronized because send() is

            short units = (short)(min_pulse + (position * (max_pulse - min_pulse)));

            System.err.println("Units:"+ units);
            System.err.println("Position:"+ position);
            ParallaxSerialServoController.this.send(PacketBuilder.setAbsolutePosition((byte)id, velocity, units));
        }

        protected final void setOn(boolean on) throws IOException {

            // This method doesn't need to be synchronized because send() is

            //ParallaxSerialServoController.this.send(PacketBuilder.setParameters(port.getBaudRate()));
        }

        protected final void setSpeed(byte speed) throws IOException {

            // This method doesn't need to be synchronized because send() is

            ParallaxSerialServoController.this.send(PacketBuilder.setSpeed((byte)id, speed));
        }

        protected final class PololuServoMeta extends AbstractMeta {

            public PololuServoMeta() {

                // VT: NOTE: According to the documentation, valid values are 500-5500

                properties.put("servo/precision", "2000");

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

                            ioex.printStackTrace();
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

                            ioex.printStackTrace();
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

                            ioex.printStackTrace();
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
