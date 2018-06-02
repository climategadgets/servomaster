package net.sf.servomaster.device.impl.serial.pololu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Packet builder for Pololu {@link USB16ServoController USB} and (later)
 * serial controllers.
 *
 * <p/>
 *
 * Since the original Pololu driver for the USB controller is just a USB to
 * serial bridge, it is possible (hopefully --vt) to reuse protocol packet
 * building code between USB and serial controller drivers.
 *
 * <p/>
 *
 * Unfortunately, it's not possible to provide an abstract Pololu controller
 * driver class and then extend it to support USB and serial controllers
 * because USB and serial support has already been implemented in abstract
 * classes, so this solution (for the time being) seems to be better.
 *
 * <p/>
 *
 * This class doesn't implement the complete command set because the only
 * method used to set the servo position is {@link #setAbsolutePosition set
 * absolute position}.
 * 
 * <p/>
 * 
 * IMPORTANT: THIS CLASS IS A DUPLICATE OF {@link net.sf.servomaster.device.impl.usb.pololu.PacketBuilder}.
 * 
 * If you are making any changes here, propagate them over.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2018
 */
public class PacketBuilder {

    private static final Logger logger = LogManager.getLogger(PacketBuilder.class);

    private static int rq = 0;
    private static int size = 0;

    /**
     * Build a byte buffer for "set parameters" command (0x00).
     *
     * <p/>
     *
     * This call ignores the "reverse" and "range" parameters because the
     * only method used to set the servo position is {@link #setAbsolutePosition set absolute position}.
     *
     * @param servoId Servo number, zero based.
     * @param enabled true if the servo is enabled.
     *
     * @return Rendered buffer.
     */
    public static byte[] setParameters(byte servoId, boolean enabled) {

        checkHighBit(servoId);

        byte[] buffer = new byte[5];

        buffer[0] = (byte) 0x80; // start byte
        buffer[1] = (byte) 0x01; // device ID
        buffer[2] = (byte) 0x00; // command
        buffer[3] = servoId;

        // 0x0F is the default range

        byte enabledMask = (byte) (enabled ? 0x40 : 0x00);
        buffer[4] = (byte) (0x0F | enabledMask);

        complain(buffer);

        return buffer;
    }

    /**
     * Build a byte buffer for "set speed" command (0x01).
     *
     * @param servoId Servo number, zero based.
     * @param speed Servo speed.
     *
     * @return Rendered buffer.
     */
    public static byte[] setSpeed(byte servoId, byte speed) {

        checkHighBit(servoId);
        checkHighBit(speed);

        byte[] buffer = new byte[5];

        buffer[0] = (byte) 0x80; // start byte
        buffer[1] = (byte) 0x01; // device ID
        buffer[2] = (byte) 0x01; // command
        buffer[3] = servoId;
        buffer[4] = speed;

        complain(buffer);

        return buffer;
    }

    /**
     * Build a byte buffer for "set absolute position" command (0x04).
     *
     * @param servoId Servo number, zero based.
     * @param position Servo position. Valid values are 500...5500.
     *
     * @return Rendered buffer.
     */
    public static byte[] setAbsolutePosition(byte servoId, short position) {

        checkHighBit(servoId);

        if (position < 500 || position > 5500) {

            throw new IllegalArgumentException("Invalid position (" + position + ") - outside of 500...5500 range");
        }

        byte[] buffer = new byte[6];

        buffer[0] = (byte) 0x80; // start byte
        buffer[1] = (byte) 0x01; // device ID
        buffer[2] = (byte) 0x04; // command
        buffer[3] = servoId;

        // According to documentation, Data2 contains the lower 7 bits, and
        // Data1 contains upper bits

        buffer[4] = (byte) ((position & 0xFF80) >> 7);
        buffer[5] = (byte) (position & 0x007F);

        complain(buffer);

        // According to documentation, all the bytes except the start byte
        // must have the high bit off. Let's see if we're doing it right.

        for (int offset = 1; offset < 6; offset++) {

            try {
                checkHighBit(buffer[offset]);
            } catch (IllegalArgumentException ex) {
                throw (IllegalStateException) new IllegalStateException("Byte at offset " + offset + " has high bit set").initCause(ex);
            }
        }

        // Let's also check where we are going, just in case

        int checkPosition = buffer[4] << 7 | buffer[5];

        logger.debug("Position: " + checkPosition);

        return buffer;
    }

    /**
     * Check if the value is valid.
     * <p/>
     * High bit should be off. If it is not,
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param value value to check.
     */
    private static void checkHighBit(byte value) {

        if ((value & (byte) 0x80) != 0) {

            throw new IllegalArgumentException("Invalid value (" + Integer.toHexString(value) + ") - high bit must be off");
        }
    }

    private static void complain(byte[] buffer) {

        rq++;
        size += buffer.length;

        if (true) {

            // If the device is not set up right, the output buffer will get
            // stuck soon

            logger.debug("" + rq + " requests, " + size + " bytes");

            // Let's see if the buffer content is OK

            StringBuilder sb = new StringBuilder();

            for (int offset = 0; offset < buffer.length; offset++) {

                int b = buffer[offset] & 0x00FF;

                if (b < 0x10) {
                    sb.append('0');
                }

                sb.append(Integer.toHexString(b)).append(' ');
            }

            logger.debug("Buffer (" + buffer.length + " bytes): " + sb.toString());
        }
    }
}
