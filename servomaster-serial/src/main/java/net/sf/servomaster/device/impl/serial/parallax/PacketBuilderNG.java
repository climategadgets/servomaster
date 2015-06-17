package net.sf.servomaster.device.impl.serial.parallax;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 * Packet builder for Parallax controller.
 * <p/>
 * Based on {@link net.sf.servomaster.device.impl.pololu.PacketBuilder}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 * @author Copyright &copy; Scott L'Hommedieu 2006
 */
public class PacketBuilderNG {

    private static final Logger logger = Logger.getLogger(PacketBuilderNG.class);

    /**
     * Protocol preamble - literal string "!SC".
     */
    private static final byte[] PREAMBLE = new byte[] {(byte) 33, (byte) 83, (byte) 67};
    
    private static final byte[] SET_PARAMETERS = new byte[]{(byte) 83, (byte) 66, (byte) 82};

    private static int rq = 0;
    private static int size = 0;

    /**
     * Build a byte buffer for "set parameters" command (0x00).
     *
     * @param buffer Buffer to write into.
     * @param speed ???
     *
     * @return Rendered buffer.
     */
    public static byte[] setParameters(byte[] buffer, int speed) {

        //setting baud rate
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.put(PREAMBLE);
        bb.put(SET_PARAMETERS);
        bb.put(speed == 38400 ? (byte) 1 : (byte) 0);
        bb.put((byte) 0x0D);

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
     * @deprecated Need to get rid of this method - it doesn't make sense anymore, and does nothing.
     */
    @Deprecated
    public static byte[] setSpeed(byte servoId, byte speed) {

        // This is not necessary for the Parallax, the speed is sent with every command (setAboslutePosition)
        return null;
    }

    /**
     * Build a byte buffer for "set absolute position" command (0x04).
     *
     * @param buffer Buffer to write into.
     * @param servoId Servo number, zero based.
     * @param velocity Servo velocity. Valid values are 0...63.
     * @param position Servo position. Valid values are (tentatively) 250...1250.
     *
     * @return Rendered buffer.
     */
    public static byte[] setAbsolutePosition(byte[] buffer, byte servoId, byte velocity, short position) {

        if (velocity < 0 || velocity > 63) {
            throw new IllegalArgumentException("Invalid velocity (" + velocity + ") - outside of 0...63 range");
        }

        if (position < 250 || position > 1250) {
            throw new IllegalArgumentException("Invalid position (" + position + ") - outside of 250...1250 range");
        }

        ByteBuffer bb = ByteBuffer.wrap(buffer);

        bb.put(PREAMBLE);
        bb.put(servoId);
        bb.put(velocity);
        bb.put((byte) position);
        bb.put((byte) (position >>> 8));
        bb.put((byte) 0x0D);

        complain(buffer);

        // Let's also check where we are going, just in case

        // Don't forget that Java doesn't have unsigned left shift
        int byte1 = 0xFF & buffer[6];
        int byte2 = 0xFF & buffer[5];
        int checkPosition = (byte1 << 8 | byte2);

        logger.debug("Position (expected/actual): " + Integer.toHexString(position) + "/" + Integer.toHexString(checkPosition));

        return buffer;
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
