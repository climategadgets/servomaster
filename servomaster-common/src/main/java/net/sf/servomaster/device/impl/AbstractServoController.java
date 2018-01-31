package net.sf.servomaster.device.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.device.model.silencer.SilentHelper;
import net.sf.servomaster.device.model.silencer.SilentProxy;

/**
 * Abstract servo controller.
 *
 * <p>
 *
 * This class provides the features that are common to all the hardware drivers:
 *
 * <ul>
 *
 * <li> Disconnected mode support
 *
 * <li> Listener additions, removal and notifications
 *
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2018
 */
public abstract class AbstractServoController implements ServoController {

    protected final Logger logger = Logger.getLogger(getClass());

    /**
     * The device-specific name of the port the controller is connected to.
     * May be {@code null}.
     */
    protected final String portName;

    /**
     * The listener set.
     */
    private Set<ServoControllerListener> listenerSet = new HashSet<ServoControllerListener>();

    /**
     * 'disconnected' mode flag.
     *
     * If it is true, then the controller driver instance will be created
     * and allowed to operate regardless of whether device is connected or
     * not. Default is true.
     *
     * @see #allowDisconnect
     * @see #isDisconnectAllowed
     * @see #isConnected
     */
    private boolean disconnected = true;

    /**
     * Is the device currently connected?
     */
    // VT: FIXME: Think about getting rid of this
    protected boolean connected = false;

    /**
     * The silencer.
     */
    private SilentHelper silencer;

    /**
     * Physical servo representation.
     *
     * VT: FIXME: Do we really need it like this? It is possible to have controllers
     * with dynamically changing servo set (and, {@link #getServoCount()} is also affected.
     */
    private Servo[] servoSet;
    
    /**
     * Initialization state.
     * 
     * Currently, calling {@link #close()} makes an instance unusable. In the
     * future, it may be possible to bring all implementations into a state where
     * they can be opened again after having been closed.
     */
    private final AtomicInteger initState = new AtomicInteger(0);

    /**
     * @see #getMeta()
     */
    private Meta meta;

    /**
     * Create an instance.
     *
     * @param portName See {@link #portName}.
     */
    protected AbstractServoController(String portName) {
        this.portName = portName;
    }

    @Override
    public final String getPort() {
        return portName;
    }

    @Override
    public final synchronized void init(String portName) throws IOException {

        if (portName != null) {
            throw new IllegalArgumentException("Deprecated method, wrong argument, see the code");
        }

        open();
    }

    @Override
    public final synchronized void open() throws IOException {
        
        // Now that we know we're instantiated, we can finally get it
        meta = createMeta();

        if (initState.get() != 0) {

            throw new IllegalStateException("state 0 expected, actual is " + initState.get());
        }

        doInit();

        // Controller is initialized, now it's time to go get the servos
        initState.incrementAndGet();

        servoSet = new Servo[getServoCount()];

        startSilencer();

        reset();
    }

    private void startSilencer() {

        if ( getMeta().getFeatures().containsKey(Feature.SILENT.name) ) {

            silencer = new SilentHelper(new ControllerSilencer());
            silencer.start();
        }
    }

    /**
     * Perform the controller initialization. Don't touch the servos yet.
     *
     * @throws IOException if there was a hardware error.
     */
    protected abstract void doInit() throws IOException;

    /**
     * Check if the controller is initialized.
     *
     * @exception IllegalStateException if the controller is not yet
     * initialized.
     */
    protected final synchronized void checkInit() {
        
        if (initState.get() != 1) {
            throw new IllegalStateException("state 1 expected, actual is " + initState.get());
        }
    }

    @Override
    public void setLazyMode(boolean enable) {
        throw new UnsupportedOperationException("Lazy mode is not supported");
    }

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public final synchronized void addListener(ServoControllerListener listener) {

        checkInit();

        listenerSet.add(listener);
    }

    @Override
    public final synchronized void removeListener(ServoControllerListener listener) {

        checkInit();

        if ( !listenerSet.contains(listener) ) {

            throw new IllegalArgumentException("Not a registered listener: "
                    + listener.getClass().getName()
                    + "@"
                    + listener.hashCode());
        }

        listenerSet.remove(listener);
    }

    /**
     * Notify the listeners about the change in the silent status.
     *
     * @param mode The new silent status. {@code false} means device is
     * sleeping, {@code true} means device is active.
     */
    protected final void silentStatusChanged(boolean mode) {

        for ( Iterator<ServoControllerListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().silentStatusChanged(this, mode);
        }
    }

    /**
     * Notify the listeners about the problem that occured.
     *
     * @param t The exception to broadcast.
     */
    protected final void exception(Throwable t) {

        for ( Iterator<ServoControllerListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().exception(this, t);
        }
    }

    public void setSilentTimeout(long timeout, long heartbeat) {

        checkInit();
        checkSilencer();

        silencer.setSilentTimeout(timeout, heartbeat);
    }

    public final void setSilentMode(boolean silent) {

        checkInit();
        checkSilencer();

        boolean oldMode = getSilentMode();

        silencer.setSilentMode(silent);

        if ( silent != oldMode ) {

            silentStatusChanged(isSilentNow());
        }

        touch();
    }

    /**
     * Check if the silent operation is supported <strong>and</strong>
     * implemented.
     *
     * @exception UnsupportedOperationException if the silent operation is
     * either not supported or not implemented.
     */
    private synchronized void checkSilencer() {

        // First check if it is declared

        // VT: Let's assume for a second that it is not null

        Meta controllerMeta = getMeta();

        // This will throw the exception if it is not declared

        boolean silentSupport = controllerMeta.getFeature(Feature.SILENT.name);

        if (!silentSupport) {

            // Oh well...

            throw new UnsupportedOperationException("Silent operation is not supported");
        }

        // Then see if it is implemented

        if ( silencer == null ) {

            throw new UnsupportedOperationException("Silent operation seems to be supported, but not implemented");
        }
    }

