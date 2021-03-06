package net.sf.servomaster.device.impl.i2c;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import net.sf.servomaster.device.impl.AbstractServoController;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * Base class for I2C based servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractI2CServoController extends AbstractServoController {

    public final int deviceAddress;

    protected final I2CDevice device;

    protected AbstractI2CServoController(int busId, int deviceAddress) throws IOException {

        // This will call init() and log the signature for us

        super(Integer.toHexString(busId) + ":" + Integer.toHexString(deviceAddress));

        ThreadContext.push("I2C()");

        try {

            this.deviceAddress = deviceAddress;

            I2CBus bus = I2CFactory.getInstance(busId);
            device = bus.getDevice(deviceAddress);

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            throw new IOException("Oops", t);

        } finally {
            ThreadContext.pop();
        }
    }

    protected AbstractI2CServoController(String portName) throws IOException {
        this(parseBusId(portName), parseDeviceAddress(portName));
    }

    private static int parseBusId(String portName) {
        return Integer.decode(split(portName)[0]);
    }

    private static int parseDeviceAddress(String portName) {
        return Integer.decode(split(portName)[1]);
    }

    private static String[] split(String source) {

        if (source == null || "".equals(source) || source.indexOf(':') == -1) {
            throw new IllegalArgumentException("malformed port name: '" + source + "'");
        }

        return source.split(":");
    }
}
