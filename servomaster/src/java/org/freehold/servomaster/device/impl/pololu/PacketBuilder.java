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
 * @version $Id: PacketBuilder.java,v 1.1 2005-01-13 06:34:53 vtt Exp $
 */
public class PacketBuilder {

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
        buffer[4] = (byte)(position & 0x00FF);
        buffer[5] = (byte)((position & 0xFF00) >> 8);
        
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

}
