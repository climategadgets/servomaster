package org.freehold.servomaster.device.model.transform;

import org.freehold.servomaster.device.model.Servo;

/**
 * Provides the sine transformation.
 *
 * Be careful with this transformer, because the output values may be
 * outside of 0...1.0 range. You may want to use the {@link ScaleTransformer
 * scale transformer} together with this object to achieve the desired
 * result.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: SineTransformer.java,v 1.1 2001-12-31 23:33:12 vtt Exp $
 */
public class SineTransformer extends AbstractCoordinateTransformer {

    public SineTransformer(Servo target) {
    
        super(target);
    }
    
    protected double transform(double value) {
    
        return Math.sin(value);
    }
    
    protected double resolve(double value) {
    
        return Math.asin(value);
    }
}
