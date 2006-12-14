package org.freehold.servomaster.device.impl.ft;

/**
 * <a href="http://www.ferrettronics.com/product639.html">FerretTronics
 * FT639</a> servo controller constants.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: FT639Constants.java,v 1.4 2006-12-14 09:17:11 vtt Exp $
 */
public interface FT639Constants {

    /**
     * A byte to send to the controller to switch it to the setup mode.
     */
    byte MODE_SETUP = 0x7A;

    /**
     * A byte to send to the controller to switch it to the active mode.
     */
    byte MODE_ACTIVE = 0x75;

    /**
     * A byte to send to the controller (in the setup mode) to make it
     * produce pulses corresponding to 90\u00B0 (short) servo range.
     */
    byte PULSE_SHORT = 85;

    /**
     * A byte to send to the controller (in the setup mode) to make it
     * produce pulses corresponding to 180\u00B0 (long) servo range.
     */
    byte PULSE_LONG = 90;
}
