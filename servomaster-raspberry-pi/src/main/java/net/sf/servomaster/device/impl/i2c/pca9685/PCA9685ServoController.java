package net.sf.servomaster.device.impl.i2c.pca9685;

import java.io.IOException;

import org.apache.log4j.NDC;

import com.pi4j.io.i2c.I2CBus;

import net.sf.servomaster.device.impl.i2c.AbstractI2CServoController;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.silencer.SilentProxy;

/**
 * Implementation based on {@link Raspberry Pi PWM HAT https://www.adafruit.com/product/2327}
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2017
 */
public class PCA9685ServoController extends AbstractI2CServoController {

    /**
     * We don't need to be fancy, just 60Hz would be fine.
     */
    private static final int pwmFrequency = 60;

    private static final int PCA9685_DEFAULT_ADDRESS = 0x40;

    private static final int MODE1 = 0x0;
    private static final int PRESCALE = 0xFE;
    private static final int LED0_ON_L = 0x06;
    private static final int LED0_ON_H = 0x07;
    private static final int LED0_OFF_L = 0x08;
    private static final int LED0_OFF_H = 0x09;

    public PCA9685ServoController() throws IOException {

        this(I2CBus.BUS_1, PCA9685_DEFAULT_ADDRESS);
    }

    public PCA9685ServoController(int busId, int deviceAddress) throws IOException {

        super(busId, deviceAddress);
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

        NDC.push("reset");

        try {

            device.write(MODE1, (byte) 0x0);
            logger.debug("ok");

            // VT: NOTE: assuming that reset clears all settings including PWM frequency

            setPwmFrequency(pwmFrequency);

        } finally {
            NDC.pop();
        }
    }

    // 25MHz, 4096 steps (12-bit)
    private final double preScaleFactor = 25_000_000.0f / 4096.0;

    private void setPwmFrequency(int hz) throws IOException {

        NDC.push("setPwmFrequency");

        try {

            // VT: FIXME: Decide if https://github.com/adafruit/Adafruit-PWM-Servo-Driver-Library/issues/11
            // is important enough, and exact enough
            // hz *= 0.9;

            double preScale = Math.floor((preScaleFactor / (double) hz) - 0.5);

            byte oldmode = (byte) device.read(MODE1);

            // sleep

            byte newmode = (byte) ((oldmode & 0x7F) | 0x10);

            // go to sleep

            device.write(MODE1, newmode);
            device.write(PRESCALE, (byte) (Math.floor(preScale)));
            device.write(MODE1, oldmode);


            try {

                // wait for oscillator

                Thread.sleep(5);

            } catch (Throwable t) {

                throw new IOException("Sleep interrupted", t);
            }

            device.write(MODE1, (byte) (oldmode | 0x80));

            logger.debug(hz + "Hz");

        } finally {
            NDC.pop();
        }
    }

    private void setPWM(int channel, int onAt, int offAt) throws IOException {

        // VT: NOTE: No sanity checks here, private method

        device.write(LED0_ON_L + 4 * channel, (byte) (onAt & 0xFF));
        device.write(LED0_ON_H + 4 * channel, (byte) (onAt >> 8));
        device.write(LED0_OFF_L + 4 * channel, (byte) (offAt & 0xFF));
        device.write(LED0_OFF_H + 4 * channel, (byte) (offAt >> 8));
    }

    @Override
    protected void doInit(String portName) throws IOException {

        // Do absolutely nothing.

        logger.info("init: " + portName);
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
