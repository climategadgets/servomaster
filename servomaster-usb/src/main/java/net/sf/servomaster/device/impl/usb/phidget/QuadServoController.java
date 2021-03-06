package net.sf.servomaster.device.impl.usb.phidget;

/**
 * Limited subclass of the generic Phidget servo controller driver, able to
 * operate only Quad Servo Controllers (product code 0x38). This class
 * exists because it may be needed sometimes to instantiate the driver when
 * the device is not yet operable, connected or available. Since it is known
 * that the device <strong>is a</strong> Quad Servo, we can safely
 * instantiate it and put it into disconnected mode right away.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2002-2018
 */
public class QuadServoController extends PhidgetServoController {

    /**
     * Create an instance connected to the device with the given serial number.
     * 
     * @param serialNumber Serial number of the device to connect to.
     */
    public QuadServoController(String serialNumber) {
        super(serialNumber);
    }
    
    @Override
    protected void fillProtocolHandlerMap() {
        registerHandler("6c2:38", new ProtocolHandler0x38());
    }
}
