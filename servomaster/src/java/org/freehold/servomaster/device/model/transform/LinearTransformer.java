package org.freehold.servomaster.device.model.transform;

import org.freehold.servomaster.device.model.Servo;

/**
 * Provides the linear transformation.
 *
 * Normally, the servo input directly corresponds to the rotation angle. For
 * a lot of real life applications, the angular movement of the servo will
 * be transformed into the linear movement of the controlled body, so the
 * abstraction supporting a transparent transformation is going to be
 * extremely useful.
 *
 * <p>
 *
 * Actually, this transformation can be achieved with the stack consisting
 * of the scale, cosine and arccosine transformers, but it is rather
 * cumbersome, plus adds unnecessary overhead, so it was decided to provide
 * a separate implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: LinearTransformer.java,v 1.2 2002-01-01 01:42:13 vtt Exp $
 */
public class LinearTransformer extends AbstractCoordinateTransformer {

    public LinearTransformer(Servo target) {
    
        super(target);
    }
    
    protected double transform(double value) {
        
        return Math.toDegrees(Math.acos((value * -2)  + 1))/180.0;
    }
    
    protected double resolve(double value) {
    
        return (Math.cos(Math.toRadians(value * 180)) - 1) / -2;
    }
}
