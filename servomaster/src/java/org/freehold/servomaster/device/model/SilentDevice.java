package org.freehold.servomaster.device.model;

import java.io.IOException;

/**
 * Defines the silent device.
 *
 * The device is considered silent if it can deactivate itself after a
 * period of inactivity, thus reducing the noise and energy consumption.
 *
 * <p>
 *
 * Some controllers support deenergizing the servos on per-controller, some
 * on per-servo basis.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: SilentDevice.java,v 1.2 2002-03-09 05:23:16 vtt Exp $
 */
public interface SilentDevice {

    /**
     * Set a silent mode.
     *
     * <p>
     *
     * Normally, even if the servo position doesn't change, the servo is
     * still being fed the control pulse, which causes it to forcibly keep
     * the position, but at the same time it growls quite sensibly. There
     * are applications that don't require any significant force to be
     * applied constantly, but they do require silent operation. For this
     * purpose, the silent mode is introduced - after a specified timeout,
     * the control pulse to the servos is stopped, and then it is switched
     * on for a short period once in a while.
     *
     * <p>
     *
     * The default mode of operation is left to the implementation.
     *
     * @param silent <code>true</code> if silent operation is required,
     * <code>false</code> otherwise.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * is not capable of suspending the control pulse.
     *
     * @exception IOException if there was a problem communicating to the
     * hardware controller.
     */
    public void setSilentMode(boolean silent) throws IOException;
    
    /**
     * Set the silent timeout.
     *
     * <p>
     *
     * When this timeout after the last positioning operation expires, the
     * control pulse to the servos is stopped until next positioning
     * operation, or next heartbeat, whichever occurs first.
     *
     * <p>
     *
     * When the heartbeat timeout expires, the controller starts sending the
     * control signal for the next <code>timeout</code> milliseconds, then
     * falls asleep again.
     *
     * @param timeout Time to hold the the servos energized after the last
     * positioning, in milliseconds.
     *
     * @param heartbeat Maximum time to allow the servos to be without a
     * control signal, in milliseconds. If this is 0, the servos will be
     * without a signal indefinitely, or until next positioning operation.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * is not capable of suspending the control pulse.
     */
    public void setSilentTimeout(long timeout, long heartbeat);
    
    /**
     * Whether the device is silent right now.
     *
     * @return true if the device is silent now.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * is not capable of suspending the control pulse.
     */
    public boolean isSilentNow();
    
    /**
     * Find out if the device is in the silent mode.
     *
     * @return true if the device is in the silent mode.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * is not capable of suspending the control pulse.
     */
    public boolean getSilentMode();
}