package org.freehold.servomaster.device.model;

/**
 * Provides the information about the servo.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoMetaData.java,v 1.1 2001-12-14 21:58:12 vtt Exp $
 */
public interface ServoMetaData {

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
     * Get the time it takes the servo to perform a 60 degree turn under the
     * given {@link #getVoltage voltage}.
     *
     * @return Speed, seconds.
     */
    public double getSpeed();

}
