package net.sf.servomaster.device.impl;

import net.sf.servomaster.device.model.ServoController;

/**
 * A hardware servo abstraction.
 *
 * Supports properties and operations specific to hardware servos, as
 * opposed to positioning and transition abstractions provided by {@link
 * AbstractServo AbstractServo}.
 *
 * <p>
 *
 * Note that this class doesn't support the {@link AbstractServo#target target} directly.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
abstract public class HardwareServo extends AbstractServo {

    /**
     * Servo identifier.
     */
    protected final int id;

    /**
     * Create an instance.
     *
     * @param servoController The controller this servo belongs to.
     *
     * @param id Hardware specific servo identifier.
     */
    public HardwareServo(ServoController servoController, int id) {

        super(servoController, null);

        this.id = id;
    }

    @Override
    public final String getName() {

        return Integer.toString(id);
    }
}
