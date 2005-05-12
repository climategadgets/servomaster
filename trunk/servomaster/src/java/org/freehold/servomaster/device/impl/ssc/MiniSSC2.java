package org.freehold.servomaster.device.impl.ssc;

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
 * <a
 * href="http://www.seetron.com/ssc.htm"
 * target="_top">Mini SSC II</a> servo controller driver.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: MiniSSC2.java,v 1.1 2005-05-12 22:32:01 vtt Exp $
 */
public class MiniSSC2 extends AbstractSerialServoController {

    private final Meta meta = new MiniSSC2Meta();
    
    public MiniSSC2() {
    
        // Can't invoke this(null) because this will blow up in doInit()
    }
    
    public MiniSSC2(String portName) throws IOException {
    
        super(portName);
    }
    
    public final Meta getMeta() {
    
        return meta;
    }

    /**
     * {@inheritDoc}
     */
    public final void reset() throws IOException {
    
        // This controller doesn't require reset
    }
    
    /**
     * {@inheritDoc}
     */
    protected final SilentProxy createSilentProxy() {
    
        throw new UnsupportedOperationException("This controller doesn't support silent operation");
    }
    
    /**
     * {@inheritDoc}
     */
    protected final synchronized Servo createServo(int id) throws IOException {
    
        return new MiniSSC2Servo(this, id);
    }
    
    public final int getServoCount() {
    
        return 8;
    }
    
    protected class MiniSSC2Meta extends SerialMeta {
    
        public MiniSSC2Meta() {
        
            properties.put("manufacturer/name", "Scott Edwards Electronics, Inc.");
            properties.put("manufacturer/URL", "http://www.seetron.com/");
            properties.put("manufacturer/model", "Mini SSC II");
            properties.put("controller/maxservos", Integer.toString(getServoCount()));
            
            // VT: I'm not sure what speed it supports, let's leave it at
            // 2400
            //properties.put("controller/protocol/serial/speed", "38400");
            
            // VT: FIXME
            
            properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
            properties.put("controller/precision", "256");
            
            // VT: FIXME: Unit depends on the way the hardware is configured
            
            properties.put("servo/range/units", "\u03BCs");
            
            // Default range is 0 to 256
            
            properties.put("servo/range/min", "0");
            properties.put("servo/range/max", "256");
        }
    }
    
    protected final class MiniSSC2Servo extends SerialServo {
    
        MiniSSC2Servo(ServoController sc, int id) {
        
            super(sc, id);
        }
        
        public final Meta createMeta() {
        
            return new MiniSSC2ServoMeta();
        }
        
        /**
         * {@inheritDoc}
         */
        protected final void sendPosition(double position) throws IOException {

            byte bPosition = (byte)(position * 255);
            byte[] buffer = new byte[3];
            
            buffer[0] = (byte) 0xFF;
            buffer[1] = (byte) id;
            buffer[2] = bPosition;
            
            MiniSSC2.this.send(buffer);
        }
        
        protected final class MiniSSC2ServoMeta extends AbstractMeta {
        
            public MiniSSC2ServoMeta() {
            
                properties.put("servo/precision", "256");
            }
        }
    }
}
