package org.freehold.servomaster.device.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.freehold.servomaster.device.model.silencer.SilentHelper;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: AbstractServoController.java,v 1.7 2005-01-14 02:59:20 vtt Exp $
 */
abstract public class AbstractServoController implements ServoController {

    /**
     * The port the controller is connected to.
     *
     * This variable contains the device-specific port name.
     */
    protected String portName;
    
    /**
     * The listener set.
     */
    private Set listenerSet = new HashSet();
    
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
    protected Servo servoSet[];
    
    public AbstractServoController() {
    
    }
    
    public synchronized final void init(String portName) throws IOException {
    
        if ( this.portName != null ) {
        
            throw new IllegalStateException("Already initialized");
        }
        
        doInit(portName);
    
        try {
        
            if ( getMeta().getFeature("controller/silent") ) {
            
                silencerProxy = createSilentProxy();
                silencer = new SilentHelper(silencerProxy);
                silencer.start();
            }
            
        } catch ( UnsupportedOperationException uoex ) {
        
            // They don't want to play nice, fine :(

        } catch ( IllegalStateException isex ) {
        
            // Ditto
        }
    }
    
    /**
     * Perform the actual initialization.
     */
    abstract protected void doInit(String portName) throws IOException;
    
    /**
     * Check if the controller is initialized.
     *
     * @exception IllegalStateException if the controller is not yet
     * initialized.
     */
    abstract protected void checkInit();
    
    public void setLazyMode(boolean lazy) {
    
        throw new UnsupportedOperationException();
    }

    public boolean isLazy() {
    
        return false;
    }
    
    public synchronized void addListener(ServoControllerListener listener) {
    
        checkInit();
    
        listenerSet.add(listener);
    }
    
    public synchronized void removeListener(ServoControllerListener listener) {
    
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
     * @param mode The new silent status. <code>false</code> means device is
     * sleeping, <code>true</code> means device is active.
     */
    protected void silentStatusChanged(boolean mode) {
    
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ServoControllerListener)i.next()).silentStatusChanged(this, mode);
        }
    }
    
    /**
     * Notify the listeners about the problem that occured.
     *
     * @param t The exception to broadcast.
     */
    protected void exception(Throwable t) {
    
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ProblemListener)i.next()).exception(this, t);
        }
    }
    
    public void setSilentTimeout(long timeout, long heartbeat) {
    
        checkInit();
    
        if ( silencer == null ) {
        
            throw new UnsupportedOperationException();
        }
        
        silencer.setSilentTimeout(timeout, heartbeat);
    }
    
    public void setSilentMode(boolean mode) {
    
        checkInit();
    
        if ( silencer == null ) {
        
            throw new UnsupportedOperationException();
        }
        
        boolean oldMode = getSilentMode();
        
        silencer.setSilentMode(mode);
        
        if ( mode != oldMode ) {
        
            silentStatusChanged(isSilentNow());
        }
        
        touch();
    }
    
    public boolean getSilentMode() {
    
        return (silencer == null) ? false : silencer.getSilentMode();        
    }
    
    public boolean isSilentNow() {
    
        return (silencer == null) ? false : silencer.isSilentNow();
    }

    public Meta getMeta() {
    
        throw new UnsupportedOperationException();
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
    abstract public boolean isConnected();
    
    public void deviceArrived(ServoController device) {
    
        System.err.println("deviceArrived is not implemented by " + getClass().getName());
    }

    public void deviceDeparted(ServoController device) {
    
        System.err.println("deviceDeparted is not implemented by " + getClass().getName());
    }
    
    /**
     * Update the silent helper timestamp.
     *
     * This method is critical to properly support the silent mode. It
     * should be called every time the operation that should keep the
     * controller energized for some more ({@link #reset reset()}, {@link
     * Servo#setPosition Servo.setPosition()}) is performed.
     */
    protected void touch() {
    
        if ( silencer != null ) {
        
            silencer.touch();
        }
    }
    
    /**
     * Create the silencer proxy.
     *
     * This is a template method because the specific means of controlling
     * the sleep mode are controller-specific.
     */
    abstract protected SilentProxy createSilentProxy();

    /**
     * @exception IllegalStateException if the controller wasn't previously
     * initialized.
     */
    public final Iterator getServos() throws IOException {
    
        checkInit();
    
        LinkedList servos = new LinkedList();
        
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
        
            throw new IllegalArgumentException("Not a number: '" + id + "'");
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
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    abstract protected Servo createServo(int id) throws IOException;
}
