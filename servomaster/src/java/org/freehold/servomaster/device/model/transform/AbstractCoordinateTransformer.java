package org.freehold.servomaster.device.model.transform;

import java.io.IOException;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoListener;
import org.freehold.servomaster.device.model.ServoMetaData;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractCoordinateTransformer.java,v 1.1 2001-12-31 23:33:12 vtt Exp $
 */
abstract public class AbstractCoordinateTransformer implements Servo {

    /**
     * The servo to apply the coordinate transformations to.
     */
    private Servo target;
    
    public AbstractCoordinateTransformer(Servo target) {
    
        if ( target == null ) {
        
            throw new IllegalArgumentException("target can't be null");
        }
        
        this.target = target;
    }
    
    protected Servo getTarget() {
    
        return target;
    }

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    public String getName() {
    
        return target.getName();
    }
    
    /**
     * Set the position, applying the {@link #transform coordinate
     * transofmation}.
     *
     * @param position Position to set, between 0 and 1.0.
     *
     * @param smooth True if the movement has to be smooth, false if it has
     * to be instant.
     *
     * @param transitionTime Time to span the movement across. Has to be
     * <code>0</code> if <code>smooth</code> is false.
     *
     * @exception IllegalArgumentException if:
     *
     * <ul>
     *
     * <li> The <code>transitionTime</code> is not <code>0</code> when
     *      <code>smooth</code> is false;
     *
     * <li> The position after applying the {@link #transform coordinate
     * transformation} is outside of <code>0...1.0</code> range.
     *
     * </ul>
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    public void setPosition(double position, boolean smooth, long transitionTime) throws IOException {
    
        target.setPosition(transform(position), smooth, transitionTime);
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
    
        return resolve(target.getPosition());
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
    
        return resolve(target.getActualPosition());
    }
    
    public void setRange(int range) throws IOException {
    
        target.setRange(range);
    }
    
    public void addListener(ServoListener listener) {
    
        target.addListener(listener);
    }
    
    public void removeListener(ServoListener listener) {
    
        target.removeListener(listener);
    }
    
    public void setEnabled(boolean enabled) throws IOException {
    
        target.setEnabled(enabled);
    }
    
    public ServoMetaData[] getMetaData() {
    
        return target.getMetaData();
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
