package net.sf.servomaster.device.model.transform;

import net.sf.servomaster.device.model.Servo;

/**
 * Provides the sine transformation.
 *
 * Be careful with this transformer, because the output values may be
 * outside of 0...1.0 range. You may want to use the {@link ScaleTransformer
 * scale transformer} together with this object to achieve the desired
 * result.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class SineTransformer extends AbstractCoordinateTransformer {

    public SineTransformer(Servo target) {

        super(target);
    }

    @Override
    protected double transform(double value) {

        return Math.sin(value);
    }

    @Override
    protected double resolve(double position) {

        return Math.asin(position);
    }
}
