package org.freehold.servomaster.device.model.transform;

import java.io.IOException;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.ServoListener;
import org.freehold.servomaster.device.model.ServoMetaData;

/**
 * The coordinate transformer skeleton.
 *
 * Provides the 'wrapper' for the servo being controlled, as well as the
 * means to perform the actual coordinate transformation.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractCoordinateTransformer.java,v 1.4 2002-01-02 09:11:18 vtt Exp $
 */
abstract public class AbstractCoordinateTransformer extends AbstractServo {

    /**
     * Create the instance.
     *
     * @param target Servo to control. May be another coordinate transformer
     * as well.
     */
    public AbstractCoordinateTransformer(Servo target) {
    
        super(null, target);
    
        if ( getTarget() == null ) {
        
            throw new IllegalArgumentException("target can't be null");
        }
    }

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    public String getName() {
    
        return getTarget().getName();
    }
    
    /**
     * Set the position, applying the {@link #transform coordinate
     * transofmation}.
     *
     * @param position Position to set, between 0 and 1.0.
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    public void setPosition(double position) throws IOException {
    
        getTarget().setPosition(transform(position));
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
    public double getActualPosition() {
    
        return resolve(getTarget().getActualPosition());
    }
    
    public void setRange(int range) throws IOException {
    
        getTarget().setRange(range);
    }
    
    public void addListener(ServoListener listener) {
    
        getTarget().addListener(listener);
    }
    
    public void removeListener(ServoListener listener) {
    
        getTarget().removeListener(listener);
    }
    
    public void setEnabled(boolean enabled) throws IOException {
    
        getTarget().setEnabled(enabled);
    }
    
    public ServoMetaData[] getMetaData() {
    
        return getTarget().getMetaData();
    }
    
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
    abstract protected double transform(double value);

    /**
     * Provide the reverse coordinate transformation.
     *
     * @param position Servo position to resolve into the
     * coordinate-transformed value.
     *
     * @return The original value before the coordinate transformation.
     */
    abstract protected double resolve(double position);
}
