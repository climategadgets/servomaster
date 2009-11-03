package net.sf.servomaster.device.model.transform;

import net.sf.servomaster.device.model.Servo;

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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class LinearTransformer extends AbstractCoordinateTransformer {

    /**
     * Start angle, degrees.
     */
    @SuppressWarnings("unused")
    private final double startAngle;

    /**
     * End angle, degrees.
     */
    @SuppressWarnings("unused")
    private final double endAngle;

    /**
     * Range between the start and end angles, in degrees.
     *
     * Used in the calculations.
     */
    private final double range;

    /**
     * Start offset, equals to the cosine of the start angle.
     *
     * Used in the calculations.
     */
    private final double offset;

    /**
     * Difference between the start and end offset.
     *
     * Used in the calculations.
     */
    private final double scale;

    /**
     * Create an instance supporting 0\u00B0 to 180\u00B0 linear
     * transformation.
     *
     * @param target Servo to provide the transformation for.
     */
    public LinearTransformer(Servo target) {

        this(target, 0.0, 180.0);
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
     * @exception IllegalArgumentException if the ending angle is less or
     * equal than starting angle, or either of them is outside of 0...180
     * range - it doesn't make sense to support that for a servo.
     */
    public LinearTransformer(Servo target, double startAngle, double endAngle) {

        super(target);

        if ( startAngle < 0 || startAngle > 180 ) {

            throw new IllegalArgumentException("Start angle is outside of 0...180 range");
        }

        if ( endAngle < 0 || endAngle > 180 ) {

            throw new IllegalArgumentException("End angle is outside of 0...180 range");
        }

        if ( endAngle <= startAngle ) {

            throw new IllegalArgumentException("End angle is less or equal than start angle");
        }

        this.startAngle = startAngle;
        this.endAngle = endAngle;

        range = endAngle - startAngle;
        offset = Math.cos(Math.toRadians(startAngle));
        scale = -(offset - Math.cos(Math.toRadians(endAngle)));
    }

    @Override
    protected double transform(double value) {

        return Math.toDegrees(Math.acos(value * scale + offset)) / range;
    }

    @Override
    protected double resolve(double position) {

        return (Math.cos(Math.toRadians(position * range)) - offset) / scale;
    }
}
