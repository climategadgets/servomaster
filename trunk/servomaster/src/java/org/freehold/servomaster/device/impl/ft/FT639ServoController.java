package org.freehold.servomaster.device.impl.ft;

import java.io.IOException;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;

import javax.comm.UnsupportedCommOperationException;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoListener;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.device.model.silencer.SilentProxy;
import org.freehold.servomaster.device.impl.serial.AbstractSerialServoController;

/**
 * <a href="http://www.ferrettronics.com/product639.html"
 * target="_top">FerretTronics FT639</a> servo controller implementation.
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
 * is not going to help at all, so I believe I did The Right Thing
 * <small><sup>TM</sup></small>.
 *
 * <p>
 *
 * Additional argument: making this object <strong>not</strong> a singleton
 * allows to make it a factory object with a possibility of proper
 * subclassing a {@link #createServo template method}, thus allowing to
 * extend the functionality without rewriting half of the code.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: FT639ServoController.java,v 1.36 2005-01-21 05:45:27 vtt Exp $
 */
public class FT639ServoController extends AbstractSerialServoController implements FT639Constants {

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
     * The current range value.
     *
     * @see #setRange
     */
    protected boolean range = false;
    
    /**
     * True if the heartbeat thread is repositioning the servos now.
     */
    private boolean repositioningNow = false;
    
    /**
     * Metadata instance.
     */
    private Meta meta;
    
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
    
        checkInit();
        
        setSetupMode();
        this.range = range;

        send(range ? PULSE_LONG : PULSE_SHORT);
        
