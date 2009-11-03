package net.sf.servomaster.device.model.transform;

import net.sf.servomaster.device.model.Servo;

/**
 * Allows to limit the servo input.
 *
 * In real life applications it often happens that mechanical limits of the
 * apparatus controlled by the servo are narrower than the range of servo
 * movement. This transfomer addresses the problem. The input range will
 * still be 0..1, but the servo range will be limited to preset values.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public class LimitTransformer extends AbstractCoordinateTransformer {

    private double min;
    private double max;

    /**
     * Create an instance.
     *
     * @param target Servo to control.
     *
     * @param min Minimum allowed target servo position. Can't be less than
     * 0, or greater or equal than <code>max</code>.
     *
     * @param max Maximum allowed target servo position. Can't be greater
     * than 1, or less or equal than <code>min</code>.
     */
    public LimitTransformer(Servo target, double min, double max) {

        super(target);

        if (min >= max) {

            throw new IllegalArgumentException("min (" + min + ") can't be greater than or equal to max (" + max + ")");
        }

        this.min = min;
        this.max = max;
    }

    /**
     * {@inheritDoc}
     */
    protected double transform(double value) {

        return min + (max - min) * value;
    }

    /**
     * {@inheritDoc}
     */
    protected double resolve(double value) {

        return (value - min) / (max - min);
    }
}
