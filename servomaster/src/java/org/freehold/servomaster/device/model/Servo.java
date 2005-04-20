package org.freehold.servomaster.device.model;

import java.io.IOException;

/**
 * The servo abstraction.
 *
 * Allows instant and controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Servo.java,v 1.12 2005-04-20 22:01:32 vtt Exp $
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
     * This method returns immediately, even though the {@link
     * TransitionController transition controller} may be attached.
     *
     * @param position Position to set, between 0 and 1.0.
     *
     * @return A token that allows to track the completion of the servo
     * movement. If the servo has a transition controller {@link #attach
     * attached}, then the token wil track the servo movement. Otherwise,
     * its {@link TransitionCompletionToken#isComplete isComplete()} method
     * will always return <code>true</code>, and its {@link
     * TransitionCompletionToken#waitFor waitFor()} method will return
     * immediately.
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    public TransitionCompletionToken setPosition(double position) throws IOException;
    
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
     * @return Servo metadata.
     */
    public Meta getMeta();
    
    /**
     * Get the reference to the controller.
     *
     * The entities controlling the servos may need access to the controller
     * metadata, but they may not have direct access to it. This is how they
     * get it.
     *
     * @return The reference to the controller that controls this servo. The
     * return value can not be <code>null</code>, if the servo instance is a
     * wrapper, it must get the original servo controller reference.
     */
    public ServoController getController();
    
    /**
     * Attach the transition controller.
     *
     * @param transitionController The transition controller to use.
     *
     * @exception UnsupportedOperationException if the particular hardware
     * or software implementation conflicts with the transition controller
     * being attached. This may be the case with the servo controllers
     * supporting the controlled transitions at the hardware level.
     *
     * @exception IllegalStateException if the transition controller is
     * already installed at the lower level of the coordinate transformation
     * stack - the transition controllers are not stackable.
     */
    public void attach(TransitionController transitionController);
    
    /**
     * Get the transition controller attached to this servo.
     *
     * @return Transition controller reference, or <code>null</code> if
     * there is none.
     */
    public TransitionController getTransitionController();
    
    /**
     * Get the servo that is stacked right under this servo.
     *
     * @return The next servo in the stack, or <code>null</code> if this
     * servo is at the top of the stack.
     */
    public Servo getTarget();
}
