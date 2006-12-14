package org.freehold.servomaster.device.model;

/**
 * The transition controller abstraction.
 *
 * <p>
 *
 * The movement of the servo should sometimes be more complicated than just
 * a simple positioning. This interface provides a unified way to control
 * the motion.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: TransitionController.java,v 1.3 2006-12-14 09:17:10 vtt Exp $
 */
public interface TransitionController {

    /**
     * Move the servo to the target position according to the specified
     * transition pattern.
     *
     * <p>
     *
     * The implementation of this method <strong>must</strong> be thread safe.
     *
     * @param target Servo to move.
     *
     * @param token Transition control token.
     *
     * @param targetPosition Position to set the servo to at the end of the
     * transition.
     *
     */
    void move(Servo target, TransitionToken token, double targetPosition);
}
