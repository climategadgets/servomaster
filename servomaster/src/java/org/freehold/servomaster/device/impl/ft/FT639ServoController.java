package org.freehold.servomaster.device.impl.ft;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.ServoMetaData;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerMetaData;
import org.freehold.servomaster.device.model.ServoListener;
import org.freehold.servomaster.device.model.ServoControllerListener;

/**
 * <a href="http://www.ferrettronics.com/product639.html">FerretTronics
 * FT639</a> servo controller implementation.
 *
 * <p>
 *
 * The product itself is quite limited, but working.
 *
 * <p>
 *
 * Misses:
 *
 * <ul>
 *
 * <li> Supports just RS-232. I wish it supported USB.
 * 
 * <li> Supports just 2400 baud speed on the serial port. Clearly not enough.
 *
 * <li> Does not support the smooth transition.
 *
 * <li> Is a write-only device, there's no way to read the status. I'd like
 *      to get an acknowledgement about the very fact that the controller
 *      itself is there, as well as acknowledgement of the fact that such
 *      and such servos are physically present.
 *
 * </ul>
 *
 * Benefits:
 *
 * <ul>
 *
 * <li> <a href="http://www.ferrettronics.com/product649.html">Stackable</a>
 *
 * <li> Pretty simple to implement and to work with.
 *
 * <li> Allows to adjust the initial servo positions
 *
 * </ul>
 *
 * <h3>Implementation note</h3>
 *
 * Even though the serial port and the controller hardware itself are
 * singletons, this class doesn't use singletons. The reason is: I hate
 * them. I prefer you to understand what you're doing and be responsible in
 * how you deal with it. All in all, if you really want to screw up, you
 * will just start multiple JVMs or try to access the serial port from a
 * C/Perl/etc. application. In any case, making this controller a singleton
 * is not going to help at all, so I believe I did The Right Thing &tm;.
 *
 * <p>
 *
 * Additional argument: making this object <strong>not</strong> a singleton
 * allows to make it a factory object with a possibility of proper
 * subclassing a {@link #createServo template method}, thus allowing to
 * extend the functionality without rewriting half of the code.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: FT639ServoController.java,v 1.12 2002-01-02 09:11:18 vtt Exp $
 */
public class FT639ServoController implements ServoController, FT639Constants {

    /**
     * The serial port the controller is connected to.
     */
    private String portName;
    
    /**
     * Open timeout.
     *
     * <p>
     *
     * Wait this much trying to open the serial port.
     */
    public static final int OPEN_TIMEOUT = 5000;
    
    /**
     * Physical servo representation.
     *
     * There is just up to 5 servos that can be connected to this device.
     */
    private Servo servoSet[] = new Servo[5];
    
    /**
     * The serial port.
     */
    private SerialPort port = null;
    
    /**
     * The serial port output stream.
     */
    private OutputStream serialOut;
    
    /**
     * Controller mode.
     *
     * <code>false</code> is setup, <code>true</code> is active.
     *
     * When the controller is powered up, it goes into setup mode, so are
     * we. But! We don't know if there was another application that could
     * have left the controller in active mode, so we'll pretend the mode is
     * active and make the constructor forcibly reset it. Thus, we go into
     * setup mode after being instantiated.
     */
    private boolean activeMode = true;
    
    /**
     * Controller silence mode.
     *
     * Set to <code>true</code> if the silent mode is on. Default is off.
     */
    private boolean silent = false;
    
    /**
     * Silent timeout, in milliseconds.
     *
     * Defines how long the servos stay energized after the last positioning
     * operation in the silent mode. When this time expires, the controller
     * is set in {@link #setSetupMode setup mode}, thus stopping the control
     * pulse.
     *
     * <p>
     *
     * Default is 10 seconds.
     */
    private long silentTimeout = 10000;
    
    /**
     * Silent timeout watcher.
     *
     * Watches the timeout expiration.
     */
     private Silencer silencer = null;
     
     /**
      * Last time when the positioning operation was performed.
      */
     private long lastOperation = System.currentTimeMillis();
     
    /**
     * The listener set.
     */
    private Set listenerSet = new HashSet();
    
    /**
     * The current range value.
     *
     * @see #setRange
     */
    protected boolean range = false;
    
    /**
     * Create the controller instance.
     *
     * <p>
     *
     * The reason the no-argument constructor is here is to allow
     * instantiating the controller with <code>Class.newInstance()</code>. 
     * The instance created in such a manner is not functional, and {@link
     * #init(java.lang.String) init(portName)} has to be called to make it
     * functional. Otherwise, <code>IllegalStateException</code> will be
     * thrown on every method call.
     */
    public FT639ServoController() {
    
    }
    