        repositionServos();
    }
    
    /**
     * {@inheritDoc}
     */
    public int getServoCount() {
    
        return 5;
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
    protected Servo createServo(int id) throws IOException {
    
        // VT: NOTE: There is no sanity checking, I expect the author of the
        // calling code to be sane - this is a protected method
    
        return new FT639Servo(this, id);
    }

    /**
     * Switch the controller into setup mode.
     *
     * @exception IOException if there was a problem with getting access to
     * the port.
     */
    protected synchronized void setSetupMode() throws IOException {
    
        checkInit();
    
        if ( !activeMode ) {
        
            // We're already in setup mode
            
            return;
        }
        
        send(MODE_SETUP);
        
        activeMode = false;
        
        //System.err.println("mode: setup");
        //new Exception("setup").printStackTrace();

        // VT: FIXME: Do I have a right to do this every time or I have to
        // rely on the silencer?

        silentStatusChanged(false);
    }

    /**
     * Switch the controller into active mode.
     *
     * @exception IOException if there was a problem with getting access to
     * the port.
     */
    protected synchronized void setActiveMode() throws IOException {
    
        checkInit();
    
        if ( activeMode ) {
        
            // We're already in active mode
            
            return;
        }
        
        send(MODE_ACTIVE);
        
        activeMode = true;

        touch();
        
        //System.err.println("mode: active");
        //new Exception("active").printStackTrace();

        // VT: FIXME: Do I have a right to do this every time or I have to
        // rely on the silencer?

        silentStatusChanged(true);
    }
    
    /**
     * Render the positioning command to be sent to the hardware.
     *
     * @param id Servo id to render the position for.
     *
     * @param position Position to render.
     *
     * @return Two byte array representing FT639 positioning command.
     */
    private byte[] renderPositionCommand(int id, int position) {
    
        // The sanity check was supposed to be done by now
        
        byte servo = (byte)(id << 4);
        
        
        byte upper = (byte)((((position >> 4) & 0x0F) | 0x80) | servo);
        byte lower = (byte)((position & 0x0F) | servo);
        
        //System.out.println(Integer.toHexString(servo) + " [" + Integer.toHexString((int)lower) + ", " + Integer.toHexString(((int)upper) & 0xFF) + "]");
        
        byte result[] = { lower, upper };
        
        return result;
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
    
        checkInit();
    
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
    
        checkInit();
    
        // Since we don't know the controller mode (some other application
        // might have been controlling it and left it in active mode), we'll
        // fake the active mode and make it forcibly go into setup mode (see
        // the activeMode declaration)
        
        activeMode = true;
        //setSetupMode(); implied by setRange
        setRange(range);
    }
    
    public synchronized Meta getMeta() {
    
        if ( meta == null ) {
        
            meta = new FT639Meta();
        }
        
        return meta;
    }
    
    protected class FT639Meta extends AbstractMeta {
    
        public FT639Meta() {
        
            properties.put("manufacturer/name", "FerretTronics");
            properties.put("manufacturer/URL", "http://www.ferrettronics.com/");
            properties.put("manufacturer/model", "FT639");

            features.put("controller/allow_disconnect", new Boolean(true));
            features.put("controller/silent", new Boolean(true));
            features.put("controller/protocol/serial", new Boolean(true));
            
            properties.put("controller/maxservos", "5");
            
            // 2400 baud max
            // 2 bytes per command
            
            properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
            properties.put("controller/precision", "256");
            
            // Silent timeout is five seconds

            properties.put("controller/silent", "5000");
            
            // Default range is 90 degrees
            // Warning: this is an FT639 specific property
            
            properties.put("controller/range", "90");
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
         * @param sc The servo controller that owns this servo.
         *
         * @param id The servo ID.
         *
         * @exception IOException if there was a problem communicating to
         * the hardware controller.
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
        
        protected void setActualPosition(double position) throws IOException {
        
            checkInit();
            checkPosition(position);
            
            int requestedPosition = double2int(position);
            
            if ( isLazy() && !repositioningNow ) {

                // Let's see if we really have to do it
                
                if ( double2int(this.actualPosition) == requestedPosition ) {
                
                    // Nah, we don't have to bother.
                    
                    // Chances are that if we're going to go ahead with it, the
                    // time spent on transmitting the control signal is going to
                    // be much more than spent in double2int().
                    
                    //System.err.println("Redundant position change request: #" + id + " at " + position + " (" + requestedPosition + ")");
                    return;
                }
            }
            
            synchronized ( getController() ) {

                // The reason it is synchronized on the controller is that the
                // setActualPosition() calls the controller's synchronized methods
                // and the deadlock can occur if *this* method was made synchronized
                
                setActiveMode();
                send(renderPositionCommand(id, requestedPosition));
                this.actualPosition = position;
            }
            
            actualPositionChanged();
            
            // FIXME: Again, this stupid problem I forgot the solution of:
            // can't access the outer class. Oh well.
            
            _touch();
        }

        public void setRange(int range) {
        
            throw new UnsupportedOperationException("This operation is controller-specific for FT639, you have to invoke it on the controller");
        }
        
        private static final double step = 1.0 / 255.0;
        
        public Meta getMeta() {
        
            return new FT639ServoMeta();
        }

        protected class FT639ServoMeta extends AbstractMeta {
        
            public FT639ServoMeta() {
            
                // VT: FIXME: Check if there are other properties
                
                properties.put("servo/precision", "256");
            }
        }
    }

    /**
     * Wrapper for <code>touch()</code>.
     *
     * Required for the inner class to be able to access the outer.
     */
    private void _touch() {
    
        touch();
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
        
        repositioningNow = true;
        
        try {
        
            for ( Iterator i = getServos(); i.hasNext(); ) {
            
                FT639Servo s = (FT639Servo)i.next();
                
                s.setActualPosition(s.getPosition());
            }

        } finally {
        
            repositioningNow = false;
        }
    }
    
    private static int double2int(double value) {
    
        return (int)(value * 255);
    }

    protected SilentProxy createSilentProxy() {
    
        return new FT639SilentProxy();
    }
    
    private void _silentStatusChanged(boolean mode) {
    
        silentStatusChanged(mode);
    }
    
    /**
     * Wrapper for {@link AbstractServoController#exception exception()}
     */
    private void _exception(Throwable t) {
    
        exception(t);
    }
    
    protected class FT639SilentProxy implements SilentProxy {
    
        public synchronized void sleep() {
        
            try {
            
                setSetupMode();
                _silentStatusChanged(false);
                
            } catch ( IOException ioex ) {
            
                _exception(ioex);
            }
        }
        
        public synchronized void wakeUp() {
        
            try {
            
                if ( !activeMode ) {
                
                    reset();
                    _silentStatusChanged(true);
                }
                
            } catch ( IOException ioex ) {
            
                _exception(ioex);
            }
        }
    }
}
