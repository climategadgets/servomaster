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
 * <p>
 *
 * <strong>NOTE:</strong>
 *
 * In the current form, the linear transformer supports only 0\u00B0 to
 * 180\u00B0 transformation. However, it will be fixed soon.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: LinearTransformer.java,v 1.3 2002-01-03 22:10:01 vtt Exp $
 */
public class LinearTransformer extends AbstractCoordinateTransformer {

    /**
     * Create an instance supporting 0\u00B0 to 180\u00B0 linear
     * transformation.
     *
     * @param target Servo to provide the transformation for.
     */
    public LinearTransformer(Servo target) {
    
        super(target);
    }
    
    /**
     * Create an instance supporting a linear transformation on arbitrary
     * angle range.
     *
     * @param target Servo to provide the transformation for.
     *
     * @param startAngle Starting rotational angle, degrees.
     *
     * @param endAngle Ending rotational angled, degrees.
     *
     * @exception IllegalArgumentException if the ending angle is less than
     * starting angle, or either of them is outside of 0...180 range - it
     * doesn't make sense to support that for a servo.
     */
    public LinearTransformer(Servo target, double startAngle, double endAngle) {
    
        super(target);
        
        throw new Error("Not Implemented");
    }
    
    protected double transform(double value) {
        
        return Math.toDegrees(Math.acos((value * -2)  + 1))/180.0;
    }
    
    protected double resolve(double value) {
    
        return (Math.cos(Math.toRadians(value * 180)) - 1) / -2;
    }
}
