package net.sf.servomaster.device.model;

/**
 * Allows to track the {@link SilentDevice} status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public interface SilentDeviceListener {

    /**
     * Accept the notification about the silent status change.
     *
     * @param source The device whose silent status has changed.
     *
     * @param silent The new mode.
     */
    void silentStatusChanged(SilentDevice source, boolean silent);
}
