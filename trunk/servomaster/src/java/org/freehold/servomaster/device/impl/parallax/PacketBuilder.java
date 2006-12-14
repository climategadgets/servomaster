package org.freehold.servomaster.device.impl.parallax;

import java.nio.ByteBuffer;

/**
 * Packet builder for Parallax controller.
 *
 * Based on {@link org.freehold.servomaster.device.impl.pololu.PacketBuilder}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Scott L'Hommedieu</a> 2006
 * @version $Id: PacketBuilder.java,v 1.2 2006-12-14 12:57:16 vtt Exp $
 */
public class PacketBuilder {

    private static int rq = 0;
    private static int size = 0;

    /**
     * Build a byte buffer for "set parameters" command (0x00).
     *
     * @param speed ???
     *
     * @return Rendered buffer.
     */
    public static byte[] setParameters(int speed) {

        //setting baud rate
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put(new byte[]{(byte)33, (byte)83, (byte)67, (byte)83, (byte)66, (byte)82, (speed == 38400)?(byte)1:(byte)0});
        bb.put((byte)0x0D);

        byte[] buffer = bb.array();
        complain(buffer);


        return buffer;
    }

    /**
     * Build a byte buffer for "set speed" command (0x01).
     *
     * @param servoId Servo number, zero based.
     *
     * @param speed Servo speed.
     *
     * @return Rendered buffer.
     */
    public static byte[] setSpeed(byte servoId, byte speed) {


        /*
         * This is not necessary for the Parallax, the speed is sent with every command (setAboslutePosition)
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