    /**
     * Create the controller instance.
     *
     * <p>
     *
     * <strong>You have to be careful not to try to connect the controller
     * to the port that doesn't have the FT639 connected. There is no way to
     * determine if the controller is there or not.</strong>
     *
     * <h3>Implementation note</h3>
     *
     * The controller instance puts the port into exclusive mode, for
     * safety's sake. You have to specifically release the port if you want
     * another entity to access it before the JVM terminates, though I don't
     * see much sense in doing so.
     *
     * @param portName The name of the serial port the controller is
     * connected to.
     *
     * @exception IOException if there was a problem with getting access to
     * the port.
     */
    public FT639ServoController(String portName) throws IOException {
    
        init(portName);
    
    }
    
    public synchronized void init(String portName) throws IOException {
    
        if ( this.portName != null ) {
        
            throw new IllegalStateException("Already initialized");
        }

        this.portName = portName;

        try {
        
            // This is a stupid way to do it, but oh well, "release early"
    
            Vector portsTried = new Vector();
            
            for ( Enumeration ports = CommPortIdentifier.getPortIdentifiers(); ports.hasMoreElements(); ) {
            
                CommPortIdentifier id = (CommPortIdentifier)ports.nextElement();
                
                // In case we fail, we'd like to tell the caller what's
                // available
                
                portsTried.addElement(id.getName());
                
                if ( id.getPortType() == CommPortIdentifier.PORT_SERIAL ) {
                
                    if ( id.getName().equals(portName) ) {
                    
                        try {
                        
                            port = (SerialPort)id.open(getClass().getName(), OPEN_TIMEOUT);
                            
                        } catch ( PortInUseException piuex ) {
                        
                            throw new IOException("Port in use: " + piuex.toString());
                        }
                        
                        break;
                    }
                }
            }
            
            if ( port == null ) {
            
                throw new IllegalArgumentException("No suitable port found, tried: " + portsTried);
            }
            
            serialOut = port.getOutputStream();
            port.setSerialPortParams(2400,
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);

        } catch ( UnsupportedCommOperationException ucoex ) {
        
            // VT: FIXME: Bastards, there's no nested exception until JDK
            // 1.4...
        
            throw new IOException("Unsupported comm operation: " + ucoex.toString());
        }
        
        reset();
    }

    /**
     * Enable the long throw for the servos.
     *
     * Default is 90\u00B0 range.
     *
     * <p>
     *
     * Be careful with the long throw, not all servos support it.
     *
     * @param range <code>false</code> for 90\u00B0 range,
     * <code>true</code> for 180\u00B0 range.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    public synchronized void setRange(boolean range) throws IOException {
    
        setSetupMode();
        this.range = range;

        send(range ? PULSE_LONG : PULSE_SHORT);
        
        repositionServos();
        
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
     */
    public synchronized Servo getServo(String id) throws IOException {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        try {
        
            int iID = Integer.parseInt(id);
            
            if ( iID < 0 || iID > 4 ) {
            
                throw new IllegalArgumentException("ID out of 0...4 range: '" + id + "'");
            }
            
            synchronized ( servoSet ) {
            
                if ( servoSet[iID] == null ) {
                
                    servoSet[iID] = createServo(iID);
                }
                
                return servoSet[iID];
            }
            
        } catch ( NumberFormatException nfex ) {
        
            throw new IllegalArgumentException("Not a number: '" + id + "'");
        }
    
    }
    
    public Iterator getServos() throws IOException {
    
        LinkedList servos = new LinkedList();
        
        for ( int idx = 0; idx < 5; idx++ ) {
        
            servos.add(getServo(Integer.toString(idx)));
        }
        
        return servos.iterator();
    }
    
    /**
     * Create the servo instance.
     *
     * This is a template method used to instantiate the proper servo implementation class.
     *
     * @param id Servo ID to create.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected Servo createServo(int id) throws IOException {
    
        // VT: NOTE: There is no sanity checking, I expect the author of the
        // calling code to be sane - this is a protected method
    
        return new FT639Servo(this, id);
    }

    /**
     * Check if the value is within 0...1.0 range.
     *
     * @param position Value to check.
     *
     * @exception IllegalArgumentException if the position is out of 0...1.0
     * range.
     */
    protected void checkPosition(double position) {
    
        if ( position < 0 || position > 1.0 ) {
        
            throw new IllegalArgumentException("Position out of 0...1.0 range: " + position);
        }
    }
    
    /**
     * Switch the controller into setup mode.
     *
     * @exception IOException if there was a problem with getting access to
     * the port.
     */
    protected synchronized void setSetupMode() throws IOException {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        if ( !activeMode ) {
        
            // We're already in setup mode
            
            return;
        }
        
        send(MODE_SETUP);
        
        activeMode = false;

        //System.err.println("mode: setup");
        silentStatusChanged();
    }

