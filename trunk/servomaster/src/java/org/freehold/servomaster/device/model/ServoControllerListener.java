package org.freehold.servomaster.device.model;

/**
 * Allows to track the servo controller status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: ServoControllerListener.java,v 1.5 2006-12-14 09:17:10 vtt Exp $
 */
public interface ServoControllerListener extends DisconnectListener, ProblemListener {

    /**
     * Accept the notification about the silent status change.
     *
     * @param controller The controller whose status has changed.
     *
     * @param silent The new mode.
     */
    void silentStatusChanged(ServoController controller, boolean silent);
}
