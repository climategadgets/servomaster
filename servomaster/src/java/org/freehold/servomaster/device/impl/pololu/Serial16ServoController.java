package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.util.Iterator;

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
 * @version $Id: Serial16ServoController.java,v 1.15 2005-05-12 21:36:37 vtt Exp $
 */
public class Serial16ServoController extends PololuSerialServoController {

    public Serial16ServoController() {
    
        // Can't invoke this(null) because this will blow up in doInit()
    }
    
    public Serial16ServoController(String portName) throws IOException {
    
        super(portName);
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
    protected final Meta createMeta() {
    
        return new Serial16Meta();
    }

    protected class Serial16Meta extends PololuMeta {
    
        public Serial16Meta() {
        
            properties.put("manufacturer/model", "Serial 16-Servo");
        }
    }
}