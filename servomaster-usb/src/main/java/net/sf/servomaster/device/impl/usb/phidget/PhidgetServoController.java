package net.sf.servomaster.device.impl.usb.phidget;

import java.io.IOException;
import java.util.Iterator;

import javax.usb.UsbConfiguration;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;

import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.usb.AbstractUsbServoController;
import net.sf.servomaster.device.impl.usb.phidget.firmware.Servo8;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * <a
 * href="http://phidgets.com/index.php?module=pncommerce&func=categoryview&CID=7"
 * target="_top">Generic PhidgetServo</a> controller.
 *
 * Detailed documentation to follow.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2002-2018
 */
public class PhidgetServoController extends AbstractUsbServoController {

    /**
     * Create an instance connected to the device with the given serial number.
     * 
     * @param serialNumber Serial number of the device to connect to.
     */
    public PhidgetServoController(String serialNumber) {
        super(serialNumber);
    }

    @Override
    protected void fillProtocolHandlerMap() {

        registerHandler("6c2:38", new ProtocolHandler0x38());
        registerHandler("6c2:39", new ProtocolHandler0x39());
        registerHandler("6c2:3b", new ProtocolHandler0x3B());
        registerHandler("6c2:60", new ProtocolHandler0x60());
    }

    @Override
    protected synchronized void sleep() throws IOException {

        try {

            protocolHandler.silence();

        } catch (UsbException ex) {

            throw new IOException(ex);
        }
    }

    @Override
    protected synchronized void wakeUp() throws IOException {

        // VT: FIXME: Do I really have to do anything? The packet with the proper data gets sent anyway...

        reset();
    }

    /**
     * An abstraction for the object handling the communications with the
     * arbitrary hardware revision of the PhidgetServo controller.
     *
     * <p>
     *
     * For every hardware revision, there will be a separate protocol
     * handler.
     */
    protected abstract class PhidgetProtocolHandler extends UsbProtocolHandler {

        /**
         * Base class representing all the common (or default) features and
         * properties of the Phidgets family of servo controllers.
         */
        protected class PhidgetMeta extends AbstractMeta {

            PhidgetMeta() {

                properties.put("manufacturer/name", "Phidgets, Inc.");
                properties.put("manufacturer/URL", "http://www.phidgets.com/");
                properties.put("manufacturer/model", getModelName());
                properties.put("controller/maxservos", Integer.toString(getServoCount()));

                features.put("controller/allow_disconnect", new Boolean(true));
            }
        }

        public abstract class PhidgetServo extends UsbServo {

            protected PhidgetServo(ServoController sc, int id) throws IOException {

                super(sc, id);
            }

            protected class PhidgetServoMeta extends AbstractMeta {

                PhidgetServoMeta() {

                    // VT: FIXME

                    properties.put("servo/precision", "1500");
                }
            }
        }
    }

    /**
     * Protocol handler for PhidgetServo 3.0 protocol.
     */
    protected abstract class ProtocolHandler003 extends PhidgetProtocolHandler {

        /**
         * Current servo position in device coordinates.
         */
        protected int[] servoPosition = new int[4];

        /**
         * 'sent' flag.
         *
         * This is an optimization measure - since the positions for all the
         * servos are transmitted at once, it would make a lot of sense to
         * ensure that if there was more than one coordinate assignment at about
         * the same time, they all get sent together.
         *
         * @see #bufferPosition
         * @see #send
         */
        private boolean sent;

        /**
         * Byte buffer to compose the packet into.
         *
         * This buffer is not thread safe, but the {@link
         * #send invocation context} makes sure it never gets corrupted.
         */
        protected byte[] buffer = new byte[6];

        protected ProtocolHandler003() {

        }

        @Override
        public void reset() throws UsbException {

            // In case the silent mode was set, we have to resend the positions

            sent = false;

            send();
        }

