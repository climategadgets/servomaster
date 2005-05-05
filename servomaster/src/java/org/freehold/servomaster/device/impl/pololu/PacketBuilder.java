package org.freehold.servomaster.device.impl.pololu;

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
 * @version $Id: PacketBuilder.java,v 1.6 2005-05-05 06:42:52 vtt Exp $
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
    public static byte[] setParameters(byte servoId, boolean enabled) {
    
        checkHighBit(servoId);
    
        byte buffer[] = new byte[5];
        
        buffer[0] = (byte)0x80; // start byte
        buffer[1] = (byte)0x01; // device ID
        buffer[2] = (byte)0x00; // command
        buffer[3] = servoId;
        
        // 0xFF is the default range
        
        byte enabledMask = (byte)(enabled ? 0x4000 : 0x0000);
        buffer[4] = (byte)(0xFF | enabledMask);
        
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
    
        checkHighBit(servoId);
        checkHighBit(speed);
    
        byte buffer[] = new byte[5];
        
        buffer[0] = (byte)0x80; // start byte
        buffer[1] = (byte)0x01; // device ID
        buffer[2] = (byte)0x01; // command
        buffer[3] = servoId;
        buffer[4] = speed;
        
        complain(buffer);
        
        return buffer;
    }

    /**
     * Build a byte buffer for "set absolute position" command (0x04).
     *
     * @param servoId Servo number, zero based.
     *
     * @param position Servo position. Valid values are 500...5500.
     */
    public static byte[] setAbsolutePosition(byte servoId, short position) {
    
        checkHighBit(servoId);
    
        if ( position < 500 || position > 5500 ) {
        
            throw new IllegalArgumentException("Invalid position (" + position + ") - outside of 500...5500 range");
        }
    
        byte buffer[] = new byte[6];
        
        buffer[0] = (byte)0x80; // start byte
        buffer[1] = (byte)0x01; // device ID
        buffer[2] = (byte)0x04; // command
        buffer[3] = servoId;
        
        // According to documentation, Data2 contains the lower 7 bits, and
        // Data1 contains upper bits
        
        buffer[4] = (byte)((position & 0xFF80) >> 7);
        buffer[5] = (byte)(position & 0x007F);
        
        complain(buffer);
        
        // According to documentation, all the bytes except the start byte
        // must have the high bit off. Let's see if we're doing it right.
        
        for (int offset = 1; offset < 6; offset++) {
        
            try {
            
                checkHighBit(buffer[offset]);
                
            } catch (IllegalArgumentException ex) {
            
                throw (IllegalStateException) (new IllegalStateException("Byte at offset " + offset + " has high bit set").initCause(ex));
            }
        }
        
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
