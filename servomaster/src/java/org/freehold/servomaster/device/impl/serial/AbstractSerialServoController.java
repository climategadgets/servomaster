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
import org.freehold.servomaster.device.model.Servo;

/**
 * Base class for all serial servo controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractSerialServoController.java,v 1.10 2005-01-21 06:43:38 vtt Exp $
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
    private OutputStream serialOut;
    
    public AbstractSerialServoController() {
    }

    public AbstractSerialServoController(String portName) throws IOException {
    
        super(portName);
    }
    
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
        
            throw new IllegalArgumentException("null portName is invalid: serial controllers don't support automated discovery");
        }

        servoSet = new Servo[getServoCount()];
        
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
        
            throw (IOException)(new IOException("Unsupported comm operation").initCause(ucoex));
        }
        
        reset();
    }

    protected void checkInit() {

        if ( portName == null ) {
        
            throw new IllegalStateException("Not initialized");
        }
    }

    /**
     * Find out whether the controller is connected.
     *
     * So far, I haven't seen a single serial controller that can return
     * some status. Therefore, for the time being this method will always
     * return true, until a reliable way to deal with it will be found.
     *
     * @return Unconditional true.
     */
    public final boolean isConnected() {
    
        return true;
    }
    
    /**
     * Send the byte down the {@link #serialOut serial port stream}.
     *
     * @param b Byte to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected synchronized void send(byte b) throws IOException {
    
        serialOut.write(b);
        serialOut.flush();
    }
    
    /**
     * Send the data buffer down the {@link #serialOut serial port stream}.
     *
     * @param buffer Buffer to send.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected synchronized final void send(byte[] buffer) throws IOException {
    
        // VT: FIXME: Can be optimized
        
        for ( int offset = 0; offset < buffer.length; offset++ ) {
        
            serialOut.write(buffer[offset]);
        }
        
        serialOut.flush();
    }
}
