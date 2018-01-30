package net.sf.servomaster.device.model;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * The servo abstraction.
 *
 * Allows instant and controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public interface Servo extends SilentDevice {

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    String getName();

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
     * its {@link Future#isDone()} method will always return {@code true},
     * and its {@link Future#get()} method will return immediately.
     *
     * @throws IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @throws IllegalStateException if the servo is currently {@link #setEnabled disabled}.
     */
    Future<Throwable> setPosition(double position) throws IOException;

    /**
     * Get the position.
     *
     * @return The position that has previously been {@link #setPosition
     * set}. Note that this may not be the actual position of the servo at
     * the time, if the {@link #getTransitionController() transition controller} is attached.
     * Use {@link #getActualPosition getActualPosition()} to obtain the actual servo position.
     */
    double getPosition();

    /**
     * Get the actual position.
     *
     * This position may be provided at software (most probably) or hardware
     * level. Since the servo abstraction supports the controlled
     * transition, it is possible that the actual position is different from
     * the one {@link #setPosition requested}.
     *
     * @return The actual position of the servo.
     */
    double getActualPosition();

    /**
     * Add the servo listener.
     *
     * @param listener The listener to notify when the position changes.
     *
     * @throws UnsupportedOperationException if the implementation doesn't support listeners.
     */
    void addListener(ServoListener listener);

    /**
     * Remove the servo listener.
     *
     * @param listener The listener to remove from notification list.
     *
     * @throws IllegalArgumentException if the listener wasn't there.
     *
     * @throws UnsupportedOperationException if the implementation doesn't support listeners.
     */
    void removeListener(ServoListener listener);

    /**
     * Enable or disable the servo.
     *
     * @param enabled {@code true} enables the servo, {@code false} disables it and (<strong>IMPORTANT!</strong>)
     * puts it into a silent mode if the servo (not the controller) supports {@code servo/silent} feature,
     * and its value is currently set to {@code true}.
     *
     * @throws IOException if there was a problem communicating with the hardware controller.
     */
    void setEnabled(boolean enabled) throws IOException;

    /**
     * Get the servo metadata.
     *
     * @return Servo metadata.
     */
    Meta getMeta();

    /**
     * Get the reference to the controller.
     *
     * The entities controlling the servos may need access to the controller
     * metadata, but they may not have direct access to it. This is how they
     * get it.
     *
     * @return The reference to the controller that controls this servo. The
     * return value can not be {@code null}, if the servo instance is a
     * wrapper, it must get the original servo controller reference.
     */
    ServoController getController();

    /**
     * Attach the transition controller.
     *
     * @param transitionController The transition controller to use.
     *
     * @param queueTransitions if set to {@code true}, the current transition (as defined by
     * previously called {@link #setPosition(double)} and attached transition controller)
     * will always be completed, and only then the next transition (initiated by next
     * {@code setPosition()} call will take place. The transition queue depth is unlimited.
     *
     * If set to {@code false}, every {@code setPosition() call will interrupt the current transition
     * (if any), and start the new transition starting from the actual position the servo
     * resides at at the moment.
     *
     * Likewise, if there is any transition in progress when this method is called, whether it is
     * canceled or completed depends on the value of this argument.
     *
     * @throws UnsupportedOperationException if the particular hardware
     * or software implementation conflicts with the transition controller
     * being attached. This may be the case with the servo controllers
     * supporting the controlled transitions at the hardware level.
     *
     * @throws IllegalStateException if the transition controller is
     * already installed at the lower level of the coordinate transformation
     * stack - the transition controllers are not stackable.
     * 
     * @see #getTransitionController()
     */
    void attach(TransitionController transitionController, boolean queueTransitions);

    /**
     * Get the transition controller attached to this servo.
     *
     * @return Transition controller reference, or {@code null} if there is none.
     * 
     * @see #attach(TransitionController)
     */
    TransitionController getTransitionController();

    /**
     * Get the servo that is stacked right under this servo.
     *
     * @return The next servo in the stack, or {@code null} if this servo is at the top of the stack.
     */
    Servo getTarget();

    /**
     * Start working with the servo.
     */
    void open();
}
