package org.freehold.servomaster.device.impl.usb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.freehold.servomaster.device.model.AbstractServoController;
import org.freehold.servomaster.device.model.Servo;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;

abstract public class AbstractUsbServoController extends AbstractServoController implements UsbServicesListener {

    private UsbServices usbServices;
    private UsbHub virtualRootHub;

    /**
     * Used to prevent polling too fast.
     *
     * VT: FIXME: This may not be necessary if javax.usb properly supports
     * arrival and departure notifications.
     */
    private long lastDetect = 0;
    
    /**
     * The USB device corresponding to the servo controller.
     */
    protected UsbDevice theServoController;
    
    /**
     * Physical servo representation.
     *
     * VT: FIXME: Must be pushed up into AbstractServoController.
     */
    protected Servo servoSet[];
    
    protected AbstractUsbServoController() {
    
        // VT: FIXME: This really belongs to init(), but at this point
        // things are not settled down yet
    
        try {
        
            usbServices = UsbHostManager.getUsbServices();
            virtualRootHub = usbServices.getRootUsbHub();
            
            usbServices.addUsbServicesListener(this);
            
        } catch ( UsbException usbex ) {
        
            throw (IllegalStateException)(new IllegalStateException("USB failure").initCause(usbex));
        }
    }

    protected final UsbDevice findUSB(String portName) throws IOException {

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
            
            if ( (System.currentTimeMillis() - lastDetect) < 1000 ) {
            
                throw new IOException("Polling too fast");
            }
            
            lastDetect = System.currentTimeMillis();
            
            Set found = new HashSet();
            
            try {
            
                find(portName, virtualRootHub, found, true);
            
            } catch ( BootException bex ) {
            
                bex.printStackTrace();
                
                throw new IllegalStateException("BootException shouldn't have propagated here");
            }
            
            if ( found.size() == 1 ) {
            
                // If there's just one object in the set, that's what we wanted in
                // any case

                return (UsbDevice)(found.toArray()[0]);
            }
                
            // Now, let's figure out what the caller wanted.
            
            if ( portName == null ) {
            
                // They just wanted a single device, but didn't know the serial
                // of it
            
                if ( found.isEmpty() ) {
                
                    throw new IOException("No compatible devices found");

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
        
            System.err.println("\nMake sure you have the directory containing libJavaxUsb.so in your LD_LIBRARY_PATH\n");
            
            throw ule;

        } catch ( UsbException usbex ) {
        
            throw (IOException)(new IOException("USB failure").initCause(usbex));
        }
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
    private void find(String portName, UsbDevice root, Set found, boolean boot) throws IOException, UsbException, BootException {
    
        if ( root == null ) {
        
            return;
        }
        
        if ( root.isUsbHub() ) {
        
            List devices = ((UsbHub)root).getAttachedUsbDevices();
            
            for ( Iterator i = devices.iterator(); i.hasNext(); ) {
            
                try {
                
                    find(portName, (UsbDevice)i.next(), found, true);

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
                    
                        System.err.println("Failed to boot SoftPhidget at hub FIXME");
                    }
                }
            }
            
        } else {
        
            // This may be our chance
            
            UsbDeviceDescriptor dd = root.getUsbDeviceDescriptor();
        
            // Check the vendor/product ID first in case the device
            // is not very smart and doesn't support control messages
            
            if ( dd.idVendor() == 0x6c2 ) {
            
                System.err.println("Phidget: " + Integer.toHexString(dd.idProduct()));
            
                // 0x6c2 is a Phidget
                
                switch ( dd.idProduct() ) {
                
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
                        
                        String serial = root.getSerialNumberString();
                        
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
                    
                        System.err.println("Phidget: unknown: " + Integer.toHexString(dd.idProduct()));
                }
            }
        }
    }

    public final void usbDeviceAttached(UsbServicesEvent e) {
    
        try {
        
            System.out.println("*** USB device attached: " + e.getUsbDevice().getProductString());
            
        } catch ( Throwable t ) {
        
            t.printStackTrace();
        }
    }

    public final void usbDeviceDetached(UsbServicesEvent e) {
    
        try {
        
            System.out.println("*** USB device detached: " + e.getUsbDevice().getProductString());
            
        } catch ( Throwable t ) {
        
            t.printStackTrace();
        }
    }
    
    /**
     * Unconditionally throw the <code>IOException</code>.
     *
     * @exception IOException with the list of device serial numbers found.
     */
    private void tooManyDevices(Set found) throws IOException, UsbException {
    
        // VT: FIXME: Chester & Kevin, verify this
        
        String message = "No port name specified, multiple PhidgetServo devices found:";
        
        for ( Iterator i = found.iterator(); i.hasNext(); ) {
        
            UsbDevice next = (UsbDevice)i.next();
            String serial = next.getSerialNumberString();
            message += " " + serial;
        }
        
        throw new IOException(message);
    }
    
    /**
     * Boot the device.
     *
     * VT: FIXME: UnsupportedEncodingException looks weird here.
     *
     * @param target Device to boot.
     */
    abstract protected void boot(UsbDevice target) throws UsbException, UnsupportedEncodingException;

    /**
     * This exception gets thrown whenever there was a USB device (such as a
     * SoftPhidget) that had to be booted, therefore the normal device
     * enumeration was broken.
     */
    protected class BootException extends Exception {
    
        public BootException(String message) {
        
            super(message);
        }
    }
}
