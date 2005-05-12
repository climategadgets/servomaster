package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.util.Iterator;

import org.freehold.servomaster.device.impl.serial.AbstractSerialServoController;
import org.freehold.servomaster.device.impl.serial.SerialMeta;
import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

/**
 * <a href="http://pololu.com/products/pololu/0240/" target="_top">Pololu
 * Serial 16-Servo Controller</a> controller.
 *
 * Should also work with the <a
 * href="http://www.pololu.com/products/pololu/0290/"
 * target="_top">8-Servo</a> controller, but be careful not to access servos
 * with IDs of 8 and up.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: Serial16ServoController.java,v 1.14 2005-05-12 21:16:12 vtt Exp $
 */
public class Serial16ServoController extends AbstractSerialServoController {

    private final Meta meta = new PololuMeta();
    
    public Serial16ServoController() {
    
        // Can't invoke this(null) because this will blow up in doInit()
    }
    
    public Serial16ServoController(String portName) throws IOException {
    
        super(portName);
    }
    
    public Meta getMeta() {
    
        return meta;
    }

    /**
     * {@inheritDoc}
     */
    public void reset() throws IOException {
    
        System.err.println("reset(): not implemented");
    }
    
    /**
     * {@inheritDoc}
     */
    protected SilentProxy createSilentProxy() {
    
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    /**
     * {@inheritDoc}
     */
    public int getServoCount() {
    
        return 16;
    }
    
    /**
     * {@inheritDoc}
     */
    protected synchronized Servo createServo(int id) throws IOException {
    
        return new PololuServo(this, id);
    }
    
    protected class PololuMeta extends SerialMeta {
    
        public PololuMeta() {
        
            properties.put("manufacturer/name", "Pololu Corp.");
            properties.put("manufacturer/URL", "http://www.pololu.com/");
            properties.put("manufacturer/model", "Serial 16-Servo");
            properties.put("controller/maxservos", Integer.toString(getServoCount()));
            properties.put("controller/protocol/serial/speed", "38400");
            
            features.put("controller/silent", new Boolean(true));
            
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
    
    protected class PololuServo extends SerialServo {
    
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
        
        public Meta createMeta() {
        
            return new PololuServoMeta();
        }
        
        /**
         * {@inheritDoc}
         */
        protected void sendPosition(double position) throws IOException {

            short units = (short)(min_pulse + (position * (max_pulse - min_pulse)));
            
            Serial16ServoController.this.send(PacketBuilder.setAbsolutePosition((byte)id, units));
        }
        
        protected void setVelocity(byte speed) throws IOException {
        
            System.err.println("FIXME: setVelocity()");
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
}
