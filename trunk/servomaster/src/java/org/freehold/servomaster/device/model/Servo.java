package org.freehold.servomaster.device.model;

import java.io.IOException;

/**
 * The servo abstraction.
 *
 * Allows instant and smooth positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Servo.java,v 1.5 2001-12-14 21:58:12 vtt Exp $
 */
public interface Servo {

    /**
     * Get the name.
     *
     * @return Servo name.
     */
    public String getName();
    
    /**
     * Set the position.
     *
     * @param position Position to set, between 0 and 255.
     *
     * @param smooth True if the movement has to be smooth, false if it has
     * to be instant.
     *
     * <p>
     *
     * <strong>NOTE:</strong> Some controllers do support the smooth
     * movement on the hardware level, some don't. For those that don't, it
     * is not possible to get the reliable time span, so the controller
     * abstraction has to perform to the best of its abilities to
     * approximate the stepping and timing. If it doesn't, oh well.
     *
     * <p>
     *
     * <strong>NOTE:</strong> It is a responsibility of the implementing
     * class to ensure that the servos are accessed in a thread safe manner.
     *
     * @param transitionTime Time to span the movement across. Has to be
     * <code>0</code> if <code>smooth</code> is false.
     *
     * @exception IllegalArgumentException if:
     *
     * <ul>
     *
     * <li> The <code>transitionTime</code> is not <code>0</code> when
     *      <code>smooth</code> is false;
     *
     * <li> The position is outside of <code>0...255</code> range.
     *
     * </ul>
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     *
     * @exception IllegalStateException if the servo is currently {@link
     * #setEnabled disabled}.
     */
    public void setPosition(int position, boolean smooth, long transitionTime) throws IOException;
    
    /**
     * Get the position.
     *
     * @return The position that has previously been {@link #setPosition
     * set}. Note that this may not be the actual position of the servo at
     * the time, if the set operation was requested to be smooth. Use {@link
     * #getActualPosition getActualPosition()} to obtain the actual servo
     * position.
     */
    public int getPosition();
    
    /**
     * Get the actual position.
     *
     * <p>
     *
     * This position may be provided at software (most probably) or hardware
     * level. Since the servo abstraction supports the smooth movement, it
     * is possible that the actual position is different from the one {@link
     * #setPosition requested}.
     *
     * @return The actual position of the servo.
     */
    public int getActualPosition();
    
    /**
     * Enable the long throw.
     *
     * <p>
     *
     * Be careful with the long throw, not all servos support it.
     *
     * @param range <code>false</code> for 90 degree range,
     * <code>true</code> for 180 degree range.
     *
     * @exception UnsupportedOperationException if the hardware controller
     * doesn't support the range selection.
     *
     * @exception IOException if there was a problem communicating with the
     * device, or the device was unable to complete the operation.
     */
    public void setRange(int range) throws IOException;
    
    /**
     * Add the servo listener.
     *
     * @param listener The listener to notify when the position changes.
     *
     * @exception UnsupportedOperationException if the implementation
     * doesn't support listeners.
     */
    public void addListener(ServoListener listener);
    
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
    public void removeListener(ServoListener listener);
    
    /**
     * Enable or disable the servo.
     *
     * @param enable <code>true</code> to enable.
     *
     * @exception IOException if ther was a problem communicating with the
     * hardware controller.
     */
    public void setEnabled(boolean enabled) throws IOException;
    
    /**
     * Get the servo metadata.
     *
     * @return Servo metadata.
     *
     * @exception UnsupportedOperationException if the particular
     * implementation doesn't support the capabilities discovery.
     */
    public ServoMetaData[] getMetaData();
}
