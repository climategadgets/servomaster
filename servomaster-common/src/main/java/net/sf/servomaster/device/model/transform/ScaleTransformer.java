package net.sf.servomaster.device.model.transform;

import net.sf.servomaster.device.model.Servo;

/**
 * Allows to scale and shift the servo input.
 *
 * The result is <code>(input * scale) + shift</code>.
 *
 * Be careful with this transformer, because the output values may be
 * outside of 0...1.0 range.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class ScaleTransformer extends AbstractCoordinateTransformer {

    private double scale;
    private double shift;

    public ScaleTransformer(Servo target, double scale, double shift) {

        super(target);

        if (scale == 0) {

            throw new IllegalArgumentException("Scale can't be 0");
        }

        this.scale = scale;
        this.shift = shift;
    }

    @Override
    protected double transform(double value) {

        return value * scale + shift;
    }

    @Override
    protected double resolve(double position) {

        return (position - shift) / scale;
    }
}