        /**
         * Compose the USB packet.
         *
         * @return A buffer that represents the positioning command for the
         * hardware.
         */
        public byte[] composeBuffer() {

            // VT: FIXME: Who knows how does the compiler implement this stuff,
            // it might be worth it to replace the division and multiplication
            // with the right and left shift.

            buffer[0]  = (byte)(servoPosition[0] % 256);
            buffer[1]  = (byte)(servoPosition[0] / 256);

            buffer[2]  = (byte)(servoPosition[1] % 256);
            buffer[1] |= (byte)((servoPosition[1] / 256) * 16);

            buffer[3]  = (byte)(servoPosition[2] % 256);
            buffer[4]  = (byte)(servoPosition[2] / 256);

            buffer[5]  = (byte)(servoPosition[3] % 256);
            buffer[4] |= (byte)((servoPosition[3] / 256) * 16);

            return buffer;
        }

        @Override
        public void setPosition(int id, double position) throws IOException, UsbException {

            // Tough stuff, we're dealing with timing now...

            PhidgetServo003 servo = (PhidgetServo003) PhidgetServoController.this.getServo(Integer.toString(id));

            int microseconds = (int)(servo.min_pulse + (position * (servo.max_pulse - servo.min_pulse)));

            // VT: NOTE: We need to know all the servo's positions because
            // they get transmitted in one packet

            bufferPosition(id, microseconds);

            logger.debug("Position:     " + position);
            logger.debug("Microseconds: " + microseconds);
            logger.debug("Buffer:       " + servoPosition[id]);
            
            send();
        }

        /**
         * Compose the USB packet and stuff it down the USB controller.
         *
         * @exception UsbException if there was an I/O error talking to the
         * controller.
         *
         * @see #servoPosition
         * @see #sent
         * @see #bufferPosition
         */
        private synchronized void send() throws UsbException {

            if ( sent ) {

                // They have already sent the positions, relax

                return;
            }

            try {

                byte[] buffer = composeBuffer();
                send(buffer);

                // If there was an exception sending the message, the flag
                // is not cleared. This is OK, since if it was a temporary
                // condition, whoever is about to call send() now will have
                // a shot at properly sending the data.

                sent = true;

            } finally {

                touch();
            }
        }

        protected synchronized void send(byte[] buffer) throws UsbException {

            // theServoController instance can still be null if the driver
            // works in disconnected mode

            if ( theServoController == null ) {

                return;
            }

            byte requestType = (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT
                    |UsbConst.REQUESTTYPE_TYPE_CLASS
                    |UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE);
            byte request = (byte)UsbConst.REQUEST_SET_CONFIGURATION;
            short value = (short)0x0200;
            short index = (short)0x00;


            UsbControlIrp message = theServoController.createUsbControlIrp(requestType, request, value, index);
            message.setData(buffer);

            theServoController.syncSubmit(message);
        }

        /**
         * Remember the servo timing and clear the "sent" flag.
         *
         * @param id Servo number
         *
         * @param position The servo position, in microseconds.
         *
         * @see #sent
         * @see #send
         */
        private synchronized void bufferPosition(int id, int position) {

            servoPosition[id] = position;
            sent = false;
        }

        @Override
        public void silence() throws UsbException {

            // Send the zero microseconds pulse

            send(new byte[6]);
        }

        @Override
        protected Meta createMeta() {

            return new PhidgetMeta003();
        }

        @Override
        public Servo createServo(ServoController sc, int id) throws IOException {

            return new PhidgetServo003(sc, id);
        }

        protected class PhidgetMeta003 extends PhidgetMeta {

            PhidgetMeta003() {

                features.put(Feature.SILENT.name, new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));

                // VT: FIXME

                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
                properties.put("controller/precision", "1500");

                // Silent timeout is five seconds

                properties.put(Feature.SILENT.name, "5000");

                // Milliseconds are default servo range units for v3
                // protocol

                properties.put("servo/range/units", "\u03BCs");

                // Default range is 500us to 2000us

                properties.put("servo/range/min", "500");
                properties.put("servo/range/max", "2000");
            }
        }

        public class PhidgetServo003 extends PhidgetServo {

            private int min_pulse = 500;
            private int max_pulse = 2000;

            public PhidgetServo003(ServoController sc, int id) throws IOException {

                super(sc, id);
            }

            @Override
            protected Meta createMeta() {

                return new PhidgetServoMeta003();
            }

            protected class PhidgetServoMeta003 extends PhidgetServoMeta {