    public final boolean getSilentMode() {

        checkInit();
        
        // Blow up if we don't support it
        getMeta().getFeature(Feature.SILENT.name);

        return (silencer == null) ? false : silencer.getSilentMode();
    }

    public final boolean isSilentNow() {

        checkInit();
        
        // Blow up if we don't support it
        getMeta().getFeature(Feature.SILENT.name);

        return (silencer == null) ? false : silencer.isSilentNow();
    }

    @Override
    public final synchronized Meta getMeta() {

        // Meta will be created the first thing after the instance is created, in open().
        // If it hasn't been, something is wrong

        if (meta == null) {
            throw new IllegalStateException("null meta found, is everything all right?");
        }

        return meta;
    }

    protected Meta createMeta() {
        throw new UnsupportedOperationException("This driver class doesn't provide metadata (most probably oversight on developer's part)");
    }

    /**
     * Disable or enable the controller driver 'disconnected' mode.
     *
     * If enabled, the driver will function regardless whether the actual
     * device is connected or not.
     *
     * <p>
     *
     * This is the only method that is allowed to be called before {@link
     * #init init()}.
     *
     * @param disconnected {@code true} if disconnected operation is allowed.
     */
    public void allowDisconnect(boolean disconnected) {

        checkInit();
        this.disconnected = disconnected;
    }

    /**
     * Check the disconnected mode.
     *
     * @return true if the controller driver can function if the device is not connected.
     */
    public boolean isDisconnectAllowed() {

        checkInit();
        return disconnected;
    }

    /**
     * Is the device currently connected?
     *
     * <p>
     *
     * This method will check the presence of the device and return the
     * status.
     *
     * @return true if the device seems to be connected.
     */
    public abstract boolean isConnected();

    public void deviceArrived(ServoController device) {

        logger.warn("deviceArrived is not implemented by " + getClass().getName());
    }

    public void deviceDeparted(ServoController device) {

        logger.warn("deviceDeparted is not implemented by " + getClass().getName());
    }

    /**
     * Update the silent helper timestamp.
     *
     * This method is critical to properly support the silent mode. It
     * should be called every time the operation that should keep the
     * controller energized for some more ({@link #reset reset()}, {@link
     * Servo#setPosition Servo.setPosition()}) is performed.
     */
    protected final void touch() {

        if ( silencer != null ) {

            silencer.touch();
        }
    }

    @Override
    public final SortedSet<Servo> getServos() throws IOException {

        checkInit();

        SortedSet<Servo> servos = new TreeSet<Servo>();

        for ( int idx = 0; idx < getServoCount(); idx++ ) {

            servos.add(getServo(Integer.toString(idx)));
        }

        return Collections.unmodifiableSortedSet(servos);
    }

    /**
     * Get the servo instance.
     *
     * @param id The servo ID. A valid ID is a <strong>decimal</strong>
     * string representation of the integer in 0...4 range.
     *
     * @return A servo abstraction instance.
     *
     * @exception IllegalArgumentException if the ID supplied doesn't map to
     * a physical device.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     *
     * @exception IllegalStateException if the controller wasn't previously
     * initialized.
     */
    public final synchronized Servo getServo(String id) throws IOException {

        checkInit();

        try {

            int iID = Integer.parseInt(id);

            if ( iID < 0 || iID > getServoCount() ) {

                throw new IllegalArgumentException("ID out of 0..." + getServoCount() + " range: '" + id + "'");
            }

            if ( servoSet[iID] == null ) {

                servoSet[iID] = createServo(iID);
                servoSet[iID].open();
            }

            return servoSet[iID];

        } catch ( NumberFormatException nfex ) {

            throw new IllegalArgumentException("Not a number: '" + id + "'", nfex);
        }
    }

    /**
     * Create the servo instance.
     *
     * This is a template method used to instantiate the proper servo
     * implementation class.
     *
     * @param id Servo ID to create.
     *
     * @return The servo instance.
     * 
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected abstract Servo createServo(int id) throws IOException;
    
    public synchronized void close() throws IOException {

        if (initState.get() != 1) {
            throw new IllegalStateException("state 1 expected, actual is " + initState.get());
        }
        
        throw new IllegalStateException("Not Implemented. See https://github.com/climategadgets/servomaster/issues/12");
    }

    protected void sleep() throws IOException {

        // Do absolutely nothing
        logger.debug("sleep: not implemented");
    }

    protected void wakeUp() throws IOException {

        // Do absolutely nothing
        logger.debug("wakeUp: not implemented");
    }

    /**
     * The reason for existence of this class is that {@link AbstractServoController#sleep()} and {@link AbstractServoController#wakeUp()}
     * operations can't be exposed via implemented interface without violating the target integrity. 
     */
    private class ControllerSilencer implements SilentProxy {

        @Override
        public void sleep() {

            NDC.push("sleep");

            try {

                AbstractServoController.this.sleep();
                AbstractServoController.this.silentStatusChanged(false);

            } catch (IOException ioex) {

                AbstractServoController.this.exception(ioex);

            } finally {
                NDC.pop();
            }
        }

        @Override
        public void wakeUp() {

            NDC.push("wakeUp");

            try {

                AbstractServoController.this.wakeUp();
                AbstractServoController.this.silentStatusChanged(true);

            } catch (IOException ioex) {

                AbstractServoController.this.exception(ioex);

            } finally {
                NDC.pop();
            }
        }
    }
}
