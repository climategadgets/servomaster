package net.sf.servomaster.device.impl.serial.pololu;

import net.sf.servomaster.device.model.Meta;

/**
 * <a href="http://pololu.com/products/pololu/0240/" target="_top">Pololu
 * Serial 16-Servo Controller</a> controller.
 *
 * Should also work with the <a href="http://www.pololu.com/products/pololu/0290/"
 * target="_top">8-Servo</a> controller, but be careful not to access servos
 * with IDs of 8 and up.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
public class Serial16ServoController extends PololuSerialServoController {

    public Serial16ServoController(String portName) {
        super(portName);
    }

    @Override
    public int getServoCount() {
    
        checkInit();

        return 16;
    }
    
    @Override
    protected final Meta createMeta() {
    
        return new Serial16Meta();
    }

    protected class Serial16Meta extends PololuMeta {
    
        public Serial16Meta() {
        
            properties.put("manufacturer/model", "Serial 16-Servo");
        }
    }
}