                PhidgetServoMeta003() {

                    PropertyWriter pwMin = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            min_pulse = Integer.parseInt(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.error("Unhandled exception", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };

                    PropertyWriter pwMax = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            max_pulse = Integer.parseInt(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.warn("Unhandled exception", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };

                    propertyWriters.put("servo/range/min", pwMin);
                    propertyWriters.put("servo/range/max", pwMax);
                }
            }
        }
    }

    /**
     * Protocol handler for QuadServo.
     */
    protected class ProtocolHandler0x38 extends ProtocolHandler003 {

        @Override
        protected String getModelName() {

            return "PhidgetServo";
        }

        @Override
        public int getServoCount() {

            return 4;
        }
    }

    /**
     * Protocol handler for UniServo.
     *
     * VT: FIXME: This has to be tested!
     */
    protected class ProtocolHandler0x39 extends ProtocolHandler003 {

        @Override
        protected String getModelName() {

            return "UniServo";
        }

        @Override
        public int getServoCount() {

            return 1;
        }
    }

    /**
     * Protocol handler for initialized AdvancedServo.
     */
    protected class ProtocolHandler0x3B extends PhidgetProtocolHandler {

        private UsbPipe out;

        ProtocolHandler0x3B() {

        }

        @Override
        protected String getModelName() {

            return "PhidgetAdvancedServo";
        }

        @Override
        public void reset() {

            // This will cause a reinitialization

            // VT: FIXME: Hmm... What about the interface that is already
            // claimed?

            out = null;
        }

        @Override
        public int getServoCount() {

            return 8;
        }

        @Override
        public synchronized void setPosition(int id, double position) throws UsbException, IOException {

            PhidgetServo0x3B servo = (PhidgetServo0x3B) PhidgetServoController.this.getServo(Integer.toString(id));

            if ( servo == null ) {

                throw new IllegalStateException("servoSet[" + id + "] is still null");
            }

            send(servo.renderPosition(position));
        }

        @Override
        public void silence() throws UsbException {

            // VT: FIXME

            logger.warn("silence() is not implemented in " + getClass().getName());
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

                    if ( ed.bEndpointAddress() == 0x01 ) {

                        endpoint = e;
                        break;
                    }
                }

                if ( endpoint == null ) {

                    throw new UsbException("Can't find endpoint 82");
                }

                out = endpoint.getUsbPipe();

                if ( !out.isOpen() ) {

                    out.open();
                }
            }
        }

        protected synchronized void send(byte[] buffer) throws UsbException {

            init();

            if ( out == null ) {

                // VT: FIXME: I guess a sent flag like for QuadServo will
                // help

                return;
            }

            UsbIrp message = out.createUsbIrp();

            message.setData(buffer);

            try {

                out.syncSubmit(message);

            } catch ( UsbException usbex ) {

                // Ouch! The pipe is most probably not valid anymore

                out = null;
                throw usbex;
            }
        }

        @Override
        public Servo createServo(ServoController sc, int id) throws IOException {

            return new PhidgetServo0x3B(sc, id);
        }

        public class PhidgetServo0x3B extends PhidgetServo {

            /**
             * Servo position, degrees.
             */
            private float position;

            /**
             * Minimum position offset, degrees.
             *
             * Default is 0.
             */
            private int min_offset = 0;

            /**
             * Maximum position offset, degrees.
             *
             * Default is 180.
             */
            private int max_offset = 180;

            /**
             * Servo velocity, degrees per second.
             */
            private float velocity;

            /**
             * Servo acceleration, degrees per second per second.
             */
            private float acceleration;

            /**
             * Byte buffer to compose the packet into.
             *
             * This buffer is not thread safe, but the invocation context makes sure it never gets corrupted.
             *
             * <p>
             *
             * Output protocol for {@code PhidgetAdvancedServo}
             *
             * <pre>
             * buffer[0] = Index - 1;
             *
             * if (m_blnAssert[Index - 1] == VARIANT_TRUE) buffer[1] = 0xff;
             * ((int *)(buffer+4))[0] = (int)((m_ServoPosition[Index - 1] + 23) * 8109);
             * ((int *)(buffer+4))[1] = (int)((m_MaxVelocity[Index - 1] / 50) * 8109);
             * ((int *)(buffer+4))[2] = (int)((m_Acceleration[Index - 1] / 50) * 8109);
             * </pre>
             *
             * MaxVelocity and Acceleration are measured in degrees/second. (^2)
             */
            private byte[] buffer = new byte[16];

