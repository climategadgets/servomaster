package org.freehold.servomaster.device.model;

/**
 * Provides the information about the servo controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerMetaData.java,v 1.2 2001-12-29 06:33:19 vtt Exp $
 */
public interface ServoControllerMetaData {

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
     * Get the maximum number of servos supported.
     *
     * @return Maximum number of servos supported.
     */
    public int getMaxServos();
    
    /**
     * Get the information about the silent mode support.
     *
     * @return <code>true</code> if the controller supports the silent mode,
     * <code>false</code> otherwise.
     */
    public boolean supportsSilentMode();
    
    /**
     * Get the information about the precision.
     *
     * Precision is measured in number of available servo positions a
     * controller can provide.
     *
     * @return Controller precision, in steps.
     */
    public int getPrecision();
    
    // VT: FIXME: think about latency and bandwidth
}
