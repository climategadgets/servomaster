package org.freehold.servomaster.device.impl.pololu;

import java.io.IOException;
import java.util.Iterator;

import org.freehold.servomaster.device.impl.serial.AbstractSerialServoController;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

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
    public synchronized Servo getServo(String id) throws IOException {
    
        throw new Error("Not Implemented");
    }
}
