package net.sf.servomaster.device.model;

/**
 * The transition controller abstraction.
 *
 * The movement of the servo may sometimes be more complicated than just a simple immediate positioning.
 * This interface provides a unified way to control the motion.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public interface TransitionController {

    /**
     * Move the servo to the target position according to the specified transition pattern.
     *
     * Unlike in {@code v.0.x}, there are no restrictions on thread safety of the implementation - it will be invoked in a thread safe manner.
     * In fact, there are no restrictions on the implementation at all except for being interruptable.
     *
     * @param target Servo to move.
     *
     * @param targetPosition Position to set the servo to at the end of the transition.
     */
    void move(Servo target, double targetPosition);
}
