package net.sf.servomaster.device.impl.i2c.pca9685;

import java.io.IOException;

import net.sf.servomaster.device.impl.i2c.AbstractI2CServoController;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.silencer.SilentProxy;

/**
 * Implementation based on {@link Raspberry Pi PWM HAT https://www.adafruit.com/product/2327}
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2017
 */
public class PCA9685ServoController extends AbstractI2CServoController {

    public PCA9685ServoController() {

        throw new IllegalStateException("Not Implemented");
    }

    /**
     * @return 16. We don' know how many of these are actually connected.
     */
    @Override
    public int getServoCount() {
        
        return 16;
    }

    @Override
    public void reset() throws IOException {

        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void doInit(String portName) throws IOException {

        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void checkInit() {

        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public boolean isConnected() {

        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected SilentProxy createSilentProxy() {

        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected Servo createServo(int id) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }
}
