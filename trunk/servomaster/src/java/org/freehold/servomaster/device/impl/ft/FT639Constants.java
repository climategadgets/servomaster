package org.freehold.servomaster.device.impl.ft;

/**
 * <a href="http://www.ferrettronics.com/product639.html">FerretTronics
 * FT639</a> servo controller constants.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: FT639Constants.java,v 1.1 2001-08-31 21:38:00 vtt Exp $
 */
public interface FT639Constants {

    public static final byte MODE_SETUP = 0x7A;
    public static final byte MODE_ACTIVE = 0x75;
    public static final byte PULSE_SHORT = 85;
    public static final byte PULSE_LONG = 90;
}
