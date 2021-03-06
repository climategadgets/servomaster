package net.sf.servomaster.device.impl.i2c.pca9685;

import com.pi4j.io.i2c.I2CBus;
import net.sf.servomaster.device.impl.AbstractMeta;
import net.sf.servomaster.device.impl.HardwareServo;
import net.sf.servomaster.device.impl.i2c.AbstractI2CServoController;
import net.sf.servomaster.device.impl.i2c.I2CMeta;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * Implementation based on <a href="https://www.adafruit.com/product/2327">Raspberry Pi PWM HAT</a>
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class PCA9685ServoController extends AbstractI2CServoController {

    /**
     * Minimal allowed absolute position for this device.
     *
     * VT: FIXME: Figure out acceptable value after the range unit is figured out. Current value is well beyond sane.
     */
    private static final short MIN_PULSE = 25;

    /**
     * Maximum allowed absolute position for this device.
     *
     * VT: FIXME: Figure out acceptable value after the range unit is figured out. Current value is well beyond sane.
     */
    private static final short MAX_PULSE = 625;

    /**
     * We don't need to be fancy, just 60Hz would be fine.
     */
    private static final int pwmFrequency = 60;

    private static final String PCA9685_DEFAULT_PORT = "1:0x40";
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

    public PCA9685ServoController(String portName) throws IOException {
        super(normalize(portName));
    }

    private static String normalize(String portName) {

        if (portName == null || "".equals(portName)) {

            LogManager.getLogger(PCA9685ServoController.class).warn("null or empty port given, using default " + PCA9685_DEFAULT_PORT);
            return PCA9685_DEFAULT_PORT;
        }

        return portName;
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

        ThreadContext.push("reset");

        try {

            device.write(MODE1, (byte) 0x0);
            logger.debug("ok");

            // VT: NOTE: assuming that reset clears all settings including PWM frequency

            setPwmFrequency(pwmFrequency);

        } finally {
            ThreadContext.pop();
        }
    }

    // 25MHz, 4096 steps (12-bit)
    private final double preScaleFactor = 25_000_000.0f / 4096.0;

    private void setPwmFrequency(int hz) throws IOException {

        ThreadContext.push("setPwmFrequency");

        try {

            // VT: FIXME: Decide if https://github.com/adafruit/Adafruit-PWM-Servo-Driver-Library/issues/11
            // is important enough, and exact enough
            // hz *= 0.9;

            double preScale = Math.floor((preScaleFactor / hz) - 0.5);

            byte oldmode = (byte) device.read(MODE1);

            // sleep

            byte newmode = (byte) ((oldmode & 0x7F) | 0x10);

            // go to sleep

            device.write(MODE1, newmode);
            device.write(PRESCALE, (byte) Math.floor(preScale));
            device.write(MODE1, oldmode);

            try {

                // wait for oscillator

                Thread.sleep(5);

            } catch (Throwable t) { // NOSONAR Consequences have been considered

                throw new IOException("Sleep interrupted", t);
            }

            device.write(MODE1, (byte) (oldmode | 0x80));

            logger.debug("{}Hz", hz);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Configure PWM pulse for an individual servo.
     *
     * @param channel Servo id (0..15).
     * @param onAt Turn the signal on this many μs after the start of the pulse (0..4095, 2^12 values).
     * @param offAt Turn the signal off this many μs after the start of the pulse  (0..4095, 2^12 values).
     */
    private void setPWM(int channel, int onAt, int offAt) throws IOException {

        ThreadContext.push("setPWM");

        try {

            logger.debug("channel={}, on={}, off={}", channel, onAt, offAt);

            // VT: NOTE: Arguments are calculation results, sanity checks are needed

            if (channel < 0 || channel > getServoCount()) {
                throw new IllegalArgumentException("servo channel (" + channel + ") out of range, valid values are 0.." + getServoCount());
            }

            checkOffset("on", onAt);
            checkOffset("of", offAt);

            device.write(LED0_ON_L + 4 * channel, (byte) (onAt & 0xFF));
            device.write(LED0_ON_H + 4 * channel, (byte) (onAt >> 8));
            device.write(LED0_OFF_L + 4 * channel, (byte) (offAt & 0xFF));
            device.write(LED0_OFF_H + 4 * channel, (byte) (offAt >> 8));

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the {@code offset} value is between 0..4095 inclusive.
     *
     * @param marker Error marker.
     * @param offset Value to check.
     */
    private void checkOffset(String marker, int offset) {

        if (offset < 0 || offset > 4095) {
            throw new IllegalArgumentException("'" + marker + "' offset is out of range (valid values 0...4095)");
        }
    }

    @Override
    protected void doInit() throws IOException {

        // Do absolutely nothing.

        logger.info("init: {}", portName);
    }

    @Override
    public boolean isConnected() {

        checkInit();
        // VT: FIXME: It remains to be seen how this check can be performed [reliably].

        logger.warn("FIXME: isConnected() returning unconditional true");
        return true;
    }

    @Override
    protected Servo createServo(int id) throws IOException {
        return new PCA9685Servo(this, id);
    }

    @Override
    protected Meta createMeta() {
        return new PCA9685Meta();
    }

    protected class PCA9685Meta extends I2CMeta {
        protected PCA9685Meta() {

            properties.put("manufacturer/name", "Adafruit");
            properties.put("manufacturer/URL", "https://www.adafruit.com/");
            properties.put("manufacturer/model", "16-Channel PWM/Servo HAT for Raspberry Pi");
            properties.put("controller/maxservos", Integer.toString(getServoCount()));

            // VT: FIXME: Servo range unit is weirdly defined, need to clarify
            // This will finally lead to the right answer: https://forums.adafruit.com/viewtopic.php?f=19&t=71522

            // properties.put("servo/range/units", "\u03BCs");

            properties.put("servo/range/units", "tick");

            properties.put("servo/range/min", Integer.toString(MIN_PULSE));
            properties.put("servo/range/max", Integer.toString(MAX_PULSE));
            properties.put("controller/precision", Integer.toString(MAX_PULSE - MIN_PULSE));
        }
    }

    private final class PCA9685Servo extends HardwareServo {

        short minPulse = MIN_PULSE;
        short maxPulse = MAX_PULSE;

        public PCA9685Servo(PCA9685ServoController sc, int id) {
            super(sc, id);
            setPosition(0.5);
        }

        @Override
        protected Meta createMeta() {
            return new PCA9685ServoMeta();
        }

        @Override
        protected void setActualPosition(double position) throws IOException {

            ThreadContext.push("setActualPosition id=" + id);

            try {

                checkPosition(position);

                setPWM(id, 0, (int) (minPulse + position * (maxPulse - minPulse)));

                actualPosition = position;

                actualPositionChanged(actualPosition);

                touch();

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        protected void sleep() throws IOException {

            ThreadContext.push("sleep:" + id);

            try {

                // Just disable the pulse and be done with it
                setPWM(id, 0, 0);

                logger.debug("sleeping now");

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        protected void wakeUp() throws IOException {

            ThreadContext.push("wakeUp:" + id);

            try {

                setActualPosition(getPosition());

            } finally {
                ThreadContext.pop();
            }
        }

        protected final class PCA9685ServoMeta extends AbstractMeta {

            PCA9685ServoMeta() {

                // Silent timeout is five seconds

                properties.put("servo/silent", "5000");
                features.put(META_SILENT, Boolean.TRUE);

                properties.put("servo/precision", Integer.toString(maxPulse - minPulse));

                PropertyWriter pwMin = (key, value) -> {

                    var p = Short.parseShort(value.toString());

                    if (p < MIN_PULSE || p > MAX_PULSE) {
                        throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                    }

                    if (p >= maxPulse) {
                        throw new IllegalStateException("min_pulse (" + p + ") can't be set higher than current max_pulse (" + maxPulse + ")");
                    }

                    minPulse = p;

                    try {

                        setActualPosition(actualPosition);

                    } catch (IOException ioex) {
                        logger.warn("Unhandled exception", ioex);
                    }

                    properties.put("servo/precision", Integer.toString(maxPulse - minPulse));
                };

                PropertyWriter pwMax = (key, value) -> {

                    var p = Short.parseShort(value.toString());

                    if (p < MIN_PULSE || p > MAX_PULSE) {
                        throw new IllegalArgumentException("Value (" + p + ") is outside of valid range (" + MIN_PULSE + "..." + MAX_PULSE + ")");
                    }

                    if (p <= minPulse) {
                        throw new IllegalStateException("max_pulse (" + p + ") can't be set lower than current min_pulse (" + minPulse + ")");
                    }

                    maxPulse = p;

                    try {

                        setActualPosition(actualPosition);

                    } catch (IOException ioex) {
                        logger.warn("Unhandled exception", ioex);
                    }

                    properties.put("servo/precision", Integer.toString(maxPulse - minPulse));
                };

                propertyWriters.put("servo/range/min", pwMin);
                propertyWriters.put("servo/range/max", pwMax);
            }
        }
    }
}
