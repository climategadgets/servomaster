package org.freehold.servomaster.device.model;

import java.io.IOException;
import java.util.Iterator;

/**
 * The servo controller abstraction.
 *
 * Provides the encapsulation of the actual hardware controller into a
 * platform-independent entity.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoController.java,v 1.8 2002-02-10 07:23:55 vtt Exp $
 */
public interface ServoController {

    /**
     * Initialize the controller.
     *
     * <p>
     *
     * If the controller was created uninitialized, this method has to be
     * called to initialize it.
     *
     * @param portName The controller port name composed in such a way that
     * controller will become operable upon parsing it.
     *
     * @exception IllegalArgumentException if the port name didn't contain
     * enough information to initialize the controller.
     *
     * @exception IllegalStateException if the controller is already initialized.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    public void init(String portName) throws IOException;

    /**
     * Get the servo instance.
     *
     * <p>
     *
     * Since the instances of servo abstractions are really the wrappers
     * created by a specific hardware controller implementation, it should
     * not be possible to instantiate a servo by itself. This is a factory
     * method that instantiates the servos and gives them to the caller.
     *
     * @param id The servo ID. Since different controllers may have
     * different ideas of how to handle the addressing, it is up to the
     * controller implementation to perform the sanity checking on the
     * input.
     *
     * @return A servo abstraction instance.
     *
     * @exception IllegalArgumentException if the ID supplied doesn't map to
     * a physical device. In particular, this exception should be thrown if
     * the controller is able to determine whether the servo is connected,
     * and it is not.
     *
     * @exception IOException if there was a problem communicating to the
     * hardware controller.
     */
    public Servo getServo(String id) throws IOException;
    
    /**
     * Get all the servos that this controller supports.
     *
     * <p>
     *
     * The servos will be included in the iterator regardless of whether
     * they are enabled or disabled. However, if the controller is able to
     * determine whether the servo is connected, and it is not, it must not
     * be included in the iterator.
     *
     * @return An iterator on all the servos physically present on the
     * controller.
     *
     * @exception IOException if there was a problem communicating to the
     * hardware controller.
     */
    public Iterator getServos() throws IOException;
    
    /**
     * Set a silent mode.
     *
     * <p>
     *
     * Normally, even if the servo position doesn't change, the servo is
     * still being fed the control pulse, which causes it to forcibly keep
     * the position, but at the same time it hums quite sensibly. There are
     * applications that don't require any significant force to be applied
     * constantly, but they do require silent operation. For this purpose,
     * the silent mode is introduced - after a specified timeout, the
     * control pulse to the servos is stopped, and then it is switched on
     * for a short period once in a while.
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
     * Check whether the controller is silent now.
     *
     * @return <code>true</code> if the controller is active,
     * <code>false</code> if it is silent.
     */
    public boolean getSilentStatus();
    
    /**
     * Set the silent timeout.
     *
     * When this timeout expires, the control pulse to the servos is stopped
     * until next positioning operation.
     *
     * @param timeout Time to hold the the servos energized after the last
     * positioning, in milliseconds.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * is not capable of suspending the control pulse.
     *
     * @deprecated Use {@link #setSilentTimeout(long, long)
     * setSilentTimeout(timeout, heartbeat)} instead.
     */
    public void setSilentTimeout(long timeout);

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
     * Reset the controller.
     *
     * <p>
     *
     * All the hardware devices fail and/or go nuts once in a while, and
     * need a reset. The controller is deemed faulty if the reset throws the
     * exception.
     *
     * @exception IOException when the controller is beyond repair.
     */
    public void reset() throws IOException;

    /**
     * Add the servo controller listener.
     *
     * @param listener The listener to notify when the controller status
     * changes.
     *
     * @exception UnsupportedOperationException if the implementation
     * doesn't support listeners.
     */
    public void addListener(ServoControllerListener listener);

    /**
     * Remove the servo listener.
     *
     * @param listener The listener to remove from notification list.
     *
     * @exception IllegalArgumentException if the listener wasn't there.
     *
     * @exception UnsupportedOperationException if the implementation
     * doesn't support listeners.
     */
    public void removeListener(ServoControllerListener listener);

    /**
     * Get the servo controller metadata.
     *
     * @return Servo controller metadata.
     *
     * @exception UnsupportedOperationException if the particular
     * implementation doesn't support the capabilities discovery.
     */
    public ServoControllerMetaData getMetaData();
    
    /**
     * Get the port name.
     *
     * @return the port name.
     *
     * @exception IllegalStateException if the controller hasn't been {@link
     * #init initialized} yet.
     */
    public String getPort();
    
    /**
     * Enable or disable the lazy mode.
     *
     * It may or may not be a good idea to save some I/O and not send the
     * redundant positioning commands to the hardware controller. However,
     * this has a downside, too: it defeats the heartbeat, because the
     * heartbeat by definition sends the command to position the servo
     * exactly where it was before.
     *
     * @param enable <code>true</code> if it is OK to skip the redundant
     * positioning commands. This will cause less I/O. If set to
     * <code>false</code>, even redundant commands will be sent to the
     * controller.
     */
    public void setLazyMode(boolean enable);
    
    /**
     * Get the current lazy mode.
     *
     * @return <code>true</code> if the redundant positioning commands are
     * <strong>not</strong> sent to the controller, <code>false</code>
     * otherwise.
     */
    public boolean isLazy();
}