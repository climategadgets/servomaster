package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.util.Iterator;

import org.freehold.servomaster.device.impl.serial.AbstractSerialServoController;
import org.freehold.servomaster.device.model.Servo;
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
 * @version $Id: Serial16ServoController.java,v 1.4 2005-01-14 03:27:40 vtt Exp $
 */
public class Serial16ServoController extends AbstractSerialServoController {

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
    
        throw new Error("Not Implemented");
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
    
        throw new Error("Not Implemented");
    }
}
