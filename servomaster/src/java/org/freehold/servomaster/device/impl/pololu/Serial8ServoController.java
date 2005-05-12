package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.util.Iterator;

import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

/**
 * <a href="http://pololu.com/products/pololu/0290/" target="_top">Pololu
 * Serial 8-Servo Controller</a> controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: Serial8ServoController.java,v 1.1 2005-05-12 21:36:37 vtt Exp $
 */
public class Serial8ServoController extends PololuSerialServoController {

    public Serial8ServoController() {
    
        // Can't invoke this(null) because this will blow up in doInit()
    }
    
    public Serial8ServoController(String portName) throws IOException {
    
        super(portName);
    }

    /**
     * {@inheritDoc}
     */
    public int getServoCount() {
    
        return 8;
    }

    /**
     * {@inheritDoc}
     */
    protected final Meta createMeta() {
    
        return new Serial8Meta();
    }

    protected class Serial8Meta extends PololuMeta {
    
        public Serial8Meta() {
        
            properties.put("manufacturer/model", "Serial 8-Servo");
        }
    }
}
