package org.freehold.servomaster.device.impl.phidget;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import usb.core.Bus;
import usb.core.Configuration;
import usb.core.ControlMessage;
import usb.core.Descriptor;
import usb.core.Device;
import usb.core.DeviceDescriptor;
import usb.core.Endpoint;
import usb.core.Host;
import usb.core.HostFactory;
import usb.core.Interface;

import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoMetaData;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerMetaData;
import org.freehold.servomaster.device.model.silencer.SilentProxy;

import org.freehold.servomaster.device.impl.phidget.Firmware;
import org.freehold.servomaster.device.impl.phidget.firmware.Servo8;

/**
 * <a
 * href="http://www.cpsc.ucalgary.ca/grouplab/phidgets/phidget-servo/phidget-servo.html"
 * target="_top">PhidgetServo</a> controller.
 *
 * Detailed documentation to follow.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: PhidgetServoController.java,v 1.10 2002-09-18 06:25:10 vtt Exp $
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
     */
    private Servo servoSet[];
    
    /**
     * Default constructor.
     *
     * Provided for <code>Class.newInstance()</code> to be happy.
     */
    public PhidgetServoController() {
    
        protocolHandlerMap.put("38", new ProtocolHandler0x38());
        protocolHandlerMap.put("39", new ProtocolHandler0x39());
        protocolHandlerMap.put("3b", new ProtocolHandler0x3B());
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
            String product = Integer.toHexString(dd.getProductId());
            
            protocolHandler = (ProtocolHandler)protocolHandlerMap.get(product);
            
            if ( protocolHandler == null ) {
            
                throw new UnsupportedOperationException("ProductID '" + product + "' is not supported");
            }
            
            servoSet = new Servo[protocolHandler.getServoCount()];
            
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
        
        protocolHandler.reset();
        
    }
    
    public Servo getServo(String id) throws IOException {
    
        checkInit();
        
        try {
        
            int iID = Integer.parseInt(id);
            
            if ( iID < 0 || iID > protocolHandler.getServoCount() ) {
            
                throw new IllegalArgumentException("ID out of 0..." + protocolHandler.getServoCount() + " range: '" + id + "'");
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
        
        for ( int idx = 0; idx < protocolHandler.getServoCount(); idx++ ) {
        
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
                
                try {
                
                    find(portName, rootHub, found, true);
                
                } catch ( BootException bex ) {
                
                    bex.printStackTrace();
                    
                    throw new IllegalStateException("BootException shouldn't have propagated here");
                }
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
     *
     * @param boot Whether to boot the phidget if one is found.
     */
    private void find(String portName, Device root, Set found, boolean boot) throws IOException, BootException {
    
        if ( root == null ) {
        
            return;
        }
        
        DeviceDescriptor dd = root.getDeviceDescriptor();
        
        if ( dd.getDeviceClass() == Descriptor.CLASS_HUB ) {
        
            for ( int port = 0; port < root.getNumPorts(); port++ ) {
            
                System.err.println("Hub " + dd.getSerial(0) + ": port " + (port + 1));
            
                Device child = root.getChild(port + 1);
                
                try {
                
                    find(portName, child, found, true);
                    
                } catch ( BootException bex ) {
                
                    // This means that SoftPhidget was found and booted, it
                    // should appear as a device with a different product ID
                    // by now. If it is still a SoftPhidget, we've failed.
                    
                    try {
                    
                        find(portName, child, found, false);

                    } catch ( BootException bex2 ) {
                    
                        System.err.println("Failed to boot SoftPhidget at hub " + dd.getSerial(0) + ": port " + (port + 1));
                    }
                }
            }
            
        } else {
        
            // This may be our chance
            
            // Check the vendor/product ID first in case the device
            // is not very smart and doesn't support control messages
            
            if ( dd.getVendorId() == 0x6c2 ) {
            
                System.err.println("Phidget: " + Integer.toHexString(dd.getProductId()));
            
                // 0x6c2 is a Phidget
                
                switch ( dd.getProductId() ) {
                
                    case 0x0038:
                    
                        // QuadServo
                        
                    case 0x0039:
                    
                        // UniServo
                        
                    case 0x003b:
                    
                        // AdvancedServo
                        
                        if ( portName == null ) {
                        
                            // We don't even have to see the serial number
                        
                            found.add(root);
                            
                            return;
                        }
                        
                        // Port name was specified, so we have to retrieve
                        // it
                        
                        int languageSet[] = ControlMessage.getLanguages(root);
                        int defaultLanguage = 0;
                        
                        if ( languageSet != null && languageSet.length != 0 ) {
                        
                            defaultLanguage = languageSet[0];
                        }
                        
                        String serial = dd.getSerial(defaultLanguage);
                        
                        System.err.println("Serial found: " + serial);
                        
                        // VT: NOTE: Serial number can be null. At least it
                        // is with the current firmware release for
                        // AdvancedServo
                        
                        if ( serial == null ) {
                        
                            // FIXME: this is a hack, but unavoidable one
                            
                            serial = "null";
                        }
                            
                        if ( serial.equals(portName) ) {
                            
                            found.add(root);
                            return;
                        }
                        
                        break;
                        
                    case 0x0060:
                    
                        // SoftPhidget
                        
                        System.err.println("SoftPhidget found");
                        
                        if ( !boot ) {
                        
                            // Oops...
                            
                            throw new BootException("Second time, refusing to boot");
                        }

                        boot(root);
                        
                        // The device enumeration path is broken now, since
                        // the product ID has changed after the boot.
                        // However, it is clear that the device is confined
                        // to the same hub, so we have to just restart the
                        // search on the same hub.
                        
                        throw new BootException("Need to re-read the device information");
                        
                    default:
                    
                        System.err.println("Phidget: unknown: " + Integer.toHexString(dd.getProductId()));
                }
            }
        }
    }
    
    /**
     * Boot the SoftPhidget.
     *
     * @param target Device to boot.
     */
    private void boot(Device target) {
    
        System.err.println("Booting " + target.getDeviceDescriptor().getSerial(0));
        
        try {
        
            Configuration cf = target.getConfiguration();
            Interface iface = cf.getInterface(0, 0);
            Endpoint endpoint = iface.getEndpoint(0);
            OutputStream out = endpoint.getOutputStream();
            Firmware fw = new Servo8();
            byte buffer[] = fw.get();
            
            out.write(buffer);
            
        } catch ( Throwable t ) {
        
            System.err.println("Boot failed:");
            t.printStackTrace();
            
            // Since there's nothing we can do about it, we'll just proceed
            // as usual. The device either will not be found at all, or will
            // be found as SoftPhidget again, which is taken care of.
        }
        
        try {
        
            // The SoftPhidget is supposed to boot in about 200ms, let's be
            // paranoid
            
            Thread.sleep(1000);
            
        } catch ( InterruptedException iex ) {
        
        }
    }
    
    protected class PhidgetServoControllerMetaData implements ServoControllerMetaData {
    
        public String getManufacturerURL() {
        
            return "http://www.cpsc.ucalgary.ca/grouplab/phidgets/phidget-servo/phidget-servo.html";
        }
        
        public String getManufacturerName() {
        
            // VT: FIXME: Get the manufacturer from the USB device?
            
            return "GLAB Chester";
        }
        
        public String getModelName() {
        
            // VT: FIXME: it may be UniServo, QuadServo, AdvancedServo
            
            return "PhidgetServo";
        }
        
        public int getMaxServos() {
        
            return protocolHandler.getServoCount();
        }
        
        public boolean supportsSilentMode() {
        
            // VT: FIXME: Verify if AdvancedServo supports this
            
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
            
            protocolHandler.setActualPosition(id, position);
            
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
            
            protocolHandler.setRange(id, min_pulse, max_pulse);
            
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
            
                protocolHandler.silence();
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
         * Reset the controller.
         */
        abstract public void reset() throws IOException;
    
        /**
         * @return the number of servos the controller supports.
         */
        abstract public int getServoCount();
        
        /**
         * Set the servo position.
         *
         * @param id Servo number.
         *
         * @param position Desired position.
         */
        abstract public void setActualPosition(int id, double position) throws IOException;
        
        /**
         * Silence the controller.
         *
         * VT: FIXME: This better be deprecated - each servo can be silenced
         * on its own
         */
        abstract public void silence() throws IOException;
        
        /**
         * Set the pulse range.
         */
        abstract public void setRange(int id, int min_pulse, int max_pulse);

        protected synchronized void send(byte buffer[]) throws IOException {
        
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
    }
    
    /**
     * Protocol handler for PhidgetServo 3.0 protocol.
     */
    abstract protected class ProtocolHandler003 extends ProtocolHandler {
    
        /**
         * Current servo position in device coordinates.
         */
        protected int servoPosition[] = new int[4];
        
        /**
         * Minimum pulse lengths for the servos.
         */
        protected int min_pulse[] = new int[4];
         
        /**
         * Maximum pulse lenghts for the servos.
         */
        protected int max_pulse[] = new int[4];
        
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
         * Byte buffer to compose the packet into.
         *
         * This buffer is not thread safe, but the {@link
         * PhidgetServoController#send invocation context} makes sure it
         * never gets corrupted.
         */
        protected byte buffer[] = new byte[6];
        
        ProtocolHandler003() {
        
            for ( int id = 0; id < 4; id++ ) {
            
                min_pulse[id] = 1000;
                max_pulse[id] = 2000;
            }
        }
        
        public void reset() throws IOException {
        
            // In case the silent mode was set, we have to resend the positions
            
            sent = false;
            
            send();
        }
        
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
        
        public void setActualPosition(int id, double position) throws IOException {
        
            // Tough stuff, we're dealing with timing now...
            
            int microseconds = (int)(min_pulse[id] + (position * (max_pulse[id] - min_pulse[id])));
            
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
                
                byte buffer[] = composeBuffer();
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
        
        public void silence() throws IOException {
        
            // Send the zero microseconds pulse
            
            send(new byte[6]);
        }
        
        public void setRange(int id, int min_pulse, int max_pulse) {
        
            this.min_pulse[id] = min_pulse;
            this.max_pulse[id] = max_pulse;
        }
    }
    
    /**
     * Protocol handler for QuadServo.
     */
    protected class ProtocolHandler0x38 extends ProtocolHandler003 {
    
        public int getServoCount() {
        
            return 4;
        }
    }
    
    /**
     * Protocol handler for UniServo.
     *
     * VT: FIXME: This has to be tested!
     */
    protected class ProtocolHandler0x39 extends ProtocolHandler003 {
    
        public int getServoCount() {
        
            return 1;
        }
    }

    /**
     * Protocol handler for AdvancedServo.
     */
    protected class ProtocolHandler0x3B extends ProtocolHandler {
    
        private ServoState servoState[] = new ServoState[8];
        
        ProtocolHandler0x3B() {
        
            for ( int idx = 0; idx < 8; idx++ ) {
            
                servoState[idx] = new ServoState(idx);
            }
        }
    
        public void reset() {
        
            // FIXME
        }
    
        public int getServoCount() {
        
            return 8;
        }
        
        public synchronized void setActualPosition(int id, double position) throws IOException {
        
            send(servoState[id].setPosition(position));
        }
        
        public void silence() throws IOException {
        
            // Not supported
        }
        
        public void setRange(int id, int min_pulse, int max_pulse) {
        
        
            // Not supported
        }
        
        protected class ServoState {
        
            /**
             * Servo position, degrees.
             */
            float position;
            
            /**
             * Servo velocity, degrees per second.
             */
            float velocity;
            
            /**
             * Servo acceleration, degrees per second per second.
             */
            float acceleration;
            
            /**
             * Byte buffer to compose the packet into.
             *
             * This buffer is not thread safe, but the {@link
             * PhidgetServoController#send invocation context} makes sure it
             * never gets corrupted.
             *
             * <p>
             *
             * Output protocol for PhidgetAdvancedServo
             *
             * <pre>
             * buffer[0] = Index - 1;
             *
             * ((float *)(buffer+4))[0] = m_ServoPosition[Index - 1];
             * ((float *)(buffer+4))[1] =  m_MaxVelocity[Index - 1] / 50;
             * ((float *)(buffer+4))[2] =  m_Acceleration[Index - 1] / 50;
             * </pre>
             *
             * MaxVelocity and Acceleration are measured in degrees/second. (^2)
             */
            private byte[] buffer = new byte[16];
            
            ServoState(int id) {
            
                // We will never touch this again
                
                // VT: FIXME: Is it a little endian or big endian?
                
                buffer[3] = (byte)id;
                
                // VT: FIXME: This stuff will be properly handled when the
                // metadata is ready
                
                velocity = 180;
                acceleration = 180;
            }
            
            public byte[] setPosition(double position) {
            
                // The requested position is 0 to 1, but the device position
                // is 0 to 180 - have to translate
                
                // VT: FIXME: adjustment for terminal positions may be
                // required
                
                this.position = (float)(position * 180);
                
                float2byte(this.position, buffer, 4);
                float2byte(this.velocity/50, buffer, 8);
                float2byte(this.acceleration/50, buffer, 8);
                
                return buffer;
            }
            
            private void float2byte(float value, byte buffer[], int offset) {
            
                // VT: FIXME: Is it a little endian or big endian?
                
                int bits = Float.floatToIntBits(value);
                
                // This is big endian
                
                buffer[offset + 0] = (byte)(bits & 0xFF);
                buffer[offset + 1] = (byte)((bits >> 8) & 0xFF);
                buffer[offset + 2] = (byte)((bits >> 16) & 0xFF);
                buffer[offset + 3] = (byte)((bits >> 24) & 0xFF);
            }
        }
    }
    
    /**
     * This exception gets thrown whenever there was a SoftPhidget that had
     * to be booted, therefore the normal device enumeration was broken.
     */
    protected class BootException extends Exception {
    
        BootException(String message) {
        
            super(message);
        }
    }
}
