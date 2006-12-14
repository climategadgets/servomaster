package org.freehold.servomaster.device.impl.parallax;

import java.nio.ByteBuffer;

/**
 * Packet builder for Pololu {@link USB16ServoController USB} and (later)
 * serial controllers.
 *
 * <p>
 *
 * Since the original Pololu driver for the USB controller is just a USB to
 * serial bridge, it is possible (hopefully --vt) to reuse protocol packet
 * building code between USB and serial controller drivers.
 *
 * <p>
 *
 * Unfortunately, it's not possible to provide an abstract Pololu controller
 * driver class and then extend it to support USB and serial controllers
 * because USB and serial support has already been implemented in abstract
 * classes, so this solution (for the time being) seems to be better.
 *
 * <p>
 *
 * This class doesn't implement the complete command set because the only
 * #method used to set the servo position is {@link setAbsolutePosition set
 * #absolute position}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: PacketBuilder.java,v 1.1 2006-12-14 12:38:28 vtt Exp $
 */
public class PacketBuilder {

    private static int rq = 0;
    private static int size = 0;

    /**
     * Build a byte buffer for "set parameters" command (0x00).
     *
     * This call ignores the "reverse" and "range" parameters because the
     * only method used to set the servo position is {@link
     * #setAbsolutePosition set absolute position}.
     *
     * @param servoId Servo number, zero based.
     *
     * @param enabled true if the servo is enabled.
     */
    public static byte[] setParameters(int speed) {
    
    	;
    	
        //setting baud rate
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(new byte[]{(byte)33, (byte)83, (byte)67, (byte)83, (byte)66, (byte)82, (speed == 38400)?(byte)1:(byte)0});
        bb.put((byte)0x0D);

        byte buffer[] = bb.array();
        complain(buffer);
    
    
        return buffer;
    }

    /**
     * Build a byte buffer for "set speed" command (0x01).
     *
     * @param servoId Servo number, zero based.
     *
     * @param speed Servo speed.
     */
    public static byte[] setSpeed(byte servoId, byte speed) {
    
        
    /*	
     * This is not necessary for the Parallax, the speed is sent with every command (setAboslutePosition)
     * 
     * checkHighBit(servoId);
        checkHighBit(speed);
 
         
        byte buffer[] = new byte[5];
        
        buffer[0] = (byte)0x80; // start byte
        buffer[1] = (byte)0x01; // device ID
        buffer[2] = (byte)0x01; // command
        buffer[3] = servoId;
        buffer[4] = speed;
        
        buffer = bb.array();
        
        complain(buffer);
        
        return buffer;
        */
    	return null;
    }

    /**
     * Build a byte buffer for "set absolute position" command (0x04).
     *
     * @param servoId Servo number, zero based.
     *
     * @param position Servo position. Valid values are 500...5500.
     */
    public static byte[] setAbsolutePosition(byte servoId, byte velocity, short position) {
    
        //checkHighBit(servoId);
    
       
    
        
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(new byte[]{(byte)33, (byte)83, (byte)67});
        bb.put(servoId);
        bb.put(velocity);
        bb.put((byte)position);
        bb.put((byte)(position>>>8));
        bb.put((byte)0x0D);
        byte buffer[] = bb.array();
        
        /*
        byte buffer[] = new byte[8];
        
        buffer[0] = (byte)33; // start byte
        buffer[1] = (byte)83; // device ID
        buffer[2] = (byte)67; // command
        buffer[3] = servoId;
        buffer[4] = velocity; // start byte
        buffer[5] = (byte)position; // low byte
        buffer[6] = ((byte)(position>>>8)); // high byte
        buffer[7] = (byte)0x0D;
        */
        complain(buffer);
          
        if (false) {
        
            // Let's also check where we are going, just in case
            
            int checkPosition = (buffer[4] << 7) | buffer[5];
            
            System.err.println("Position: " + checkPosition);
        }    
        
        return buffer;
    }
    
    /**
     * Check if the value is valid.
     *
     * High bit should be off. If it is not,
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param value value to check.
     */
    private static void checkHighBit(byte value) {
    
        if ( (value & (byte)0x80) != 0 ) {
        
            throw new IllegalArgumentException("Invalid value (" + Integer.toHexString(value) + ") - high bit must be off");
        }
    }
    
    private static void complain(byte buffer[]) {
    
        rq++;
        size += buffer.length;
        
        if (true) {
        
            // If the device is not set up right, the output buffer will get
            // stuck soon
            
            System.err.println("" + rq + " requests, " + size + " bytes");
        
            // Let's see if the buffer content is OK
            
            StringBuffer sb = new StringBuffer();
            
            for (int offset = 0; offset < buffer.length; offset++ ) {
            
                int b = buffer[offset] & 0x00FF;
                
                if (b < 0x10) {
                
                    sb.append('0');
                }
                
                sb.append(Integer.toHexString(b)).append(' ');
            }
            
            System.err.println("Buffer (" + buffer.length + " bytes): " + sb.toString());
        }
    }
}
