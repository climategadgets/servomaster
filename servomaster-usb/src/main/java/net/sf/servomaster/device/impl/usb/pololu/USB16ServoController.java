package net.sf.servomaster.device.impl.usb.pololu;

import java.io.IOException;
import java.util.Iterator;

import javax.usb.UsbConfiguration;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;

import org.apache.logging.log4j.ThreadContext;

import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.usb.AbstractUsbServoController;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * <a href="http://pololu.com/products/pololu/0390/" target="_top">Pololu USB 16-Servo Controller</a> controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
public class USB16ServoController extends AbstractUsbServoController {

    protected USB16ServoController(String portName) {
        super(portName);
    }

    @Override
    protected void fillProtocolHandlerMap() {

        registerHandler("10c4:803b", new PololuProtocolHandler());
    }

    @Override
    public synchronized void sleep() throws IOException {

        try {

            protocolHandler.silence();
            USB16ServoController.this.silentStatusChanged(false);

        } catch ( UsbException ex ) {

            throw new IOException(ex);
        }
    }

    @Override
    public synchronized void wakeUp() throws IOException {

        // VT: FIXME: Do I really have to do anything? The packet with
        // the proper data gets sent anyway...

        try {

            reset();
            USB16ServoController.this.silentStatusChanged(true);

        } catch ( IOException ioex ) {

            USB16ServoController.this.exception(ioex);
        }
    }

    /**
     * Pololu USB 16-Servo controller protocol handler.
     */
    protected class PololuProtocolHandler extends UsbProtocolHandler {

        private UsbPipe out;

        @Override
        public Servo createServo(ServoController sc, int id) throws IOException {

            return new PololuServo(sc, id);
        }

        /**
         * Base class representing all the common (or default) features and
         * properties of the Pololus family of servo controllers.
         */
        protected class PololuMeta extends AbstractMeta {

            protected PololuMeta() {

                properties.put("manufacturer/name", "Pololu Corp.");
                properties.put("manufacturer/URL", "http://www.pololu.com/");
                properties.put("manufacturer/model", getModelName());
                properties.put("controller/maxservos", Integer.toString(getServoCount()));

                features.put("controller/allow_disconnect", new Boolean(true));

                features.put(Feature.SILENT.name, new Boolean(true));
                features.put("controller/protocol/serial", new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));

                // VT: FIXME

                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
                properties.put("controller/precision", "5000");

                // Silent timeout is five seconds

                properties.put(Feature.SILENT.name, "5000");

                // Half milliseconds are default servo range units for the
                // protocol

                properties.put("servo/range/units", "\u03BCs/2");

                // Default range is (500/2)us to (5500/2)us

                properties.put("servo/range/min", "500");
                properties.put("servo/range/max", "5500");

            }
        }

        public class PololuServo extends UsbServo {

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

            protected PololuServo(ServoController sc, int id) throws IOException {

                super(sc, id);
            }

            @Override
            protected Meta createMeta() {

                return new PololuServoMeta();
            }

            protected void setVelocity(byte newVelocity) throws IOException {

                logger.debug("NOT IMPLEMENTED", new Error("Trace"));
            }

            protected class PololuServoMeta extends AbstractMeta {

                protected PololuServoMeta() {

                    // VT: NOTE: According to the documentation, valid values are 500-5500

                    properties.put("servo/precision", "5000");

                    PropertyWriter pwMin = new PropertyWriter() {

                        @Override
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

                                logger.error("setActualPosition(" + actualPosition + ") failed", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };

                    PropertyWriter pwMax = new PropertyWriter() {

                        @Override
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

                                logger.error("setActualPosition(" + actualPosition + ") failed", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };

                    PropertyWriter pwVelocity = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            velocity = Byte.parseByte(value.toString());

                            try {

                                setVelocity(velocity);

                            } catch ( IOException ioex ) {

                                logger.error("setVelocity(" + velocity + ") failed", ioex);
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

        @Override
        public void silence() throws UsbException {

            // Send the zero microseconds pulse

            //send(new byte[6]);

            logger.debug("NOT IMPLEMENTED", new Error("Trace"));
        }

        @Override
        public int getServoCount() {

            return 16;
        }

        @Override
        public String getModelName() {

            return "USB 16-Servo";
        }

        @Override
        protected Meta createMeta() {

            return new PololuMeta();
        }

        @Override
        public void setPosition(int id, double position) throws UsbException, IOException {

            // Tough stuff, we're dealing with timing now...

            PololuServo servo = (PololuServo) USB16ServoController.this.getServo(Integer.toString(id));

            // One unit is 1/2 of a microsecond

            short units = (short)(servo.min_pulse + (position * (servo.max_pulse - servo.min_pulse)));

            setAbsolutePosition((byte)id, units);
        }

        @SuppressWarnings("unchecked")
        private void init() throws UsbException {

            if ( out == null ) {

                if ( theServoController == null ) {

                    // There's nothing we can do at this point

                    return;
                }

                UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                UsbInterface iface = cf.getUsbInterface((byte)0x00);

                // VT: FIXME: Verify: with the latest changes, we should've
                // claimed it already

                //iface.claim();

                UsbEndpoint endpoint = null;

                for ( Iterator<UsbEndpoint> i = iface.getUsbEndpoints().iterator(); i.hasNext(); ) {

                    UsbEndpoint e = i.next();
                    UsbEndpointDescriptor ed = e.getUsbEndpointDescriptor();
                    logger.info("Endpoint: " + Integer.toHexString(ed.bEndpointAddress() & 0xFF));

                    if ( ed.bEndpointAddress() == 0x03 ) {

                        endpoint = e;
                        break;
                    }
                }

                if ( endpoint == null ) {

                    throw new UsbException("Can't find endpoint 0x03");
                }

                out = endpoint.getUsbPipe();

                if ( !out.isOpen() ) {

                    out.open();
                }
            }
        }

        private synchronized void setAbsolutePosition(byte servoId, short units) throws UsbException {

            ThreadContext.push("setAbsolutePosition");

            try {

                init();

                if ( out == null ) {

                    return;
                }

                byte[] buffer = PacketBuilder.setAbsolutePosition(servoId, units);

                logger.debug("(" + Integer.toString(servoId) + ", " + units + ")");

                UsbIrp message = out.createUsbIrp();

                message.setData(buffer);

                try {

                    out.syncSubmit(message);

                } catch ( UsbException usbex ) {

                    // Ouch! The pipe is most probably not valid anymore

                    out = null;
                    throw usbex;
                }

                logger.debug("done");

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void reset() throws UsbException {

            // In case the silent mode was set, we have to resend the positions

            //sent = false;

            //send();
        }
    }
}
