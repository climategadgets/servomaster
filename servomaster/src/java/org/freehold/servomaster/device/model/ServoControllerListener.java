package org.freehold.servomaster.device.model;

/**
 * Allows to track the servo controller status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerListener.java,v 1.2 2002-01-02 09:11:18 vtt Exp $
 */
public interface ServoControllerListener {

    /**
     * Accept the notification about the silent status change.
     *
     * @param source The controller whose status has changed.
     *
     * @param mode The new mode.
     */
    public void silentStatusChanged(ServoController source, boolean mode);
}
