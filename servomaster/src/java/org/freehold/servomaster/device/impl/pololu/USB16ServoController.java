package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import javax.usb.UsbConfiguration;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;

import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

import org.freehold.servomaster.device.impl.usb.AbstractUsbServoController;

/**
 * <a href="http://pololu.com/products/pololu/0390/" target="_top">Pololu
 * USB 16-Servo Controller</a> controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: USB16ServoController.java,v 1.4 2005-01-21 07:15:06 vtt Exp $
 */
public class USB16ServoController extends AbstractUsbServoController {

    /**
     * Default constructor.
     *
     * Provided for <code>Class.newInstance()</code> to be happy.
     */
    public USB16ServoController() {
    
    }
    
    protected void fillProtocolHandlerMap() {
    
        registerHandler("10c4:803b", new PololuProtocolHandler());
    }
    

    protected SilentProxy createSilentProxy() {
    
        return new PololuSilentProxy();
    }
    
    private void _silentStatusChanged(boolean mode) {
    
        silentStatusChanged(mode);
    }
    
    /**
     * Wrapper for {@link AbstractServoController#exception exception()}
     */
    private void _exception(Throwable t) {
    
        exception(t);
    }
    
    protected class PololuSilentProxy implements SilentProxy {
    
        public synchronized void sleep() {
        
            try {
            
                protocolHandler.silence();
                _silentStatusChanged(false);
                
            } catch ( UsbException usbex ) {
            
                _exception(usbex);
            }
        }
        
        public synchronized void wakeUp() {
        
            // VT: FIXME: Do I really have to do anything? The packet with
            // the proper data gets sent anyway...
            
            try {
            
                reset();
                _silentStatusChanged(true);
                
            } catch ( IOException ioex ) {
            
                _exception(ioex);
            }
        }
    }
    
    /**
     * Pololu USB 16-Servo controller protocol handler.
     */
    protected class PololuProtocolHandler extends UsbProtocolHandler {
    
        private UsbPipe out;
    
        public Servo createServo(ServoController sc, int id) throws IOException {
        
            return new PololuServo(sc, id);
        }

        /**
         * Base class representing all the common (or default) features and
         * properties of the Pololus family of servo controllers.
         */
        protected class PololuMeta extends AbstractMeta {
        
            public PololuMeta() {
            
                properties.put("manufacturer/name", "Pololu Corp.");
                properties.put("manufacturer/URL", "http://www.pololu.com/");
                properties.put("manufacturer/model", getModelName());
                properties.put("controller/maxservos", Integer.toString(getServoCount()));

                features.put("controller/allow_disconnect", new Boolean(true));
                
                features.put("controller/silent", new Boolean(true));
                features.put("controller/protocol/serial", new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));
                
                // VT: FIXME
                
                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
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
            
            /**
             * {@inheritDoc}
             */
            protected Meta createMeta() {
            
                return new PololuServoMeta();
            }
            
            protected void setVelocity(byte newVelocity) throws IOException {
            
                new Error("NOT IMPLEMENTED").printStackTrace();
            }

            protected class PololuServoMeta extends AbstractMeta {
            
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
                            
                                setVelocity(velocity);

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

        public void silence() throws UsbException {
        
            // Send the zero microseconds pulse
            
            //send(new byte[6]);
            
            new Error("NOT IMPLEMENTED").printStackTrace();
        }

        public int getServoCount() {
        
            return 16;
        }
        
        public String getModelName() {
        
            return "USB 16-Servo";
        }
        
        protected Meta createMeta() {
        
            return new PololuMeta();
        }
        
        public void setPosition(int id, double position) throws UsbException {
        
            // Tough stuff, we're dealing with timing now...
            
            PololuServo servo = (PololuServo)servoSet[id];
            
            // One unit is 1/2 of a microsecond
            
            short units = (short)(servo.min_pulse + (position * (servo.max_pulse - servo.min_pulse)));
            
            setAbsolutePosition((byte)id, units);
        }
        
        private void init() throws UsbException {
        
            if ( out == null ) {
            
                if ( theServoController == null ) {
                
                    // There's nothing we can do at this point
                
                    return;
                }
            
                UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                UsbInterface iface = cf.getUsbInterface((byte)0x00);
                
                if ( false ) {
                
                    // VT: FIXME: Verify: with the latest changes, we should've
                    // claimed it already
                    
                    iface.claim();
                }
                
                UsbEndpoint endpoint = null;
                
                for ( Iterator i = iface.getUsbEndpoints().iterator(); i.hasNext(); ) {
                
                    UsbEndpoint e = (UsbEndpoint)i.next();
                    UsbEndpointDescriptor ed = e.getUsbEndpointDescriptor();
                    System.err.println("Endpoint: " + Integer.toHexString(ed.bEndpointAddress() & 0xFF));
                    
                    if ( ed.bEndpointAddress() == 0x03 ) {
                    
                        endpoint = e;
                        break;
                    }
                }
                
                if ( endpoint == null ) {
                
                    throw new UsbException("Can't find endpoint 03");
                }
                
                out = endpoint.getUsbPipe();
                
                if ( !out.isOpen() ) {
                
                    out.open();
                }
            }
        }

        private synchronized void setAbsolutePosition(byte servoId, short units) throws UsbException {
        
            init();
            
            if ( out == null ) {
            
                return;
            }
            
            byte buffer[] = PacketBuilder.setAbsolutePosition(servoId, units);
            
            System.err.println("setAbsolutePosition(" + Integer.toString(servoId) + ", " + units + ")");
            
            UsbIrp message = out.createUsbIrp();
            
            message.setData(buffer);
            
            try {
            
                out.syncSubmit(message);
                
            } catch ( UsbException usbex ) {
            
                // Ouch! The pipe is most probably not valid anymore
                
                out = null;
                throw usbex;
            }
            
            System.err.println("setAbsolutePosition: done");
        }

        public void reset() throws UsbException {
        
            // In case the silent mode was set, we have to resend the positions
            
            //sent = false;
            
            //send();
        }
    }
}