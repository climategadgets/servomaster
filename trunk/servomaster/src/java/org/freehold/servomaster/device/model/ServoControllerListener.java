package org.freehold.servomaster.device.model;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerListener.java,v 1.1 2001-08-31 21:38:00 vtt Exp $
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
