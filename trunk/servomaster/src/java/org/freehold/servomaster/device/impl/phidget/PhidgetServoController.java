package org.freehold.servomaster.device.impl.phidget;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import usb.core.Bus;
import usb.core.ControlMessage;
import usb.core.Descriptor;
import usb.core.Device;
import usb.core.DeviceDescriptor;
import usb.core.Host;
import usb.core.HostFactory;

import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoMetaData;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerMetaData;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

/**
 * <a
 * href="http://www.cpsc.ucalgary.ca/grouplab/phidgets/phidget-servo/phidget-servo.html"
 * target="_top">PhidgetServo</a> controller.
 *
 * Detailed documentation to follow.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: PhidgetServoController.java,v 1.5 2002-05-13 03:20:05 vtt Exp $
 */
public class PhidgetServoController extends AbstractServoController {

    /**
     * The revision to protocol handler map.
     *
     * <p>
     *
     * The key is the revision, the value is the protocol handler. This is a
     * little bit of overhead, but adds flexibility.
     *
     * <p>
     *
     * At the instantiation time, the protocol handlers for all known
     * hardware revisions are instantiated and put into this map.
     *
     * <p>
     *
     * At the {@link #init init()} time, the hardware revision is looked up,
     * the proper protocol handler resolved and assigned to the {@link
     * #protocolHandler instance protocol handler}.
     */
    private Map protocolHandlerMap = new HashMap();
     
    /**
     * The USB device object corresponding to the servo controller.
     */
    private Device thePhidgetServo;
    
    /**
     * The protocol handler taking care of this specific instance.
     *
     * @see #init
     *
     */
    private ProtocolHandler protocolHandler;
    
    /**
     * Physical servo representation.
     *
     * There is just up to 4 servos that can be connected to this device.
     */
    private Servo servoSet[] = new Servo[4];
    
    /**
     * Current servo position in device coordinates.
     */
    private int servoPosition[] = new int[4];
    
    /**
     * 'sent' flag.
     *
     * This is an optimization measure - since the positions for all the
     * servos are transmitted at once, it would make a lot of sense to
     * ensure that if there was more than one coordinate assignment at about
     * the same time, they all get sent together.
     *
     * @see #bufferPosition
     * @see #send
     */
    private boolean sent = false;
    
