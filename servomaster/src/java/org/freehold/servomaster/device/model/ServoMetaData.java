package org.freehold.servomaster.device.model;

/**
 * Provides the information about the servo.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoMetaData.java,v 1.3 2001-12-29 06:33:19 vtt Exp $
 */
public interface ServoMetaData {

    /**
     * Get the manufacturer URL.
     *
     * @return The manufacturer URL as string.
     */
    public String getManufacturerURL();
    
    /**
     * Get the manufacturer name.
     *
     * @return The manufacturer name.
     */
    public String getManufacturerName();
    
    /**
     * Get the controller model name.
     *
     * @return The controller model name.
     */
    public String getModelName();
    
    /**
     * Get the voltage that has to be applied to the servo to provide the
     * advertised {@link #getTorque torque} and {@link #getSpeed speed}.
     *
     * @return Voltage, volts.
     */
    public double getVoltage();
    
    /**
     * Get the torque that the servo is able to provide under the given
     * {@link #getVoltage voltage}.
     *
     * @return Torque, kg-cm.
     */
    public double getTorque();
    
    /**
     * Get the time it takes the servo to perform a 60\u00B0 turn under the
     * given {@link #getVoltage voltage}.
     *
     * @return Speed, seconds.
     */
     
    // VT: FIXME: It may be better to return degrees per second?
    
    public double getSpeed();
    
    /**
     * Get the servo range.
     *
     * Most servos can operate within 90\u00B0, some can support 180\u00B0. 
     * It is possible for the servo abstraction implementation to return the
     * actual range of the servo, including the preset limits, so be
     * prepared for the value being other than 90 or 180.
     */
    public int getRange();
}
