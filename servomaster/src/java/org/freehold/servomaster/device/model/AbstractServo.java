package org.freehold.servomaster.device.model;

import java.io.IOException;

/**
 * Supports the transition controller functionality.
 *
 * Allows instant and controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractServo.java,v 1.1 2002-01-02 03:51:13 vtt Exp $
 */
abstract public class AbstractServo implements Servo {

    private Servo target;
    private TransitionController controller;
    
    public AbstractServo(Servo target) {
    
        this.target = target;
    }
    
    public AbstractServo() {
    
        this(null);
    }

    public final void attach(TransitionController controller) {
    
        this.controller = controller;
    }
    
    public final TransitionController getTransitionController() {
    
        return controller;
    }
    
    public final Servo getTarget() {
    
        return target;
    }
    
        /**
         * Set the servo position without regard to the transition controller.
         *
         * <p>
         *
         * This method is ultimately called by the transition controller, as
         * well as directly from {@link #setPosition setPosition()} when
         * the transition controller is not attached.
         */
    abstract protected void setActualPosition(double position) throws IOException;
}
