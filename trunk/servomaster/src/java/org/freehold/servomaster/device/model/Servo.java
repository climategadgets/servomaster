package org.freehold.servomaster.device.model;

import java.io.IOException;

/**
 * The servo abstraction.
 *
 * Allows instant and controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Servo.java,v 1.7 2002-01-02 03:51:13 vtt Exp $
 */
public interface Servo {

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    public String getName();
    
    /**
     * Set the position.
     *
     * @param position Position to set, between 0 and 1.0.
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    public void setPosition(double position) throws IOException;
    
    /**
     * Get the position.
     *
     * @return The position that has previously been {@link #setPosition
     * set}. Note that this may not be the actual position of the servo at
     * the time, if the transition controller is attached. Use {@link
     * #getActualPosition getActualPosition()} to obtain the actual servo
     * position.
     */
    public double getPosition();
    
    /**
     * Get the actual position.
     *
     * <p>
     *
     * This position may be provided at software (most probably) or hardware
     * level. Since the servo abstraction supports the controlled
     * transition, it is possible that the actual position is different from
     * the one {@link #setPosition requested}.
     *
     * @return The actual position of the servo.
     */
    public double getActualPosition();
    
    /**
     * Set the servo range.
     *
     * <p>
     *
     * Be careful with the range selection, especially over 90\u00B0, not
     * all the servos support it.
     *
     * @param range Requested servo range, 0\u00B0 to 180\u00B0.
     *
     * @exception IllegalArgumentException if the servo isn't capable of
     * supporting the requested range.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * doesn't support the range selection.
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     */
    public void setRange(int range) throws IOException;
    
    /**
     * Add the servo listener.
     *
     * @param listener The listener to notify when the position changes.
     *
     * @exception UnsupportedOperationException if the implementation
     * doesn't support listeners.
     */
    public void addListener(ServoListener listener);
    
    /**
     * Remove the servo listener.
     *
     * @param listener The listener to remove from notification list.
     *
     * @exception IllegalArgumentException if the listener wasn't there.
     *
     * @exception UnsupportedOperationException if the implementation
     * doesn't support listeners.
     */
    public void removeListener(ServoListener listener);
    
    /**
     * Enable or disable the servo.
     *
     * @param enable <code>true</code> to enable.
     *
     * @exception IOException if ther was a problem communicating with the
     * hardware controller.
     */
    public void setEnabled(boolean enabled) throws IOException;
    
    /**
     * Get the servo metadata.
     *
     * @return Servo metadata array. Each array entry corresponds to the
     * servo properties under the voltage pertinent to that entry.
     *
     * @exception UnsupportedOperationException if the particular
     * implementation doesn't support the capabilities discovery.
     */
    public ServoMetaData[] getMetaData();
    
    /**
     * Attach the transition controller.
     *
     * @param transitionController The transition controller to use.
     *
     * @exception UnsupportedOperationException if the particular hardware
     * or software implementation conflicts with the transition controller
     * being attached. This may be the case with the servo controllers
     * supporting the controlled transitions at the hardware level, or if
     * the transition controller is already installed at the lower level of
     * the coordinate transformation stack - the transition controllers are
     * not stackable.
     */
    public void attach(TransitionController transitionController);
    
    /**
     * Get the servo that is stacked right under this servo.
     *
     * @return The next servo in the stack, or <code>null</code> if this
     * servo is at the top of the stack.
     */
    public Servo getTarget();
}