    /**
     * Default constructor.
     *
     * Provided for <code>Class.newInstance()</code> to be happy.
     */
    public PhidgetServoController() {
    
        protocolHandlerMap.put("0.03", new ProtocolHandler003());
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
    public synchronized boolean isConnected() {
    
        checkInit();
        
        if ( thePhidgetServo != null ) {
        
            // We can be reasonably sure that we're connected
            
            return true;

        } else {
        
            // Let's find out
            
            try {
            
                thePhidgetServo = findUSB(portName);

                return true;

            } catch ( IOException ioex ) {
            
                // Uh oh, I don't think so
                
                exception(ioex);
                return false;
            }
        }
    }
    
    /**
     * Initialize the controller.
     *
     * @param portName The controller board unique serial number in a string
     * representation. If this is null, then all the PhidgetServo devices
     * connected will be found. If the only device is found, then it is
     * used, and its serial number will be assigned to
     * <code>portName</code>.
     *
     * @exception IllegalArgumentException if the <code>portName</code> is
     * null and none or more than one device were found, or the device
     * corresponding to the name specified is not currently connected and
     * {@link #allowDisconnect disconnected mode} is not enabled.
     *
     * @exception UnsupportedOperationException if the device revision is
     * not supported by this driver.
     */
    public synchronized void init(String portName) throws IOException {
    
        if ( this.portName != null ) {
        
            throw new IllegalStateException("Already initialized");
        }
        
        try {
        
            thePhidgetServo = findUSB(portName);
        
            // At this point, we've either flying by on the wings of
            // IllegalArgumentException (null portName, none or more than
            // one device), or the phidget serial contains the same value as
            // the requested portName. However, in disconnected mode we can
            // safely assume that the device portName is the same passed
            // from the caller, and just assign it - we don't care if they
            // made a typo
            
            int languageSet[] = ControlMessage.getLanguages(thePhidgetServo);
            int defaultLanguage = 0;
            
            if ( languageSet != null && languageSet.length != 0 ) {
            
                defaultLanguage = languageSet[0];
            }
            
            DeviceDescriptor dd = thePhidgetServo.getDeviceDescriptor();
            String serial = dd.getSerial(defaultLanguage);
            String revision = dd.getDeviceId();
            
            protocolHandler = (ProtocolHandler)protocolHandlerMap.get(revision);
            
            if ( protocolHandler == null ) {
            
                throw new UnsupportedOperationException("Revision '" + revision + "' is not supported");
            }
            
            this.portName = serial;
            connected = true;
        
        } catch ( IOException ioex ) {
        
            exception(ioex);
        
            if ( isDisconnectAllowed() && (portName != null) ) {
                
                    // No big deal, let's just continue
                    
                    this.portName = portName;
                    
                    // FIXME: Think about better solution
                    
                    synchronized ( System.err ) {
                    
                        System.err.println("Working in the disconnected mode, cause:");
                        ioex.printStackTrace();
                    }
                    
                    return;
            }
            
            // Too bad, we're not in the disconnected mode
            
            throw ioex;
        }
        
        // VT: FIXME: Since right now the controller is write-only box and
        // we can't get the current servo position, I'd better set them to a
        // predefined position, otherwise they're going to stay where they
        // were when we ordered a positioning last time, and this might
        // wreak havoc on some calculations (in particular, timing coupled
        // with listeners).
        
        // FIXME: set the servos to 0.5 now...
        
        for ( Iterator i = getServos(); i.hasNext(); ) {
        
            Servo s = (Servo)i.next();
            
            s.setPosition(0.5);
        }
    }
    
    public ServoControllerMetaData getMetaData() {
    
        return new PhidgetServoControllerMetaData();
    }

    public String getPort() {
    
        checkInit();
        
        return portName;
    }
    
    protected void checkInit() {
    
        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    }
    
    public void reset() throws IOException {
    
        checkInit();
        
        // In case the silent mode was set, we have to resend the positions
        
        sent = false;
        
        send();
    }
    
    public Servo getServo(String id) throws IOException {
    
        checkInit();
        
        try {
        
            int iID = Integer.parseInt(id);
            
            if ( iID < 0 || iID > 3 ) {
            
                throw new IllegalArgumentException("ID out of 0...3 range: '" + id + "'");
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
    protected Servo createServo(int id) throws IOException {
    
        // VT: NOTE: There is no sanity checking, I expect the author of the
        // calling code to be sane - this is a protected method
    
        return new PhidgetServo(this, id);
    }

    /**
     * @exception IllegalStateException if the controller wasn't previously
     * initialized.
     */
    public Iterator getServos() throws IOException {
    
        checkInit();
    
        LinkedList servos = new LinkedList();
        
        for ( int idx = 0; idx < 4; idx++ ) {
        
            servos.add(getServo(Integer.toString(idx)));
        }
        
        return servos.iterator();
    }
    
    private Device findUSB(String portName) throws IOException {

        // VT: There are multiple ways to handle this:

        // 	- Analyze the portName and figure out which device we're
        // 	  talking about (in case there is more than one PhidgetServo
        // 	  on the box). This case allows to continue the operations
        // 	  until the real need to talk to the device arises.

        //	- Just browse the map until we find all of them and explode
        //	  if there's more than one or none.
        
        try {
        
            // VT: FIXME: What if there's more than one host? Bummer...
            
            Host usbHost = HostFactory.getHost();
            
            // VT: FIXME: This has to be handled inside getHost(), but since the
            // jUSB code sucks, it might have not been
            
            if ( usbHost == null ) {
            
                throw new IOException("No USB hosts found");
            }
            
            Bus busSet[] = usbHost.getBusses();
            
            Set found = new HashSet();
            
            for ( int idx = 0; idx < busSet.length; idx++ ) {
            
                Bus bus = busSet[idx];
                Device rootHub = bus.getRootHub();
                
                // Let's start walking down
                
                find(portName, rootHub, found);
            }
            
            if ( found.size() == 1 ) {
            
                // If there's just one object in the set, that's what we wanted in
                // any case

                return (Device)(found.toArray()[0]);
            }
                
            // Now, let's figure out what the caller wanted.
            
            if ( portName == null ) {
            
                // They just wanted a single device, but didn't know the serial
                // of it
            
                if ( found.isEmpty() ) {
                
                    throw new IOException("No PhidgetServo devices found");

                } else {
                
                    tooManyDevices(found);
                }
            
            } else {
            
                // The caller had specified the serial number
                
                if ( found.isEmpty() ) {
                
                    throw new IOException("PhidgetServo with a serial number '" + portName + "' is not connected");

                } else {
                
                    tooManyDevices(found);
                }
            }
            
            throw new IOException("No PhidgetServo devices found");
        
        } catch ( UnsatisfiedLinkError ule ) {
        
            System.err.println("\nMake sure you have the directory containing libjusb.so to your LD_LIBRARY_PATH\n");
            
            throw ule;
        }
    }
    
    /**
     * Unconditionally throw the <code>IOException</code>.
     *
     * @exception IOException with the list of device serial numbers found.
     */
    private void tooManyDevices(Set found) throws IOException {
    
        // VT: FIXME: Chester & Kevin, verify this
        
        String message = "No port name specified, multiple PhidgetServo devices found:";
        
        for ( Iterator i = found.iterator(); i.hasNext(); ) {
        
            Device next = (Device)i.next();
            DeviceDescriptor dd = next.getDeviceDescriptor();
            int languageSet[] = ControlMessage.getLanguages(next);
            int defaultLanguage = 0;
            
            if ( languageSet != null && languageSet.length != 0 ) {
            
                defaultLanguage = languageSet[0];
            }
            
            String serial = dd.getSerial(defaultLanguage);
            message += " " + serial;
        }
        
        throw new IOException(message);
    }
    
    /**
     * Find the PhidgetServo.
     *
     * @param portName Port name.
     *
     * @param root Device to start walking down from. Quite possibly this
     * may be the device being eventually added to the set. Can be <code>null</code>.
     *
     * @param found Set to put the found devices into.
     */
    private void find(String portName, Device root, Set found) throws IOException {
    
        if ( root == null ) {
        
            return;
        }
        
        DeviceDescriptor dd = root.getDeviceDescriptor();
        
        if ( dd.getDeviceClass() == Descriptor.CLASS_HUB ) {
        
            for ( int port = 0; port < root.getNumPorts(); port++ ) {
            
                Device child = root.getChild(port + 1);
                find(portName, child, found);
            }
            
        } else {
        
            // This may be our chance
            
            int languageSet[] = ControlMessage.getLanguages(root);
            int defaultLanguage = 0;
            
            if ( languageSet != null && languageSet.length != 0 ) {
            
                defaultLanguage = languageSet[0];
            }
            
            String product = dd.getProduct(defaultLanguage);
            
            if ( "PhidgetServo".equals(product) ) {
            
                // Yes!
                
                String serial = dd.getSerial(defaultLanguage);
                
                System.err.println("Serial found: " + serial);
                
                if ( portName == null ) {
                
                    found.add(root);
                    
                    return;
                }
                
                // Bold assumption: serial is not null
                
                if ( serial.equals(portName) ) {
                
                    found.add(root);
                    return;
                }
            }
        }
    }
    
    /**
     * Remember the servo timing and clear the "sent" flag.
     *
     * @param id Servo number
     *
     * @param position The servo position, in microseconds.
     *
     * @see #sent
     * @see #send
     */
    private synchronized void bufferPosition(int id, int position) {
    
        servoPosition[id] = position;
        sent = false;
    }
    
    /**
     * Compose the USB packet and stuff it down the USB controller.
     *
     * @exception IOException if there was an I/O error talking to the
     * controller.
     *
     * @see #servoPosition
     * @see #sent
     * @see #bufferPosition
     */
    private synchronized void send() throws IOException {
    
        if ( sent ) {
        
            // They have already sent the positions, relax

            return;
        }
        
        try {
        
            if ( thePhidgetServo == null ) {
            
                thePhidgetServo = findUSB(portName);
                System.err.println("Found " + portName);
                connected = true;
                
                // FIXME: notify the listeners about the arrival
            }
            
            // If we've found the phidget, we can get to composing the
            // buffer, otherwise it would've been waste of time
            
            byte buffer[] = protocolHandler.composeBuffer();
            send(buffer);
            
            // If there was an IOException sending the message, the flag is not
            // cleared. This is OK, since if it was a temporary condition,
            // whoever is about to call send() now will have a shot at properly
            // sending the data.
            
            sent = true;
            
        } catch ( IOException ioex ) {
        
            exception(ioex);
        
            // FIXME: notify the listeners about the departure
                
            connected = false;
            
            if ( isDisconnectAllowed() ) {
            
                // We're fine, but will have to keep looking for the phidget
                
                thePhidgetServo = null;
                
                return;
            }
            
            // Too bad
            
            throw ioex;

        } finally {
        
            touch();
        }
    }
    
    private synchronized void send(byte buffer[]) throws IOException {
    
        ControlMessage message = new ControlMessage();

        message.setRequestType((byte)(ControlMessage.DIR_TO_DEVICE
                                     |ControlMessage.TYPE_CLASS
                                     |ControlMessage.RECIPIENT_INTERFACE));
        message.setRequest((byte)ControlMessage.SET_CONFIGURATION);
        message.setValue((short)0x0200);
        message.setIndex((byte)0);
        message.setLength(buffer.length);
        message.setBuffer(buffer);
        
        // The instance has to be non-null at this point, or the
        // IOException was already thrown
        
        thePhidgetServo.control(message);
    }
    
    protected class PhidgetServoControllerMetaData implements ServoControllerMetaData {
    
        public String getManufacturerURL() {
        
            return "http://www.cpsc.ucalgary.ca/grouplab/phidgets/phidget-servo/phidget-servo.html";
        }
        
        public String getManufacturerName() {
        
            // FIXME: Get the manufacturer from the USB device?
            
            return "GLAB Chester";
        }
        
        public String getModelName() {
        
            return "PhidgetServo";
        }
        
        public int getMaxServos() {
        
            return 4;
        }
        
        public boolean supportsSilentMode() {
        
            return true;
        }
        
        public int getPrecision() {
        
            // FIXME: This has to be determined. Since the controller
            // accepts the raw values in microseconds, and the usable range
            // is at least 1000us to 2000us and quite possibly beyond, it is
            // AT LEAST this much.
            
            return 1000;
        }
        
        public int getBandwidth() {
        
            // FIXME
            
            return (2400 / 8) / 2;
        }
    }
    
    public class PhidgetServo extends AbstractServo {
    
        private int id;
        private int min_pulse = 1000;
        private int max_pulse = 2000;
        
        protected PhidgetServo(ServoController sc, int id) throws IOException {
        
            super(sc, null);
            
            this.id = id;
        }
        
        public ServoMetaData[] getMetaData() {
        
            // FIXME
            throw new UnsupportedOperationException();
        }

        public String getName() {
        
            return Integer.toString(id);
        }
        
        protected void setActualPosition(double position) throws IOException {
        
            checkInit();
            checkPosition(position);
            
            // Tough stuff, we're dealing with timing now...
            
            int microseconds = (int)(min_pulse + (position * (max_pulse - min_pulse)));
            
            // VT: NOTE: We need to know all the servo's positions because
            // they get transmitted in one packet
            
            bufferPosition(id, microseconds);
            
            if ( false ) {
            
                System.err.println("Position:     " + position);
                System.err.println("Microseconds: " + microseconds);
                System.err.println("Buffer:       " + servoPosition[id]);
                System.err.println("");
            }
            
            send();
            
            this.actualPosition = position;
            actualPositionChanged();
        }
        
        public void setRange(int range) {
        
            throw new UnsupportedOperationException();
        }
        
        public void setRange(int min_pulse, int max_pulse) throws IOException {
        
            if ( min_pulse >= max_pulse ) {
            
                throw new IllegalArgumentException("Inverted pulse length values: min >= max ("
                	+ min_pulse
                	+ " >= "
                	+ max_pulse
                	+ ")");
            }
            
            if ( min_pulse < 150 ) {
            
                throw new IllegalArgumentException("Unreasonably short min_pulse: " + min_pulse + ", 150 recommended");
            }

            if ( max_pulse > 2500 ) {
            
                throw new IllegalArgumentException("Unreasonably long max_pulse: " + max_pulse + ", 2500 recommended");
            }
            
            this.min_pulse = min_pulse;
            this.max_pulse = max_pulse;
            
            setActualPosition(actualPosition);
        }
    }

    protected SilentProxy createSilentProxy() {
    
        return new PhidgetSilentProxy();
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
    
    protected class PhidgetSilentProxy implements SilentProxy {
    
        public synchronized void sleep() {
        
            try {
            
                send(new byte[6]);
                _silentStatusChanged(false);
                
            } catch ( IOException ioex ) {
            
                _exception(ioex);
            }
        }
        
        public synchronized void wakeUp() {
        
            // VT: FIXME: Do I really have to do anything? The packet with the proper data gets sent anyway...
            
            try {
            
                reset();
                _silentStatusChanged(true);
                
            } catch ( IOException ioex ) {
            
                _exception(ioex);
            }
        }
    }
    
    /**
     * An abstraction for the object handling the communications with the
     * arbitrary hardware revision of the PhidgetServo controller.
     *
     * <p>
     *
     * For every hardware revision, there will be a separate protocol
     * handler.
     */
    protected abstract class ProtocolHandler {
    
        /**
         * Compose the USB packet.
         *
         * @return A buffer that represents the positioning command for the
         * hardware.
         */
        abstract public byte[] composeBuffer();
    }
    
    /**
     * Protocol handler for PhidgetServo revision 0.03.
     */
    protected class ProtocolHandler003 extends ProtocolHandler {
    
        byte buffer[] = new byte[6];

        /**
         * Compose the USB packet.
         *
         * @return A buffer that represents the positioning command for the
         * hardware.
         */
        public byte[] composeBuffer() {
        
            // VT: FIXME: Who knows how does the compiler implement this stuff,
            // it might be worth it to replace the division and multiplication
            // with the right and left shift.
            
            buffer[0]  = (byte)(servoPosition[0] % 256);
            buffer[1]  = (byte)(servoPosition[0] / 256);
            
            buffer[2]  = (byte)(servoPosition[1] % 256);
            buffer[1] |= (byte)((servoPosition[1] / 256) * 16);
            
            buffer[3]  = (byte)(servoPosition[2] % 256);
            buffer[4]  = (byte)(servoPosition[2] / 256);
            
            buffer[5]  = (byte)(servoPosition[3] % 256);
            buffer[4] |= (byte)((servoPosition[3] / 256) * 16);
            
            return buffer;
        }
    }
}
