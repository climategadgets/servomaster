package org.freehold.servomaster.device.impl.serial;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.Vector;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.freehold.servomaster.device.model.AbstractServoController;

/**
 * Base class for all serial servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractSerialServoController.java,v 1.1 2005-01-13 23:14:27 vtt Exp $
 */
abstract public class AbstractSerialServoController extends AbstractServoController {

    /**
     * Open timeout.
     *
     * <p>
     *
     * Wait this much trying to open the serial port.
     */
    public static final int OPEN_TIMEOUT = 5000;
    
    /**
     * The serial port.
     */
    protected SerialPort port = null;
    
    /**
     * The serial port output stream.
     */
    protected OutputStream serialOut;
    
    /**
     * Initialize the controller.
     *
     * @param portName Serial port name recognized by <a
     * href="http://java.sun.com/products/javacomm/" target="_top">Java
     * Communications API</a>.
     *
     * @exception IllegalStateException if the controller has already been initialized.
     */
    protected void doInit(String portName) throws IOException {
    
        this.portName = portName;
        
        if ( this.portName == null ) {
        
            throw new IllegalArgumentException("null portName is invalid: this controller doesn't support automated discovery");
        }

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

    public String getPort() {
    
        checkInit();
        
        return portName;
    }
    
    protected void checkInit() {

        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    }
}