            public byte[] renderPosition(double position) {

                // The requested position is 0 to 1, but the device position
                // is 0 to 180 - have to translate

                // VT: FIXME: adjustment for terminal positions may be
                // required

                this.position = (float)min_offset + (float)(position * (max_offset - min_offset));

                // 8109 is firmware translation factor

                float2byte((this.position + 23) * 8109, buffer, 4);
                float2byte((this.velocity / 50) * 8109, buffer, 8);
                float2byte((this.acceleration / 50) * 8109, buffer, 12);

                //logger.debug("Position: " + this.position);

                /*
                
                StringBuilder sb = new StringBuilder();
                
                sb.append("Buffer: ");
                for ( int idx = 0; idx < buffer.length; idx++ ) {

                    if ( (idx % 4) == 0 && idx > 0 ) {

                        sb.append(" -");
                    }
                    sb.append(" " + Integer.toHexString(buffer[idx] & 0xFF));

                }

                logger.debug(sb.toString());

                 */

                return buffer;
            }

            private void float2byte(float value, byte[] buffer, int offset) {

                //int bits = Float.floatToIntBits(value);
                int bits = (int)value;

                buffer[offset + 0] = (byte)(bits & 0xFF);
                buffer[offset + 1] = (byte)((bits >> 8) & 0xFF);
                buffer[offset + 2] = (byte)((bits >> 16) & 0xFF);
                buffer[offset + 3] = (byte)((bits >> 24) & 0xFF);
            }

            public PhidgetServo0x3B(ServoController sc, int id) throws IOException {

                super(sc, id);

                // We will never touch this again

                buffer[0] = (byte)id;

                // VT: FIXME: This will have to change to support the
                // 'silenced' mode. According to the specs, bit 1 should be
                // set to 1, otherwise the servos will not get the pulse and
                // will go to standby mode. In reality, it has to be 0xFF.

                buffer[1] = (byte)0xFF;

                // Initial values

                velocity = 400;
                acceleration = 2000;
            }

            @Override
            protected Meta createMeta() {

                return new PhidgetServoMeta0x3B();
            }

            protected class PhidgetServoMeta0x3B extends PhidgetServoMeta {

                PhidgetServoMeta0x3B() {

                    PropertyWriter pwMin = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            min_offset = Integer.parseInt(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.warn("Unhandled exception", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_offset - min_offset));
                        }
                    };

                    PropertyWriter pwMax = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            max_offset = Integer.parseInt(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.warn("Unhandled exception", ioex);
                            }

