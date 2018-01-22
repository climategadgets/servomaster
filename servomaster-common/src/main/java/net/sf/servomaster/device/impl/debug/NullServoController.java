package net.sf.servomaster.device.impl.debug;

import java.io.IOException;

import org.apache.log4j.NDC;

import net.sf.servomaster.device.model.AbstractServoController;
import net.sf.servomaster.device.model.HardwareServo;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.AbstractMeta;
import net.sf.servomaster.device.model.silencer.SilentProxy;

/**
 * A servo controller implementation requiring no hardware and producing no effect other than debug statements.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class NullServoController extends AbstractServoController {

    public NullServoController() throws IOException {
        this("/dev/null");
    }

    public NullServoController(String portName) throws IOException {
        super(portName);

        servoSet = new Servo[getServoCount()];
    }

    @Override
    public int getServoCount() {

        // This would be more than enough to demonstrate the functionality
        return 8;
    }

    @Override
    public void reset() throws IOException {
        logger.info("reset()");
    }

    @Override
    protected void doInit(String portName) throws IOException {

        // Do absolutely nothing.
    }

    @Override
    protected void checkInit() {

        // Do absolutely nothing.
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    protected SilentProxy createSilentProxy() {
        
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    protected Servo createServo(int id) throws IOException {

        return new NullServo(this, id);
    }
    
    @Override
    public Meta getMeta() {
    
        return new NullMeta();
    }
    
    protected class NullMeta extends AbstractMeta {
        
        protected NullMeta() {
            
            properties.put("manufacturer/name", "DIY Zoning Project");
            properties.put("manufacturer/URL", "https://www.homeclimatecontrol.com/");
            properties.put("manufacturer/model", "8 Channel Demo Servo Controller");
            properties.put("controller/maxservos", Integer.toString(getServoCount()));

            properties.put("servo/range/min", "0");
            properties.put("servo/range/max", "1023");
            properties.put("controller/precision", "1024");
        }
    }
    
    protected class NullServo extends HardwareServo {
        
        /**
         * Minimal allowed absolute position for this device.
         */
        final short POSITION_MIN = 0;

        /**
         * Maximum allowed absolute position for this device.
         */
        final short POSITION_MAX = 1023;

        short position_min = POSITION_MIN;
        short position_max = POSITION_MAX;
        
        short position = (short) (position_min + (position_max - position_min) / 2);

        public NullServo(ServoController servoController, int id) {
            super(servoController, id);
        }

        @Override
        protected Meta createMeta() {

            return new NullServoMeta();
        }

        @Override
        protected void setActualPosition(double position) throws IOException {
            
            NDC.push("setActualPosition id=" + id);
            
            try {
                
                this.position = (short)(position_min + (position_max - position_min) * position);
                
                logger.info("requested=" + position);
                logger.info("actual=" + this.position);
                
            } finally {
                NDC.pop();
            }
            
        }
        
        protected class NullServoMeta extends AbstractMeta {
            
            protected NullServoMeta() {
            
                properties.put("servo/precision", Integer.toString(position_max - position_min));
    
                PropertyWriter pwMin = new PropertyWriter() {
    
                    @Override
                    public void set(String key, Object value) {
    
                        short p = Short.parseShort(value.toString());
    
                        if (p < POSITION_MIN || p > POSITION_MAX) {
                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + POSITION_MIN + "..." + POSITION_MAX + ")");
                        }
    
                        if (p >= position_max) {
                            throw new IllegalStateException("position_min (" + p + ") can't be set higher than current position_max (" + position_max + ")");
                        }
    
                        position_min = p;
    
                        try {
    
                            setActualPosition(actualPosition);
    
                        } catch (IOException ioex) {
                            logger.warn("Unhandled exception", ioex);
                        }
    
                        properties.put("servo/precision", Integer.toString(position_max - position_min));
                    }
                };
    
                PropertyWriter pwMax = new PropertyWriter() {
    
                    @Override
                    public void set(String key, Object value) {
    
                        short p = Short.parseShort(value.toString());
    
                        if (p < POSITION_MIN || p > POSITION_MAX) {
                            throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + POSITION_MIN + "..." + POSITION_MAX + ")");
                        }
    
                        if (p <= position_min) {
                            throw new IllegalStateException("position_max (" + p + ") can't be set lower than current position_min (" + position_min + ")");
                        }
    
                        position_max = p;
    
                        try {
    
                            setActualPosition(actualPosition);
    
                        } catch (IOException ioex) {
                            logger.warn("Unhandled exception", ioex);
                        }
    
                        properties.put("servo/precision", Integer.toString(position_max - position_min));
                    }
                };
    
                propertyWriters.put("servo/range/min", pwMin);
                propertyWriters.put("servo/range/max", pwMax);
            }
        }
    }
}
