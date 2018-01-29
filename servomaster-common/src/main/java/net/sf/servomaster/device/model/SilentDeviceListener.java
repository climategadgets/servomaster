package net.sf.servomaster.device.model;

/**
 * Allows to track the {@link SilentDevice} status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public interface SilentDeviceListener<T extends SilentDevice> {

    /**
     * Accept the notification about the silent status change.
     *
     * @param target The device whose silent status has changed.
     *
     * @param silent The new mode.
     */
    void silentStatusChanged(T target, boolean silent);
}
