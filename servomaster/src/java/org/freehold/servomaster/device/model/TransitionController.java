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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: TransitionController.java,v 1.1 2002-01-02 03:51:13 vtt Exp $
 */
public interface TransitionController {

    /**
     * Move the servo to the target position according to the specified
     * transition pattern.
     *
     * @param targetPosition Position to set the servo to at the end of the
     * transition.
     *
     */
    public void move(double targetPosition);
}
