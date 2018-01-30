package net.sf.servomaster.device.impl.serial.pololu;

import net.sf.servomaster.device.model.Meta;

/**
 * <a href="http://pololu.com/products/pololu/0290/" target="_top">Pololu
 * Serial 8-Servo Controller</a> controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
public class Serial8ServoController extends PololuSerialServoController {

    public Serial8ServoController(String portName) {
        super(portName);
    }

    @Override
    public int getServoCount() {

        return 8;
    }

    @Override
    protected final Meta createMeta() {

        return new Serial8Meta();
    }

    protected class Serial8Meta extends PololuMeta {

        public Serial8Meta() {

            properties.put("manufacturer/model", "Serial 8-Servo");
        }
    }
}
