package org.freehold.servomaster.device.model;

/**
 * Allows to track the servo controller status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerListener.java,v 1.4 2002-03-12 07:07:00 vtt Exp $
 */
public interface ServoControllerListener extends DisconnectListener, ProblemListener {

    /**
     * Accept the notification about the silent status change.
     *
     * @param source The controller whose status has changed.
     *
     * @param mode The new mode.
     */
    public void silentStatusChanged(ServoController source, boolean mode);
}
