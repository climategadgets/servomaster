package org.freehold.servomaster.device.model.transform;

import org.freehold.servomaster.device.model.Servo;

/**
 * Provides the cosine transformation.
 *
 * Be careful with this transformer, because the output values may be
 * outside of 0...1.0 range. You may want to use the {@link ScaleTransformer
 * scale transformer} together with this object to achieve the desired
 * result.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: CosineTransformer.java,v 1.2 2006-12-14 09:17:09 vtt Exp $
 */
public class CosineTransformer extends AbstractCoordinateTransformer {

    public CosineTransformer(Servo target) {

        super(target);
    }

    @Override
    protected double transform(double value) {

        return Math.cos(value);
    }

    @Override
    protected double resolve(double position) {

        return Math.acos(position);
    }
}