    /**
     * Switch the controller into active mode.
     *
     * @exception IOException if there was a problem with getting access to
     * the port.
     */
    protected synchronized void setActiveMode() throws IOException {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        startTimeout();
        
        if ( activeMode ) {
        
            // We're already in active mode
            
            return;
        }
        
        send(MODE_ACTIVE);
        
        activeMode = true;

        //System.err.println("mode: active");
        silentStatusChanged();
    }
    
    private byte[] renderPositionCommand(int id, int position) {
    
        // The sanity check was supposed to be done by now
        
        byte servo = (byte)(id << 4);
        
        
        byte upper = (byte)((((position >> 4) & 0x0F) | 0x80) | servo);
        byte lower = (byte)((position & 0x0F) | servo);
        
        //System.out.println(Integer.toHexString(servo) + " [" + Integer.toHexString((int)lower) + ", " + Integer.toHexString(((int)upper) & 0xFF) + "]");
        
        byte result[] = { lower, upper };
        
        return result;
    }
    
    public synchronized void setSilentMode(boolean silent) {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        this.silent = silent;
        
        if ( !silent ) {
        
            for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
            
                ((ServoControllerListener)i.next()).silentStatusChanged(this, true);
            }
        }
        
        startTimeout();
    }
    
    public void setSilentTimeout(long timeout) {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        if ( timeout <= 0 ) {
        
            throw new IllegalArgumentException("Timeout must be positive");
        }
    
        silentTimeout = timeout;
        startTimeout();
    }
    
    private synchronized void startTimeout() {
    
        // VT: NOTE: This is a little bit inefficient when not in silent
        // mode, but with 2400 baud... the hell with it
        
        lastOperation = System.currentTimeMillis();
        
        if ( !silent ) {
        
            return;
        }
        
        if ( silencer == null ) {
        
            silencer = new Silencer();
            silencer.start();

        }
        
        //System.err.println("Silent timer updated");
    }
    
    /**
     * Send the byte array down the {@link #serialOut serial port stream}.
     *
     * @param b Byte array to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    private void send(byte b[]) throws IOException {
    
        // VT: FIXME: Can be optimized
        
        for ( int offset = 0; offset < b.length; offset++ ) {
        
            serialOut.write(b[offset]);
        }
        
        serialOut.flush();
    }
    
    /**
     * Send the byte down the {@link #serialOut serial port stream}.
     *
     * @param b Byte to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    private void send(byte b) throws IOException {
    
        serialOut.write(b);
        serialOut.flush();
    }
    
    /**
     * Adjust the initial position.
     *
     * <p>
     *
     * This operation is speficic to FT639, and has to be executed with
     * care. See the <a href="../../../../../../docs/FT639.html"
     * target="_top">implementation notes</a>.
     *
     * @param headerLength A value indicating the initial servo position.
     */
    public synchronized void setHeaderLength(int headerLength) throws IOException {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        if ( headerLength < 0 || headerLength > 15 ) {
        
            throw new IllegalArgumentException("Header length outside of 0...15 range: " + headerLength);
        }
        
        setSetupMode();
        
        // VT: FIXME: account for the pulse length?
        
        headerLength |= 0x60;
        
        System.err.println("Trim: " + headerLength + ": 0x" + Integer.toHexString(headerLength));
        
        send((byte)headerLength);
        
        repositionServos();
    }
    
    public void reset() throws IOException {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        // Since we don't know the controller mode (some other application
        // might have been controlling it and left it in active mode), we'll
        // fake the active mode and make it forcibly go into setup mode (see
        // the activeMode declaration)
        
        activeMode = true;
        setSetupMode();
        setRange(range);
    }
    
    public synchronized void addListener(ServoControllerListener listener) {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        listenerSet.add(listener);
    }
    
    private void silentStatusChanged() {
    
        if ( !silent ) {
        
            return;
        }
    
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ServoControllerListener)i.next()).silentStatusChanged(this, activeMode);
        }
    }
    
    public synchronized void removeListener(ServoControllerListener listener) {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    
        if ( !listenerSet.contains(listener) ) {
        
            throw new IllegalArgumentException("Not a registered listener: "
                                               + listener.getClass().getName()
                                               + "@"
                                               + listener.hashCode());
        }
        
        listenerSet.remove(listener);
    }
    
    public ServoControllerMetaData getMetaData() {
    
        return new FT639MetaData();
    }

    protected class FT639MetaData implements ServoControllerMetaData {
    
        public String getManufacturerURL() {
        
            return "http://www.ferrettronics.com/";
        }
        
        public String getManufacturerName() {
        
            return "FerretTronics";
        }
        
        public String getModelName() {
        
            return "FT639";
        }
        
        public int getMaxServos() {
        
            return 5;
        }
        
        public boolean supportsSilentMode() {
        
            return true;
        }
        
        public int getPrecision() {
        
            return 256;
        }
    }
    
    /**
     * The servo implementation.
     *
     * <p>
     *
     * There is no need to check whether the controller has been initialized
     * - this check is done before the servo instance can be obtained.
     */
    public class FT639Servo extends AbstractServo {
    
        /**
         * The servo id, 0 to 4.
         */
        private int id;
        
        /**
         * Create an instance.
         *
         * @param id The servo ID.
         */
        protected FT639Servo(ServoController sc, int id) throws IOException {
        
            super(sc, null);
        
            // Sanity checking is performed by the controller class
            
            this.id = id;
            
            // Reset the servo position
            setPosition((255 >> 1)/255.0);
        }
        
        public String getName() {
        
            return Integer.toString(id);
        }
        
        protected synchronized void setActualPosition(double position) throws IOException {
        
            checkPosition(position);
            setActiveMode();
            send(renderPositionCommand(id, double2int(position)));
            this.actualPosition = position;
            actualPositionChanged();
            
            startTimeout();
        }

        public void setRange(int range) {
        
            throw new UnsupportedOperationException("This operation is controller-specific for FT639, you have to invoke it on the controller");
        }
        
        private static final double step = 1.0 / 255.0;
        
        /*
        protected class TransitionController implements Runnable {
        
            public void run() {
            
                // VT: NOTE: Since the input is in doubles, and the actual
                // control values are integers, we need some slack so
                // there's no jitter on the software level (which does
                // happen if I just compare the position and the actual
                // position). The slack is equal to 1 divided by the
                // controller precision. Since we know for sure that it is
                // 255, we can skip the calculation and declare it final
                // (above).
                
                while ( true ) {
                
                    double diff = position - actualPosition;
                    
                    if ( diff < 0 ) {
                    
                        diff = -diff;
                    }
                    
                    if ( diff <= step ) {
                    
                        break;
                    }
                    
                    try {
                    
                        // VT: NOTE: Remember not to interfere with the
                        // position variables!
                    
                        synchronized ( this ) {
                        
                            if ( actualPosition > position ) {
                            
                                setActualPosition(actualPosition - step);
                                
                            } else if ( actualPosition < position ) {
                            
                                setActualPosition(actualPosition + step);
                            }
                        }
                    
                    } catch ( Throwable t ) {
                    
                        // VT: FIXME: Have to process this smarter. I guess
                        // I'll attempt to reset the controller first, and
                        // if that fails, I have to have the error flag in
                        // the controller itself that will cause it to stop
                        // operating.
                        
                        System.err.println("Transition problem:");
                        t.printStackTrace();
                    }
                }
                
                //System.err.println("Transition done");
                transitionController = null;
            }
        }
        
        */
        
        public ServoMetaData[] getMetaData() {
        
            throw new UnsupportedOperationException("Capabilities discovery is not supported");
        }
    }
    
    /**
     * Time left to wait before going into the silent mode.
     */
     
    protected long waitTime() {
    
        return (lastOperation + silentTimeout) - System.currentTimeMillis();
    }
    
    protected class Silencer extends Thread {
    
        /**
         * Wait until timeout expires, then put the controller into a setup
         * mode.
         */
        public synchronized void run() {
        
            try {
            
                while ( waitTime() > 0 ) {
                
                    //System.err.println("waiting for " + waitTime() + "ms...");
                    wait(waitTime());
                    //System.err.println("checking... " + waitTime() + " left");
                }
                
                synchronized ( this ) {
                
                    //System.err.println("Sleeping now.");
                    setSetupMode();
                    silencer = null;
                }
            
            } catch ( Throwable t ) {
            
                // Oh shit... Okay. Let's get out of here.

                System.err.println("Silencer:");
                t.printStackTrace();
                
                return;
            }
        }
    }
    
    /**
     * Make sure that the servos are at the places they're supposed to be.
     *
     * <p>
     *
     * Sometimes, the actual position of the servo is not the one requested,
     * in particular, this happens after the controller reset (software
     * thing) and the header length change (hardware implementation). In
     * order to make sure that the servos are where they are supposed to be,
     * we'll just get the position and set it to the same value.
     */
    private void repositionServos() throws IOException {
    
        // Now that we've taken care of the range, let's reset the servo
        // position
        
        for ( Iterator i = getServos(); i.hasNext(); ) {
        
            Servo s = (Servo)i.next();
            
            s.setPosition(s.getPosition());
        }
    }
    
    private static int double2int(double value) {
    
        return (int)(value * 255);
    }
}
