package org.freehold.servomaster.test;

import java.io.IOException;

import usb.core.Bus;
import usb.core.Configuration;
import usb.core.ControlMessage;
import usb.core.Descriptor;
import usb.core.Device;
import usb.core.DeviceDescriptor;
import usb.core.Host;
import usb.core.HostFactory;
import usb.core.Interface;
import usb.core.USBListener;

/**
 * This class just prints out the map of the devices currently attached to
 * the USB port.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001,2002
 * @version $Id: USB_Map.java,v 1.2 2002-03-09 05:23:16 vtt Exp $
 */
public class USB_Map {

    /**
     * USB host available.
     *
     * FIXME: handle the case of multiple hosts available.
     */
    private Host usbHost;
    
    /**
     * USB busses available.
     */
    private Bus usbBusSet[];
    
    private Listener listener;
    
    public static void main(String args[]) {
    
        new USB_Map().run();
    }
    
    public void run() {
    
        try {
        
            // Look if we have the USB host available
            
            usbHost = HostFactory.getHost();
            
            // it shouldn't happen, but could
            
            if ( usbHost == null ) {
            
                throw new IllegalStateException("usbHost is still null");
            }
        
            System.err.println("Host: " + usbHost);
            
            listener = new Listener();
            usbHost.addUSBListener(listener);
            
            usbBusSet = usbHost.getBusses();
            
            print(0, "<host>");
            
            for ( int idx = 0; idx < usbBusSet.length; idx++ ) {
            
                print(2, usbBusSet[idx]);
            
            }
            
            print(0, "</host>");
                
        } catch ( Throwable t ) {
        
            t.printStackTrace();
        }
        
    
    }
    
    private void print(int indent, String message) {
    
        while ( indent-- > 0 ) {
        
            System.err.print(" ");
        }
        
        System.err.println(message);
    }
    
    private void print(int indent, Bus bus) throws IOException {
    
        print(indent, "<bus id=" + bus.getBusId() + ">");
        
            print(indent + 2, bus.getRootHub());
            
        print(indent, "</bus>");
    }
    
    private void print(int indent, Device device) throws IOException {
    
        String tag = device.getDeviceDescriptor().getDeviceClassName();
        
        print(indent, "<" + tag + " id=" + Integer.toHexString(device.getAddress()) + " port=" + device.getPortIdentifier() + ">");
        printDetails(indent + 2, device);
        
        if ( "hub".equalsIgnoreCase(tag) ) {
        
            int ports = device.getNumPorts();
            
            for ( int port = 0; port < ports; port++ ) {
            
                print(indent + 2, "<port id=" + (port + 1) + ">");
                Device child = device.getChild(port + 1);
                
                if ( child == null ) {
                
                    print(indent + 4, "<!-- no device -->");
                
                } else {
                
                    print(indent + 4, child);
                }
                
                print(indent + 2, "</port>");
            }
        }
        
        print(indent, "</" + tag + ">");
    }
    
    private void printDetails(int indent, Device device) throws IOException {
    
        DeviceDescriptor dd = device.getDeviceDescriptor();
        int languageSet[] = ControlMessage.getLanguages(device);
        int defaultLanguage = 0;
        
        if ( languageSet != null && languageSet.length != 0 ) {
        
            defaultLanguage = languageSet[0];
        }
        
        print(indent, "<descriptor>");
        print(indent + 2, "<class id=" + Integer.toHexString(dd.getDeviceClass()) + " name=" + dd.getDeviceClassName() + "/>");
        print(indent + 2, "<id>");
        print(indent + 4, dd.getDeviceId());
        print(indent + 2, "</id>");
        print(indent + 2, "<protocol id=" + Integer.toHexString(dd.getDeviceProtocol()) + "/>");
        print(indent + 2, "<subclass id=" + Integer.toHexString(dd.getDeviceSubClass()) + "/>");
        print(indent + 2, "<manufacturer>");
        print(indent + 4, dd.getManufacturer(defaultLanguage));
        print(indent + 2, "</manufacturer>");
        print(indent + 2, "<product>");
        print(indent + 4, dd.getProduct(defaultLanguage));
        print(indent + 2, "</product>");
        print(indent, "</descriptor>");
        
        int cfcount = dd.getNumConfigurations();
        
        for ( int cfroot = 0; cfroot < cfcount; cfroot++ ) {
        
            print(indent, device.getConfiguration(cfroot), defaultLanguage);
        }
    }
    
    private void print(int indent, Configuration conf, int defaultLanguage) throws IOException {
    
        int ifcount = conf.getNumInterfaces();
        print(indent, "<configuration>");
        print(indent + 2, "<power max=" + conf.getMaxPower() + "/>");
        print(indent + 2, "<interfaces count=" + ifcount + "/>");
        
        if ( ifcount != 0 ) {
        
            for ( int i = 0; i < ifcount; i++ ) {
            
                for ( int alt = 0; conf.getInterface(i, alt) != null; alt++ ) {
                
                    print(indent + 4, conf.getInterface(i, alt), defaultLanguage);
                }
            }
        }
        
        print(indent, "</configuration>");
    }
    
    private void print(int indent, Interface iface, int defaultLanguage) throws IOException {
    
        print(indent, "<interface name=" + iface.getInterface(defaultLanguage) + " claimedBy=" + iface.getClaimer() + ">");
        print(indent + 2, "<class>");
        print(indent + 4, iface.getInterfaceClassName());
        print(indent + 2, "</class>");
        print(indent, "</interface>");
    }

    protected class Listener implements USBListener {
    
        public void busAdded(Bus bus) {
        
            System.err.println("USB Bus added: " + bus);
        }
        
        public void busRemoved(Bus bus) {
        
            System.err.println("USB Bus removed: " + bus);
        }
        
        public void deviceAdded(Device device) {
        
            System.err.println("USB Device added: " + device);
        }

        public void deviceRemoved(Device device) {
        
            System.err.println("USB Device removed: " + device);
        }
    }
}
