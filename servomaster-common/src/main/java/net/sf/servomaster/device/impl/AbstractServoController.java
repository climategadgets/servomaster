package net.sf.servomaster.device.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
     * String key to retrieve the silent support feature.
     */
    public static final String META_SILENT = "controller/silent";

    /**
     * The port the controller is connected to.
     *
     * This variable contains the device-specific port name.
     */
    protected String portName;

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
     * The silencer proxy.
     */
    private SilentProxy silencerProxy;

    /**
     * Physical servo representation.
     */
    protected Servo[] servoSet;

    protected AbstractServoController() {

    }

    protected AbstractServoController(String portName) throws IOException {

        init(portName);
    }

    @Override
    public final String getPort() {

        checkInit();

        return portName;
    }

    @Override
    public final synchronized void init(String portName) throws IOException {

        if ( this.portName != null ) {

            throw new IllegalStateException("Already initialized");
        }

        doInit(portName);

        try {

            if ( getMeta().getFeature("controller/silent") ) {

                silencerProxy = new SilentGuard();
                silencer = new SilentHelper(silencerProxy);
                silencer.start();
            }

        } catch ( UnsupportedOperationException ex ) {

            // VT: NOTE: In this particular case, it's OK to suppress the exception trace - it's a part of the contract
            logger.info("controller doesn't support silent operation, reason: " + ex.getMessage());

        } catch ( IllegalStateException ex ) {

            logger.warn("unexpected exception", ex);
        }
    }

    /**
     * Perform the actual initialization.
     *
     * @param portName Port to initialize with.
     * @throws IOException if there was a hardware error.
     */
    protected abstract void doInit(String portName) throws IOException;

    /**
     * Check if the controller is initialized.
     *
     * @exception IllegalStateException if the controller is not yet
     * initialized.
     */
    protected abstract void checkInit();

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

        boolean silentSupport = controllerMeta.getFeature(META_SILENT);

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

        return (silencer == null) ? false : silencer.getSilentMode();
    }

    public final boolean isSilentNow() {

        return (silencer == null) ? false : silencer.isSilentNow();
    }

    @Override
    public Meta getMeta() {

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

        this.disconnected = disconnected;
    }

    /**
     * Check the disconnected mode.
     *
     * @return true if the controller driver can function if the device is not connected.
     */
    public boolean isDisconnectAllowed() {

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

    /**
     * @exception IllegalStateException if the controller wasn't previously
     * initialized.
     */
    @Override
    public final Iterator<Servo> getServos() throws IOException {

        checkInit();

        List<Servo> servos = new LinkedList<Servo>();

        for ( int idx = 0; idx < getServoCount(); idx++ ) {

            servos.add(getServo(Integer.toString(idx)));
        }

        return servos.iterator();
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
    
    public void close() throws IOException {
        
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
    private class SilentGuard implements SilentProxy {

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
