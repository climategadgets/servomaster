package net.sf.servomaster.device.model.transform;

import net.sf.servomaster.device.model.Servo;

/**
 * Reverses the servo input.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class Reverser extends AbstractCoordinateTransformer {

    public Reverser(Servo target) {

        super(target);
    }

    @Override
    protected double transform(double value) {

        return 1 - value;
    }

    @Override
    protected double resolve(double position) {

        return 1 - position;
    }
}
