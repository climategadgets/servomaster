package net.sf.servomaster.device.impl.usb;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;

import org.apache.log4j.NDC;

import net.sf.servomaster.device.impl.AbstractServoController;
import net.sf.servomaster.device.impl.HardwareServo;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * Base class for all USB servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public abstract class AbstractUsbServoController extends AbstractServoController implements UsbServicesListener {

  private UsbHub virtualRootHub;

    /**
     * The revision to protocol handler map.
     *
     * <p>
     *
     * The key is the string formed as "${vendor-id}:${product-id}" (with
     * IDs being lowercase hex representations, no leading "0x". A
     * convenient way to obtain a signature is to call {@link #getSignature
     * getSignature()}), the value is the protocol handler. This is a little
     * bit of overhead, but adds flexibility.
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
    private Map<String, UsbProtocolHandler> protocolHandlerMap = new HashMap<String, UsbProtocolHandler>();

    /**
     * The protocol handler taking care of this specific instance.
     *
     * @see #init
     */
    protected UsbProtocolHandler protocolHandler;

    /**
     * Used to prevent polling too fast.
     *
     * VT: FIXME: This may not be necessary if javax.usb properly supports
     * arrival and departure notifications.
     */
    private long lastDetect;

    /**
     * The USB device corresponding to the servo controller.
     */
    protected UsbDevice theServoController;

    protected AbstractUsbServoController() {

        fillProtocolHandlerMap();

        if ( isOnly() ) {

            // OK, we've been subclassed to support just one specific kind
            // of a device. Good.

            protocolHandler = (UsbProtocolHandler)protocolHandlerMap.values().toArray()[0];
            servoSet = new Servo[getServoCount()];
        }

        // VT: FIXME: This really belongs to init(), but at this point
        // things are not settled down yet

        try {

            UsbServices usbServices = UsbHostManager.getUsbServices();
            virtualRootHub = usbServices.getRootUsbHub();

            usbServices.addUsbServicesListener(this);

        } catch ( UsbException usbex ) {

            throw (IllegalStateException) new IllegalStateException("USB failure").initCause(usbex);
        }
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
    protected abstract void fillProtocolHandlerMap();

    protected final void registerHandler(String signature, UsbProtocolHandler handler) {

        protocolHandlerMap.put(signature, handler);
    }

    /**
     * Is this class handling only one type of a device?
     *
     * @return true if only one type of a USB controller is supported.
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
    @Override
    public final synchronized boolean isConnected() {

        checkInit();

        if ( theServoController != null ) {

            // We can be reasonably sure that we're connected

            return true;

        } else {

            // Let's find out

            try {

                theServoController = findUSB(portName);

                UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                UsbInterface iface = cf.getUsbInterface((byte)0x00);

                if ( iface.isClaimed() ) {

                    throw new IOException("Can't claim interface - already claimed. "
                    + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
                }

                iface.claim();

                return true;

            } catch ( Throwable t ) {

                // Uh oh, I don't think so

                exception(t);
                return false;
            }
        }
    }

    protected final UsbDevice findUSB(String portName) throws IOException {
        
        NDC.push("findUsb");
        
        logger.info("Looking for device '" + portName + "'");
        
        // VT: There are multiple ways to handle this:

        // 	- Analyze the portName and figure out which device we're
        // 	  talking about (in case there is more than one device on
        // 	  the box). This case allows to continue the operations
        // 	  until the real need to talk to the device arises.

        //	- Just browse the map until we find all of them and explode
        //	  if there's more than one or none.

        try {

            // VT: Make sure we don't loop endlessly. Since there are no
            // notifications, the only chance to discover the device is just
            // keep polling. This means that this method will be called
            // every time there's an attempt to use the device. Let's
            // restrict the poll rate...

            // VT: FIXME: Right now, the delay is hardcoded at 1 second -
            // make this configurable

            if (System.currentTimeMillis() - lastDetect < 1000) {

                throw new IOException("Polling too fast");
            }

            lastDetect = System.currentTimeMillis();

            Set<UsbDevice> found = new HashSet<UsbDevice>();

            try {

                find(portName, virtualRootHub, found, true);

            } catch ( BootException bex ) {

                throw new IllegalStateException("BootException shouldn't have propagated here", bex);
            }

            if ( found.size() == 1 ) {

                // If there's just one object in the set, that's what we wanted in
                // any case

                return (UsbDevice) found.toArray()[0];
            }

            // Now, let's figure out what the caller wanted.

            if ( portName == null ) {

                // They just wanted a single device, but didn't know the serial
                // of it

                if ( found.isEmpty() ) {

                    throw new IOException("No compatible devices found. Make sure you have /proc/bus/usb read/write permissions.");

                } else {

                    tooManyDevices(found);
                }

            } else {

                // The caller had specified the serial number

                if ( found.isEmpty() ) {

                    throw new IOException("Device with a serial number '" + portName + "' is not connected");

                } else {

                    tooManyDevices(found);
                }
            }

            throw new IOException("No device found with serial number " + portName);

        } catch ( UnsatisfiedLinkError ule ) {

            logger.debug("\nMake sure you have the directory containing libJavaxUsb.so in your LD_LIBRARY_PATH\n");

            throw ule;

        } catch ( UsbException usbex ) {

            throw (IOException) new IOException("USB failure").initCause(usbex);
        } finally {
            NDC.pop();
        }
    }

    /**
     * Find the controller.
     *
     * @param portName Port name.
     *
     * @param root Device to start walking down from. Quite possibly this
     * may be the device being eventually added to the set. Can be <code>null</code>.
     *
     * @param found Set to put the found devices into.
     *
     * @param boot Whether to boot the device if one is found.
     *
     * @throws IOException if there's a hardware error.
     * @throws UsbException if there's a USB protocol level error.
     * @throws BootException if one of the devices encountered in the enumeration had to be booted,
     * thus interrupting normal enumeration flow.
     */
    @SuppressWarnings("unchecked")
    private void find(String portName,
                      UsbDevice root,
                      Set<UsbDevice> found,
                      boolean boot) throws IOException, UsbException, BootException {
        
        NDC.push("find" + (boot ? "&boot" : "!boot"));
        
        try {

            if ( root == null ) {

                return;
            }

            logger.debug("Found " + getSignature(root.getUsbDeviceDescriptor()) + ": "
                    + root.getProductString()
                    + ", S/N " + root.getSerialNumberString()
                    + " by " +root.getManufacturerString());

            if ( root.isUsbHub() ) {
                
                List<UsbDevice> devices = ((UsbHub)root).getAttachedUsbDevices();

                for ( Iterator<UsbDevice> i = devices.iterator(); i.hasNext(); ) {

                    try {

                        find(portName, i.next(), found, true);

                    } catch (BootException bex) {

                        // This means that a bootable device was found and
                        // booted, it should appear as a device with a different
                        // product ID by now. If it is still a device with the
                        // same signature as before, we've failed.

                        // The USB device handle for the device which used to be
                        // a bootable device is now stale, so we have to reset
                        // the enumeration to the root device. Since we're
                        // adding the devices found to the *set*, there's
                        // nothing to worry about - we will just lose some time,
                        // but there will be no duplicates.

                        // And since we're doing the job all over again starting
                        // from this method's entry point, we'll just return
                        // afterwards.

                        try {

                            find(portName, root, found, false);
                            return;

                        } catch (BootException bex2) {

                            logger.error("Failed to boot a bootable device", bex2);
                        }
                    }
                }

            } else {

                // This may be our chance

                UsbDeviceDescriptor dd = root.getUsbDeviceDescriptor();

                String signature = getSignature(dd);

                // See if we have a protocol handler for this device

                UsbProtocolHandler handler = protocolHandlerMap.get(signature);

                if ( handler != null ) {

                    if ( handler.isBootable() ) {

                        // If it is a bootable device, it's too early to check
                        // the rest - let's boot it first

                        if ( !boot ) {

                            // Oops...

                            throw new BootException("Second time, refusing to boot");
                        }

                        handler.boot(root);

                        // The device enumeration path is broken now, since
                        // the product ID has changed after the boot.
                        // However, it is clear that the device is confined
                        // to the same hub, so we have to just restart the
                        // search on the same hub.

                        throw new BootException("Need to rescan the bus - device booted");
                    }

                    if ( portName == null ) {

                        // No need to check the serial

                        found.add(root);
                        return;
                    }

                    // Port name was specified, so we have to retrieve
                    // it

                    String serial = root.getSerialNumberString();

                    logger.info("Serial found: " + serial);

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
                }

                // VT: NOTE: The only case we get here is if it's not our
                // device. Generally, this should be uncommented only if we're
                // debugging.

                logger.debug("Unknown device: " + signature);
            }

        } finally {
            NDC.pop();
        }
    }

    /**
     * Get device signature, in "${vendor-id}:${product-id}" form.
     *
     * @param dd Device descriptor to extract the signature from.
     *
     * @return Device signature.
     */
    protected final String getSignature(UsbDeviceDescriptor dd) {

        return Integer.toHexString(dd.idVendor() & 0xFFFF) + ":" + Integer.toHexString(dd.idProduct() & 0xFFFF);
    }

    /**
     * Get a protocol handler for a device, if one exists.
     *
     * @param target Device to get the handler for.
     * @return The protocol handler, or {@code null} if there's none.
     */
    protected final UsbProtocolHandler getProtocolHandler(UsbDevice target) {

        String signature = getSignature(target.getUsbDeviceDescriptor());

        return protocolHandlerMap.get(signature);
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
    @Override
    protected void doInit(String portName) throws IOException {

        try {

            theServoController = findUSB(portName);

            UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
            UsbInterface iface = cf.getUsbInterface((byte)0x00);

            if ( iface.isClaimed() ) {

                throw new IOException(
                      "Can't claim interface - already claimed. "
                    + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
            }

            iface.claim();

            // At this point, we've either flying by on the wings of
            // IllegalArgumentException (null portName, none or more than
            // one device), or the phidget serial contains the same value as
            // the requested portName. However, in disconnected mode we can
            // safely assume that the device portName is the same passed
            // from the caller, and just assign it - we don't care if they
            // made a typo

            UsbDeviceDescriptor dd = theServoController.getUsbDeviceDescriptor();
            String serial = theServoController.getSerialNumberString();
            String signature = getSignature(dd);

            // VT: NOTE: Serial number can be null. At least it
            // is with the current firmware release for
            // AdvancedServo. The implication is that there can
            // be just one instance of AdvancedServo on the
            // system.

            if ( serial == null ) {

                // FIXME: this is a hack, but unavoidable one

                serial = "null";
            }

            protocolHandler = protocolHandlerMap.get(signature);

            if ( protocolHandler == null ) {

                throw new UnsupportedOperationException("Vendor/product ID '" + signature + "' is not supported");
            }

            servoSet = new Servo[getServoCount()];

            this.portName = serial;
            connected = true;

        } catch ( Throwable t ) {

            exception(t);

            if (isDisconnectAllowed() && portName != null) {

                    // No big deal, let's just continue

                    this.portName = portName;

                    logger.error("Working in the disconnected mode, cause:", t);

                    return;
            }

            // Too bad, we're not in the disconnected mode

            if (t instanceof IOException) {

                throw (IOException)t;
            }

            throw (IOException)(new IOException().initCause(t));
        }

        // VT: FIXME: Since right now the controller is write-only box and
        // we can't get the current servo position, I'd better set them to a
        // predefined position, otherwise they're going to stay where they
        // were when we ordered a positioning last time, and this might
        // wreak havoc on some calculations (in particular, timing coupled
        // with listeners).

        // FIXME: set the servos to 0.5 now...

        for (Iterator<Servo> i = getServos(); i.hasNext(); ) {

            Servo s = i.next();

            s.setPosition(0.5);
        }
    }

    @Override
    public synchronized Meta getMeta() {

        checkInit();

        if ( protocolHandler == null ) {

            throw new IllegalStateException("Hardware not yet connected, try later");
        }

        return protocolHandler.getMeta();
    }

    @Override
    protected synchronized void checkInit() {

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

        if ( protocolHandler == null && portName == null ) {

            // VT: FIXME: It might be a good idea to try to reinit the
            // controller if portName is not null

            throw new IllegalStateException("Not initialized");
        }
    }

    public synchronized void reset() throws IOException {

        checkInit();

        if ( protocolHandler != null ) {

            try {

                protocolHandler.reset();

            } catch ( UsbException usbex ) {

                throw (IOException) new IOException("Failed to reset USB device").initCause(usbex);
            }
        }
    }

    public int getServoCount() {

        checkInit();

        if (protocolHandler == null) {

            throw new IllegalStateException("Not Initialized (can't determine hardware type?)");
        }

        return protocolHandler.getServoCount();
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
    @Override
    protected Servo createServo(int id) throws IOException {

        // VT: NOTE: There is no sanity checking, I expect the author of the
        // calling code to be sane - this is a protected method

        return protocolHandler.createServo(this, id);
    }

    public final synchronized void usbDeviceAttached(UsbServicesEvent e) {

        try {

            logger.warn("*** USB device attached: " + e.getUsbDevice().getProductString());

            // Let's see if this is by chance our runaway device

            UsbDevice arrival = e.getUsbDevice();
            UsbProtocolHandler handler = getProtocolHandler(arrival);

            if ( handler == null ) {

                // It's not ours

                return;
            }

            // All right, it may be ours.

            // If it is a bootable device, let's try to boot it - it may be
            // something like AdvancedServo after power loss

            if ( handler.isBootable() ) {

                handler.boot(arrival);

                // We'll get another notification shortly

                return;
            }

            String arrivalSerial = arrival.getSerialNumberString();

            if ( portName != null ) {

                if ( portName.equals(arrivalSerial) ) {

                    // Damn! This *is* our runaway device...

                    // VT: NOTE: upon departure, theServoController should have
                    // become null

                    theServoController = arrival;
                    UsbConfiguration cf = theServoController.getActiveUsbConfiguration();
                    UsbInterface iface = cf.getUsbInterface((byte)0x00);

                    if ( iface.isClaimed() ) {

                        throw new IOException("Can't claim interface - already claimed. "
                        + "Make sure no other applications or modules (hid.o or phidgetservo.o in particular) use this device");
                    }

                    iface.claim();


                    // A protocol handler is basically a singleton in this
                    // context, let's override it just in case

                    protocolHandler = handler;

                    // Protocol handler may be stateful, need to reset it

                    handler.reset();

                    // VT: FIXME: Broadcast arrival notification

                    logger.info("*** Restored device");

                    return;

                } else {

                    // Nope, it's not ours

                    return;
                }

            } else {

                // portName is null, let's see what we have

                throw new Error("Not Implemented: restoring a device with null serial number");
            }

        } catch ( Throwable t ) {

            logger.warn("Unhandled exception", t);
        }
    }

    public final synchronized void usbDeviceDetached(UsbServicesEvent e) {

        try {

            logger.warn("*** USB device detached: " + e.getUsbDevice().getProductString());

            UsbDevice departure = e.getUsbDevice();

            if ( departure == theServoController ) {

                // Ouch! It's ours!

                connected = false;
                theServoController = null;

                // VT: FIXME: Notify listeners
            }

        } catch ( Throwable t ) {

            logger.warn("Unhandled exception", t);
        }
    }

    /**
     * Unconditionally throw the <code>IOException</code>.
     *
     * @param found Set of devices found during enumeration.
     * @exception IOException with the list of device serial numbers found.
     * @throws UsbException if there was a problem on a USB protocol level.
     */
    private void tooManyDevices(Set<UsbDevice> found) throws IOException, UsbException {

        String message = "No port name specified, multiple PhidgetServo devices found:";

        for ( Iterator<UsbDevice> i = found.iterator(); i.hasNext(); ) {

            UsbDevice next = i.next();
            String serial = next.getSerialNumberString();
            message += " " + serial;
        }

        throw new IOException(message);
    }

    /**
     * This exception gets thrown whenever there was a USB device (such as a
     * SoftPhidget) that had to be booted, therefore the normal device
     * enumeration was broken.
     */
    @SuppressWarnings("serial")
    protected class BootException extends Exception {

        BootException(String message) {

            super(message);
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
    protected abstract class UsbProtocolHandler {

        /**
         * Controller metadata.
         */
        private final Meta meta;

        protected UsbProtocolHandler() {

            if ( isBootable() ) {

                // Skip everything

                meta = null;
                return;
            }

            meta = createMeta();
        }

        protected abstract Meta createMeta();

        public final Meta getMeta() {

            return meta;
        }

        /**
         * Whether a device is bootable.
         *
         * Generally, it's not the case, this is why the default
         * implementation returning false is provided.
         *
         * @return true if the device is bootable.
         */
        public boolean isBootable() {

            return false;
        }

        public void boot(UsbDevice target) throws UsbException {

            throw new IllegalAccessError("Operation not supported");
        }

        /**
         * Get the device model name.
         *
         * This method is here because the protocol handlers are create
         * before the actual devices are found. Ideally, the model name
         * should be retrieved from the USB device (and possibly, it will be
         * done so later), but so far this will do.
         *
         * @return Human readable model name.
         */
        protected abstract String getModelName();

        /**
         * Reset the controller.
         *
         * @throws UsbException if there was a problem at USB protocol level.
         */
        public abstract void reset() throws UsbException;

        /**
         * @return the number of servos the controller supports.
         */
        public abstract int getServoCount();

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
         *
         * @exception UsbException if there was a problem sending data to
         * the USB device.
         */
        public abstract void setPosition(int id, double position) throws UsbException;

        /**
         * Silence the controller.
         *
         * VT: FIXME: This better be deprecated - each servo can be silenced
         * on its own
         *
         * @throws UsbException if there was a problem at USB protocol level.
         */
        public abstract void silence() throws UsbException;

        public abstract Servo createServo(ServoController sc, int id) throws IOException;

        public abstract class UsbServo extends HardwareServo {

            protected UsbServo(ServoController sc, int id) throws IOException {

                super(sc, id);
            }

            @Override
            protected final void setActualPosition(double position) throws IOException {

                checkInit();
                checkPosition(position);

                try {

                    protocolHandler.setPosition(id, position);

                    actualPosition = position;
                    actualPositionChanged();

                } catch ( UsbException usbex ) {

                    connected = false;
                    theServoController = null;

                    if ( !isDisconnectAllowed() ) {

                        // Too bad

                        throw (IOException) new IOException("Device departed, disconnect not allowed").initCause(usbex);
                    }

                    logger.warn("Assumed disconnect, reason:", usbex);
                }
            }
        }
    }
}
