package org.freehold.servomaster.device.model.transform;

import org.freehold.servomaster.device.model.Servo;

/**
 * Reverses the servo input.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: Reverser.java,v 1.2 2006-12-14 09:17:09 vtt Exp $
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
