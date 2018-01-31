package net.sf.servomaster.device.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.SortedSet;

/**
 * The servo controller abstraction.
 *
 * Provides the encapsulation of the actual hardware controller into a
 * platform-independent entity.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public interface ServoController extends SilentDevice, Closeable {

    public static enum Feature {

        SILENT("controller/silent");

        public final String name;

        private Feature(String name) {
            this.name = name;
        }
    }

    /**
     * Initialize the controller.
     *
     * If the controller was created uninitialized, this method has to be
     * called to initialize it.
     *
     * @param portName The controller port name composed in such a way that
     * controller will become operable upon parsing it.
     *
     * @throws IllegalArgumentException if the port name is not {@code null}.
     *
     * @deprecated Use {@link #open()} instead. This method is only retained here temporarily
     * until https://github.com/climategadgets/servomaster/issues/2 is done.
     */
    @Deprecated
    void init(String portName) throws IOException;

    /**
     * Start working with the controller.
     *
     * @throws IOException if there was a problem communicating with the hardware controller.
     */
    void open() throws IOException;

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
     * @throws IllegalArgumentException if the ID supplied doesn't map to
     * a physical device. In particular, this exception should be thrown if
     * the controller is able to determine whether the servo is connected,
     * and it is not.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     *
     * @throws IOException if there was a problem communicating to the hardware controller.
     */
    Servo getServo(String id) throws IOException;

    /**
     * Get the number of servos this controller supports.
     *
     * NOTE: This method was introduced as a part of refactoring with the
     * goal being to remove redundant code. It is quite possible that it
     * will be deprecated in favor of using
     * <code>controller/maxservos</code> meta property.
     *
     * @return Number of servos supported by this controller.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     */
    int getServoCount();

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
     * @return An immutable set of all the servos physically present on the controller.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     *
     * @throws IOException if there was a problem communicating to the hardware controller.
     */
    SortedSet<Servo> getServos() throws IOException;

    /**
     * Reset the controller.
     *
     * <p>
     *
     * All the hardware devices fail and/or go nuts once in a while, and
     * need a reset. The controller is deemed faulty if the reset throws the
     * exception.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     *
     * @throws IOException when the controller is beyond repair.
     */
    void reset() throws IOException;

    /**
     * Add the servo controller listener.
     *
     * @param listener The listener to notify when the controller status changes.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     *
     * @throws UnsupportedOperationException if the implementation doesn't support listeners.
     */
    void addListener(ServoControllerListener listener);

    /**
     * Remove the servo listener.
     *
     * @param listener The listener to remove from notification list.
     *
     * @throws IllegalArgumentException if the listener wasn't there.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     *
     * @throws UnsupportedOperationException if the implementation doesn't support listeners.
     */
    void removeListener(ServoControllerListener listener);

    /**
     * Get the servo controller metadata.
     *
     * @return Servo controller metadata.
     */
    Meta getMeta();

    /**
     * Get the port name.
     *
     * @return the port name.
     */
    String getPort();

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
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     */
    void setLazyMode(boolean enable);

    /**
     * Get the current lazy mode.
     *
     * @return <code>true</code> if the redundant positioning commands are
     * <strong>not</strong> sent to the controller, <code>false</code>
     * otherwise.
     *
     * @throws IllegalStateException if {@link #open()} hasn't been called yet, or {@link Closeable#close()} was already called.
     */
    boolean isLazy();
}
