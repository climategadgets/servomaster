package net.sf.servomaster.device.impl.i2c;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import net.sf.servomaster.device.impl.AbstractServoController;

/**
 * Base class for I2C based servo controllers.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractI2CServoController extends AbstractServoController {

    /**
     * @see I2CBus
     */
    public final int busId;
    public final int deviceAddress;

    private final I2CBus bus;
    protected final I2CDevice device;

    protected AbstractI2CServoController(int busId, int deviceAddress) throws IOException {

        // This will call init() and log the signature for us

        super(Integer.toHexString(busId) + ":" + Integer.toHexString(deviceAddress));

        ThreadContext.push("I2C()");

        try {

            this.busId = busId;
            this.deviceAddress = deviceAddress;

            bus = I2CFactory.getInstance(busId);
            device = bus.getDevice(deviceAddress);

        } catch (Throwable t) {

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