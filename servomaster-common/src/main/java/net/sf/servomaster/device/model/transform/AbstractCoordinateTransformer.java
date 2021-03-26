package net.sf.servomaster.device.model.transform;

import java.io.IOException;
import java.util.concurrent.Future;

import net.sf.servomaster.device.impl.AbstractServo;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * The coordinate transformer skeleton.
 *
 * Provides the 'wrapper' for the servo being controlled, as well as the
 * means to perform the actual coordinate transformation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractCoordinateTransformer extends AbstractServo {

    /**
     * Create the instance.
     *
     * @param target Servo to control. May be another coordinate transformer
     * as well.
     */
    protected AbstractCoordinateTransformer(Servo target) {

        super(null, target);

        if (getTarget() == null) {

            throw new IllegalArgumentException("target can't be null");
        }
    }

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    @Override
    public String getName() {

        return getTarget().getName();
    }

    /**
     * Set the position, applying the {@link #transform coordinate
     * transofmation}.
     *
     * @param position Position to set, between 0 and 1.0.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    @Override
    public Future<TransitionStatus> setPosition(double position) {

        return getTarget().setPosition(transform(position));
    }

    /**
     * Get the position, applying the {@link #resolve reverse coordinate
     * transofmation}.
     *
     * @return The position that has previously been {@link #setPosition
     * set}. Note that this may not be the actual position of the servo at
     * the time, if the set operation was requested to be smooth. Use {@link
     * #getActualPosition getActualPosition()} to obtain the actual servo
     * position.
     */
    @Override
    public double getPosition() {

        return resolve(getTarget().getPosition());
    }

    /**
     * Get the actual position, applying the {@link #resolve reverse
     * coordinate transofmation}.
     *
     * <p>
     *
     * This position may be provided at software (most probably) or hardware
     * level. Since the servo abstraction supports the smooth movement, it
     * is possible that the actual position is different from the one {@link
     * #setPosition requested}.
     *
     * @return The actual position of the servo.
     */
    @Override
    public double getActualPosition() {

        return resolve(getTarget().getActualPosition());
    }

    @Override
    public void addListener(ServoListener listener) {

        getTarget().addListener(listener);
    }

    @Override
    public void removeListener(ServoListener listener) {

        getTarget().removeListener(listener);
    }

    @Override
    public void setEnabled(boolean enabled) throws IOException {

        getTarget().setEnabled(enabled);
    }

    @Override
    protected Meta createMeta() {

        return getTarget().getMeta();
    }

    @Override
    protected void setActualPosition(double position) throws IOException {

        throw new Error("How come we ended up here?");
        //getTarget().setActualPosition(position);
    }

    /**
     * Provide the forward coordinate transformation.
     *
     * @param value Coordinate to transform.
     *
     * @return Servo position after the coordinate transformation.
     */
    protected abstract double transform(double value);

    /**
     * Provide the reverse coordinate transformation.
     *
     * @param position Servo position to resolve into the
     * coordinate-transformed value.
     *
     * @return The original value before the coordinate transformation.
     */
    protected abstract double resolve(double position);
}
