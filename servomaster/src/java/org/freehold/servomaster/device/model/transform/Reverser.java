package org.freehold.servomaster.device.model.transform;

import org.freehold.servomaster.device.model.Servo;

/**
 * Reverses the servo input.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Reverser.java,v 1.1 2001-12-31 23:33:12 vtt Exp $
 */
public class Reverser extends AbstractCoordinateTransformer {

    public Reverser(Servo target) {
    
        super(target);
    }
    
    protected double transform(double value) {
    
        return 1 - value;
    }
    
    protected double resolve(double value) {
    
        return 1 - value;
    }
}