                            properties.put("servo/precision", Integer.toString(max_offset - min_offset));
                        }
                    };

                    PropertyWriter pwVelocity = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            velocity = Float.parseFloat(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.warn("Unhandled exception", ioex);
                            }

                            properties.put("servo/velocity", Float.toString(velocity));
                        }
                    };

                    PropertyWriter pwAcceleration = new PropertyWriter() {

                        @Override
                        public void set(String key, Object value) {

                            acceleration = Float.parseFloat(value.toString());

                            try {

                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {

                                logger.warn("Unhandled exception", ioex);
                            }

                            properties.put("servo/acceleration", Float.toString(acceleration));
                        }
                    };

                    propertyWriters.put("servo/range/min", pwMin);
                    propertyWriters.put("servo/range/max", pwMax);
                    propertyWriters.put("servo/velocity", pwVelocity);
                    propertyWriters.put("servo/acceleration", pwAcceleration);

                    // Default velocity is 400 degrees/sec, default
                    // acceleration is 2000 dev/sec^2.

                    properties.put("servo/velocity", "400");
                    properties.put("servo/acceleration", "2000");
                }
            }
        }

        @Override
        protected Meta createMeta() {

            return new PhidgetMeta0x3B();
        }

        protected class PhidgetMeta0x3B extends PhidgetMeta {

            PhidgetMeta0x3B() {

                features.put(Feature.SILENT.name, new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));

                // NOTE: This controller does indeed have the 'serial' feature,
                // but it is permanently disabled

                features.put("controller/protocol/serial", new Boolean(false));

                // Silent timeout is five seconds

                properties.put(Feature.SILENT.name, "5000");

                // VT: FIXME

                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
                properties.put("controller/precision", "1500");

                // Degrees are default servo range units for 0x3B protocol

                properties.put("servo/range/units", "\u00B0");

                // Default range is 0 to 180 degrees

                properties.put("servo/range/min", "0");
                properties.put("servo/range/max", "180");

                // Default velocity is 400 degrees/sec, default acceleration
                // is 2000 dev/sec^2.

                properties.put("servo/velocity", "400");
                properties.put("servo/acceleration", "2000");
            }
        }
    }

    /**
     * Protocol handler for AdvancedServo SoftPhidget.
     */
    protected class ProtocolHandler0x60 extends PhidgetProtocolHandler {

        ProtocolHandler0x60() {

        }

        @Override
        public boolean isBootable() {

            return true;
        }

        @Override
        protected String getModelName() {

            return "SoftPhidget";
        }

        @Override
        public void reset() {

            throw new IllegalAccessError("Operation not supported");
        }

        @Override
        public int getServoCount() {

            throw new IllegalAccessError("Operation not supported");
        }

        @Override
        public synchronized void setPosition(int id, double position) throws UsbException {

            throw new IllegalAccessError("Operation not supported");
        }

        @Override
        public void silence() throws UsbException {

            throw new IllegalAccessError("Operation not supported");
        }

        /*
        private void init() throws IOException, UsbException {

            throw new IllegalAccessError("Operation not supported");
        }
        */

        protected synchronized void send(byte[] buffer) throws IOException, UsbException {

            throw new IllegalAccessError("Operation not supported");
        }

        @Override
        public Servo createServo(ServoController sc, int id) throws IOException {

            throw new IllegalAccessError("Operation not supported");
        }

        @Override
        protected Meta createMeta() {

            throw new IllegalAccessError("Operation not supported");
        }

        /**
         * Boot the SoftPhidget.
         *
         * @param target Device to boot.
         */
        @Override
        public void boot(UsbDevice target) throws UsbException {

            logger.info("Booting SoftPhidget");

            try {

                UsbConfiguration cf = target.getActiveUsbConfiguration();
                UsbInterface iface = cf.getUsbInterface((byte)0x00);
                UsbEndpoint endpoint = iface.getUsbEndpoint((byte)0x01);

                Firmware fw = new Servo8();
                byte[] buffer = fw.get();

                logger.info("Firmware size " + buffer.length + ", header");

                StringBuilder sb = new StringBuilder();
                
                for ( int offset = 0; offset < 4; offset++ ) {

                    sb.append(" 0x");

                    String hex = Integer.toHexString(buffer[offset]&0xFF);

                    if ( hex.length() == 1 ) {

                        hex = "0" + hex;
                    }

                    sb.append(hex.toUpperCase());
                }

                logger.info(sb.toString());

                UsbPipe pipe = endpoint.getUsbPipe();
                UsbIrp message = pipe.createUsbIrp();

                message.setData(buffer);

                iface.claim();
                pipe.open();
                pipe.syncSubmit(message);
                pipe.close();
                iface.release();

            } catch (UsbException  usbex) {

                // Analyze the exception. It's possible that the device
                // announced itself removed by now and we're getting an
                // exception because of that

                // VT: FIXME: This depends heavily on jUSB code, hope it can be
                // made more clear later

                String message = usbex.getMessage();

                if ( message != null ) {

                    if (message.equals("writeBulk -- USB device has been removed -- No such device [19]")) {

                        // Yes, this is a classical symptom

                        logger.warn("Boot may have failed: device prematurely departed; ignored", usbex);
                    }
                }

            } catch (Throwable t) {

                logger.error("Boot failed:", t);

                // Since there's nothing we can do about it, we'll just proceed
                // as usual. The device either will not be found at all, or will
                // be found as SoftPhidget again, which is taken care of.
            }

            try {

                // The SoftPhidget is supposed to boot in about 200ms, let's be
                // paranoid

                Thread.sleep(5000);

            } catch ( InterruptedException iex ) {

            }
        }
    }
}
