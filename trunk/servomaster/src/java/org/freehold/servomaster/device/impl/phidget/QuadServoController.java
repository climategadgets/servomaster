package org.freehold.servomaster.device.impl.phidget;

/**
 * Limited subclass of the generic Phidget servo controller driver, able to
 * operate only Quad Servo Controllers (product code 0x38). This class
 * exists because it may be needed sometimes to instantiate the driver when
 * the device is not yet operable, connected or available. Since it is known
 * that the device <strong>is a</strong> Quad Servo, we can safely
 * instantiate it and put it into disconnected mode right away.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2005
 * @version $Id: QuadServoController.java,v 1.3 2006-12-14 09:17:09 vtt Exp $
 */
public class QuadServoController extends PhidgetServoController {

    @Override
    protected void fillProtocolHandlerMap() {

        registerHandler("6c2:38", new ProtocolHandler0x38());
    }
}
