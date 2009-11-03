package net.sf.servomaster.device.model;

/**
 * Allows to track the servo controller status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
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
