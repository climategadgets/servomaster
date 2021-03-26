package net.sf.servomaster.device.impl.serial.ssc;

import java.io.IOException;

import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.serial.AbstractSerialServoController;
import net.sf.servomaster.device.impl.serial.SerialMeta;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * <a href="http://www.seetron.com/ssc.htm" target="_top">Mini SSC II</a> servo controller driver.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2005-2018
 */
public class MiniSSC2 extends AbstractSerialServoController {

    public MiniSSC2(String portName) throws IOException {
        super(portName);
    }
    
    @Override
    protected final Meta createMeta() {
    
        return new MiniSSC2Meta();
    }

    @Override
    public final void reset() throws IOException {

        checkInit();
        // This controller doesn't require reset
    }
    
    @Override
    protected final synchronized Servo createServo(int id) throws IOException {
    
        return new MiniSSC2Servo(this, id);
    }
    
    @Override
    public final int getServoCount() {
    
        checkInit();
        return 8;
    }
    
    protected class MiniSSC2Meta extends SerialMeta {
    
        public MiniSSC2Meta() {
        
            properties.put("manufacturer/name", "Scott Edwards Electronics, Inc.");
            properties.put("manufacturer/URL", "http://www.seetron.com/");
            properties.put("manufacturer/model", "Mini SSC II");
            properties.put("controller/maxservos", Integer.toString(getServoCount()));
            
            // VT: I'm not sure what speed it supports, let's leave it at 2400
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
        
        @Override
        public final Meta createMeta() {
        
            return new MiniSSC2ServoMeta();
        }
        
        @Override
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
