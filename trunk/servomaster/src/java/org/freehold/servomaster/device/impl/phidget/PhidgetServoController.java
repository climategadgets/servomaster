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
import usb.core.USBException;

import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.ServoController;
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
 * @version $Id: PhidgetServoController.java,v 1.21 2003-07-03 18:07:59 vtt Exp $
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
    protected Map protocolHandlerMap = new HashMap();
     
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
    
        fillProtocolHandlerMap();
    }
    
    /**
     * Fill the protocol handler map.
     *
     * This class puts all the protocol handlers known to it into the {@link
     * #protocolHandlerMap protocol handler map}, so the devices can be
     * recognized. However, this presents some difficulties in relation to
     * disconnected mode if a particular, known beforehand, type of device
     * with a known identifier must be operated, but it is not be present at
     * the driver startup time. If this is the case, then subclasses of this
     * class must be used that fill the protocol handler map
     * with the only protocol handler.
     *
     * @see #isOnly
     */
    protected void fillProtocolHandlerMap() {
    
        protocolHandlerMap.put("38", new ProtocolHandler0x38());
        protocolHandlerMap.put("39", new ProtocolHandler0x39());
        protocolHandlerMap.put("3b", new ProtocolHandler0x3B());
    }
    
    /**
     * Is this class handling only one type of Phidget?
     *
     * @return true if only one type of Phidget is supported.
     */
    protected boolean isOnly() {
    
        // VT: FIXME: This may be a problem for AdvancedServo, since we have
        // to identify two product ids - check the code
    
        return protocolHandlerMap.size() == 1;
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

                Configuration cf = thePhidgetServo.getConfiguration();
                Interface iface = cf.getInterface(0, 0);
                
                if ( !iface.claim() ) {
                
                    throw new IOException("Can't claim interface - already claimed by " + iface.getClaimer());
                }
                
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
    protected  void doInit(String portName) throws IOException {
    
        try {
        
            thePhidgetServo = findUSB(portName);
        
            Configuration cf = thePhidgetServo.getConfiguration();
            Interface iface = cf.getInterface(0, 0);
            
            if ( !iface.claim() ) {
            
                throw new IOException("Can't claim interface - already claimed by " + iface.getClaimer());
            }

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
            
            // VT: NOTE: Serial number can be null. At least it
            // is with the current firmware release for
            // AdvancedServo. The implication is that there can
            // be just one instance of AdvancedServo on the
            // system.
            
            if ( serial == null ) {
            
                // FIXME: this is a hack, but unavoidable one
                
                serial = "null";
            }
            
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
    
    public synchronized Meta getMeta() {
    
        checkInit();
    
        if ( protocolHandler == null ) {
        
            throw new IllegalStateException("Hardware not yet connected, try later");
        }
        
        return protocolHandler.getMeta();
    }

    public String getPort() {
    
        checkInit();
        
        // VT: FIXME: When TUSB hack will be implemented, portName will have
        // no right to be null at this point
        
        return portName;
    }
    
    protected void checkInit() {
    
        // VT: NOTE: Checking for initialization is quite complicated in
        // this case. Here are following scenarios:
        //
        // 1. Port name was given, device was not present at instantiation
        // time.
        //
        // 2. Port name was not given, device was present and the only at
        // instantiation time.
        //
        // In both cases, the portName is known either way. We don't handle
        // the case when the portName is not known, and there were no
        // devices detected at instantiation time.
        //
        // Now, the complications.
        //
        // This class, so far the only one, supports multiple hardware
        // products with different capabilities. The only known feature of
        // them is that they're all Phidgets. Therefore, the capabilities of
        // the particular controller will not be known until the device is
        // actually connected and recognized. Consequently, the protocol
        // handler (and hence the metadata) will be created at that time,
        // and the metadata will be available from that point on.
        //
        // Additional complication is that there is a possible case of a
        // serial number clash between different kinds of Phidgets, but
        // let's discount this case for a while - it seems unlikely. In
        // other words, as soon as the Phidget is recognized for the first
        // time, the protocol handler will be created, portName remembered,
        // and the capabilities will be known from that point on.
        
        // VT: FIXME: PhidgetAdvancedServo doesn't return serial number -
        // TUSB hack pending
        
        if ( protocolHandler == null && portName == null ) {
        
            // VT: FIXME: It might be a good idea to try to reinit the
            // controller if portName is not null
        
            throw new IllegalStateException("Not initialized");
        }
    }
    
    public synchronized void reset() throws IOException {
    
        checkInit();
        
        if ( protocolHandler != null ) {
        
            protocolHandler.reset();
        }
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
    
        return protocolHandler.createServo(this, id);
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
                    
                    // The USB device handle for the device which used to be
                    // a SoftPhidget is now stale, so we have to reset the
                    // enumeration to the root device. Since we're adding
                    // the devices found to the *set*, there's nothing to
                    // worry about - we will just lose some time, but there
                    // will be no duplicates.
                    
                    // And since we're doing the job all over again starting
                    // from this method's entry point, we'll just return
                    // afterwards.
                    
                    try {
                    
                        find(portName, root, found, false);
                        return;

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
                        // AdvancedServo. The implication is that there can
                        // be just one instance of AdvancedServo on the
                        // system.
                        
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
            
            System.err.print("Firmware size " + buffer.length + ", header");
            
            for ( int offset = 0; offset < 4; offset++ ) {
            
                System.err.print(" 0x");
                
                String hex = Integer.toHexString(buffer[offset]&0xFF);
                
                if ( hex.length() == 1 ) {
                
                    hex = "0" + hex;
                }
                
                System.err.print(hex.toUpperCase());
            }
            
            System.err.println("");

            out.write(buffer);
            out.flush();
            
        } catch ( USBException  usbex ) {
        
            // Analyze the exception. It's possible that the device
            // announced itself removed by now and we're getting an
            // exception because of that
            
            // VT: FIXME: This depends heavily on jUSB code, hope it can be
            // made more clear later
            
            String message = usbex.getMessage();
            
            if ( message != null ) {
            
                if ( message.equals("writeBulk -- USB device has been removed -- No such device [19]") ) {
                
                    // Yes, this is a classical symptom
                    
                    System.err.println("Boot may have failed: device prematurely departed; ignored");
                    usbex.printStackTrace();
                }
            }
        
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
            
            Thread.sleep(5000);
            
        } catch ( InterruptedException iex ) {
        
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
         * Controller metadata.
         */
        private final Meta meta;
        
        ProtocolHandler() {
        
            meta = createMeta();
        }
        
        abstract protected Meta createMeta();
        
        Meta getMeta() {
        
            return meta;
        }
        
        /**
         * Get the device model name.
         *
         * This method is here because the protocol handlers are create
         * before the actual devices are found. Ideally, the model name
         * should be retrieved from the USB device (and possibly, it will be
         * done so later), but so far this will do.
         */
        abstract protected String getModelName();
        
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
         * <p>
         *
         * <strong>NOTE:</strong> originally this method was named
         * <code>setActualPosition()</code>. This worked all right with JDK
         * 1.4.1, however, later it turned out that JDK 1.3.1 was not able
         * to properly resolve the names and thought that this method
         * belongs to <code>PhidgetServo</code>, though the signature was
         * different. The name was changed to satisfy JDK 1.3.1, but this
         * points out JDK 1.3.1's deficiency in handling the inner classes. 
         * Caveat emptor. You better upgrade.
         *
         * @param id Servo number.
         *
         * @param position Desired position.
         */
        abstract public void setPosition(int id, double position) throws IOException;
        
        /**
         * Silence the controller.
         *
         * VT: FIXME: This better be deprecated - each servo can be silenced
         * on its own
         */
        abstract public void silence() throws IOException;
        
        abstract public Servo createServo(ServoController sc, int id) throws IOException;
        
        /**
         * Base class representing all the common (or default) features and
         * properties of the Phidgets family of servo controllers.
         */
        protected class PhidgetMeta extends AbstractMeta {
        
            public PhidgetMeta() {
            
                properties.put("manufacturer/name", "Phidgets, Inc.");
                properties.put("manufacturer/URL", "http://www.phidgets.com/");
                properties.put("manufacturer/model", getModelName());
                properties.put("controller/maxservos", Integer.toString(getServoCount()));

                features.put("controller/allow_disconnect", new Boolean(true));
            }
        }

        abstract public class PhidgetServo extends AbstractServo {
        
            /**
             * Servo number.
             */
            protected int id;
            
            /**
             * Servo metadata.
             */
            private Meta meta;
            
            protected PhidgetServo(ServoController sc, int id) throws IOException {
            
                super(sc, null);
                
                this.id = id;
                this.meta = createServoMeta();
            }
            
            public Meta getMeta() {
            
                return meta;
            }
            
            /**
             * Template method to create the instance of the metadata for
             * the servo.
             *
             * @return Class specific metadata instance.
             */
            abstract protected Meta createServoMeta();

            public String getName() {
            
                return Integer.toString(id);
            }
            
            protected void setActualPosition(double position) throws IOException {
            
                checkInit();
                checkPosition(position);
                
                try {
                
                    protocolHandler.setPosition(id, position);
                    
                    this.actualPosition = position;
                    actualPositionChanged();
                    
                } catch ( USBException usbex ) {
                
                    connected = false;
                
                    if ( !isDisconnectAllowed() ) {
                    
                        // Too bad
                        
                        throw (IOException)usbex;
                    }
                
                    // VT: NOTE: This block is dependent on jUSB error message
                    // text
                    
                    String xmessage = usbex.getMessage();
                    
                    if ( xmessage == null ) {
                    
                        // Can't determine what kind of problem it is
                        
                        throw (IOException)usbex;
                    }
                    
                    if (    xmessage.indexOf("Bad file descriptor") != -1
                         || xmessage.indexOf("USB device has been removed") != -1 ) {
                    
                        // This probably means that the controller was
                        // disconnected
                        
                        System.err.println("Assumed disconnect, reason: " + xmessage);
                        
                        thePhidgetServo = null;
                    }
                }
            }
            
            protected class PhidgetServoMeta extends AbstractMeta {
            
                public PhidgetServoMeta() {
                
                    // VT: FIXME
                    
                    properties.put("servo/precision", "1500");
                }
            }
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
         * #send invocation context} makes sure it never gets corrupted.
         */
        protected byte buffer[] = new byte[6];
        
        ProtocolHandler003() {
        
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
        
        public void setPosition(int id, double position) throws IOException {
        
            // Tough stuff, we're dealing with timing now...
            
            PhidgetServo003 servo = (PhidgetServo003)servoSet[id];
            
            int microseconds = (int)(servo.min_pulse + (position * (servo.max_pulse - servo.min_pulse)));
            
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

                    Configuration cf = thePhidgetServo.getConfiguration();
                    Interface iface = cf.getInterface(0, 0);
                    
                    if ( !iface.claim() ) {
                    
                        throw new IOException("Can't claim interface - already claimed by " + iface.getClaimer());
                    }

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
            
                if ( connected ) {
                
                    exception(ioex);
            
                    // FIXME: notify the listeners about the departure
                    
                    connected = false;
                }
                
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

        protected Meta createMeta() {
        
            return new PhidgetMeta003();
        }
        
        public Servo createServo(ServoController sc, int id) throws IOException {
        
            return new PhidgetServo003(sc, id);
        }
        
        protected class PhidgetMeta003 extends PhidgetMeta {
        
            PhidgetMeta003() {
            
                features.put("controller/silent", new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));
                
                // VT: FIXME
                
                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
                properties.put("controller/precision", "1500");
                
                // Silent timeout is five seconds

                properties.put("controller/silent", "5000");

                // Milliseconds are default servo range units for v3
                // protocol
                
                properties.put("servo/range/units", "\u03BCs");
                
                // Default range is 500us to 2000us
                
                properties.put("servo/range/min", "500");
                properties.put("servo/range/max", "2000");
            }
        }
        
        public class PhidgetServo003 extends PhidgetServo {
        
            int min_pulse = 500;
            int max_pulse = 2000;
        
            public PhidgetServo003(ServoController sc, int id) throws IOException {
            
                super(sc, id);
            }
            
            protected Meta createServoMeta() {
            
                return new PhidgetServoMeta003();
            }
        
            protected class PhidgetServoMeta003 extends PhidgetServoMeta {
            
                PhidgetServoMeta003() {
                
                    PropertyWriter pwMin = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            min_pulse = Integer.parseInt(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };
                    
                    PropertyWriter pwMax = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            max_pulse = Integer.parseInt(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/precision", Integer.toString(max_pulse - min_pulse));
                        }
                    };
                    
                    propertyWriters.put("servo/range/min", pwMin);
                    propertyWriters.put("servo/range/max", pwMax);
                }
            }
        }
    }
    
    /**
     * Protocol handler for QuadServo.
     */
    protected class ProtocolHandler0x38 extends ProtocolHandler003 {
    
        protected String getModelName() {
        
            return "PhidgetServo";
        }
        
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
    
        protected String getModelName() {
        
            return "UniServo";
        }
        
        public int getServoCount() {
        
            return 1;
        }
    }

    /**
     * Protocol handler for AdvancedServo.
     */
    protected class ProtocolHandler0x3B extends ProtocolHandler {
    
        private OutputStream out;
        
        ProtocolHandler0x3B() {
        
        }
    
        protected String getModelName() {
        
            return "PhidgetAdvancedServo";
        }
        
        public void reset() {
        
            // FIXME
        }
    
        public int getServoCount() {
        
            return 8;
        }
        
        public synchronized void setPosition(int id, double position) throws IOException {
        
            if ( servoSet[id] == null ) {
            
                throw new IllegalStateException("servoSet[" + id + "] is still null");
            }
            
            PhidgetServo0x3B servo = (PhidgetServo0x3B)servoSet[id];
        
            send(servo.renderPosition(position));
        }
        
        public void silence() throws IOException {
        
            // VT: FIXME
            
            System.err.println("silence() is not implemented in " + getClass().getName());
        }
        
        private void init() throws IOException {
        
            if ( out == null ) {
            
                Configuration cf = thePhidgetServo.getConfiguration();
                Interface iface = cf.getInterface(0, 0);
                
                if ( false ) {
                
                // VT: FIXME: Verify: with the latest changes, we should've
                // claimed it already
                
                if ( !iface.claim() ) {
                
                    throw new IOException("Can't claim interface - already claimed by " + iface.getClaimer());
                }
                
                }
                
                Endpoint endpoint = null;
                
                for ( int idx = 0; idx < iface.getNumEndpoints(); idx++ ) {
                
                    Endpoint e = iface.getEndpoint(idx);
                    
                    System.err.println("Endpoint: " + e.getEndpoint() + ":" + Integer.toHexString(e.getEndpoint()));
                    
                    if ( e.getEndpoint() == 0x01 ) {
                    
                        endpoint = e;
                    }
                }
                
                if ( endpoint == null ) {
                
                    throw new IOException("Can't find endpoint 82");
                }
                
                out = endpoint.getOutputStream();
            }
        }
        
        protected synchronized void send(byte buffer[]) throws IOException {
        
            init();
        
            out.write(buffer);
            out.flush();
        }
        
        public Servo createServo(ServoController sc, int id) throws IOException {
        
            return new PhidgetServo0x3B(sc, id);
        }

        public class PhidgetServo0x3B extends PhidgetServo {
        
            /**
             * Servo position, degrees.
             */
            float position;
            
            /**
             * Minimum position offset, degrees.
             *
             * Default is 0.
             */
            int min_offset = 0;
            
            /**
             * Maximum position offset, degrees.
             *
             * Default is 180.
             */
            int max_offset = 180;
            
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
             * #send invocation context} makes sure it never gets corrupted.
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
             *
             * or
             *
             * buffer[0] = Index - 1;
             *
             * if (m_blnAssert[Index - 1] == VARIANT_TRUE) buffer[1] = 0xff;
             * ((int *)(buffer+4))[0] = (int)((m_ServoPosition[Index - 1] + 23) * 16218);
             * ((int *)(buffer+4))[1] = (int)((m_MaxVelocity[Index - 1] / 50) * 16218);
             * ((int *)(buffer+4))[2] = (int)((m_Acceleration[Index - 1] / 50) * 16218);
             * </pre>
             *
             * MaxVelocity and Acceleration are measured in degrees/second. (^2)
             */
            private byte[] buffer = new byte[16];
            
            public byte[] renderPosition(double position) {
            
                // The requested position is 0 to 1, but the device position
                // is 0 to 180 - have to translate
                
                // VT: FIXME: adjustment for terminal positions may be
                // required
                
                this.position = (float)min_offset + (float)(position * (max_offset - min_offset));
                
                // 16218
                
                float2byte((this.position + 23) * 8109, buffer, 4);
                float2byte((this.velocity / 50) * 8109, buffer, 8);
                float2byte((this.acceleration / 50) * 8109, buffer, 12);
                
                //System.err.println("Position: " + this.position);
                //System.err.print("Buffer:");
                
                /*
                for ( int idx = 0; idx < buffer.length; idx++ ) {
                
                    if ( (idx % 4) == 0 && idx > 0 ) {
                    
                        System.err.print(" -");
                    }
                    System.err.print(" " + Integer.toHexString(buffer[idx] & 0xFF));
                    
                }
                
                System.err.println("");
                
                 */
                
                return buffer;
            }
            
            private void float2byte(float value, byte buffer[], int offset) {
            
                //int bits = Float.floatToIntBits(value);
                int bits = (int)value;
                
                buffer[offset + 0] = (byte)(bits & 0xFF);
                buffer[offset + 1] = (byte)((bits >> 8) & 0xFF);
                buffer[offset + 2] = (byte)((bits >> 16) & 0xFF);
                buffer[offset + 3] = (byte)((bits >> 24) & 0xFF);
            }

            public PhidgetServo0x3B(ServoController sc, int id) throws IOException {
            
                super(sc, id);

                // We will never touch this again
                
                buffer[0] = (byte)id;
                
                // VT: FIXME: This will have to change to support the
                // 'silenced' mode. According to the specs, bit 1 should be
                // set to 1, otherwise the servos will not get the pulse and
                // will go to standby mode. In reality, it has to be 0xFF.
                
                buffer[1] = (byte)0xFF;
                
                // Initial values
                
                velocity = 360;
                acceleration = 360;
            }
            
            protected Meta createServoMeta() {
            
                return new PhidgetServoMeta0x3B();
            }
        
            protected class PhidgetServoMeta0x3B extends PhidgetServoMeta {
            
                PhidgetServoMeta0x3B() {
                
                    PropertyWriter pwMin = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            min_offset = Integer.parseInt(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/precision", Integer.toString(max_offset - min_offset));
                        }
                    };
                    
                    PropertyWriter pwMax = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            max_offset = Integer.parseInt(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/precision", Integer.toString(max_offset - min_offset));
                        }
                    };
                    
                    PropertyWriter pwVelocity = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            velocity = Float.parseFloat(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/velocity", Float.toString(velocity));
                        }
                    };
                    
                    PropertyWriter pwAcceleration = new PropertyWriter() {
                    
                        public void set(String key, Object value) {
                        
                            acceleration = Float.parseFloat(value.toString());
                            
                            try {
                            
                                setActualPosition(actualPosition);

                            } catch ( IOException ioex ) {
                            
                                ioex.printStackTrace();
                            }
                            
                            properties.put("servo/acceleration", Float.toString(acceleration));
                        }
                    };
                    
                    propertyWriters.put("servo/range/min", pwMin);
                    propertyWriters.put("servo/range/max", pwMax);
                    propertyWriters.put("servo/velocity", pwVelocity);
                    propertyWriters.put("servo/acceleration", pwAcceleration);

                    // Default velocity is 360 degrees/sec, default acceleration
                    // is 360 dev/sec^2. Maybe it is too much, but I don't care
                    // to find out at this time.
                    
                    properties.put("servo/velocity", "360");
                    properties.put("servo/acceleration", "360");
                }
            }
        }

        protected Meta createMeta() {
        
            return new PhidgetMeta0x3B();
        }
        
        protected class PhidgetMeta0x3B extends PhidgetMeta {
        
            PhidgetMeta0x3B() {
            
                features.put("controller/silent", new Boolean(true));
                features.put("controller/protocol/USB", new Boolean(true));
                
                // NOTE: This controller does indeed have the 'serial' feature,
                // but it is permanently disabled
                
                features.put("controller/protocol/serial", new Boolean(false));
                
                // Silent timeout is five seconds

                properties.put("controller/silent", "5000");

                // VT: FIXME
                
                properties.put("controller/bandwidth", Integer.toString((2400 / 8) / 2));
                properties.put("controller/precision", "1500");

                // Degrees are default servo range units for 0x3B protocol
                
                properties.put("servo/range/units", "\u00B0");
                
                // Default range is 0 to 180 degrees
                
                properties.put("servo/range/min", "0");
                properties.put("servo/range/max", "180");
                
                // Default velocity is 360 degrees/sec, default acceleration
                // is 360 dev/sec^2. Maybe it is too much, but I don't care
                // to find out at this time.
                
                properties.put("servo/velocity", "360");
                properties.put("servo/acceleration", "360");
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
