package org.freehold.servomaster.device.model;

/**
 * Provides the information about the servo controller.
 *
 * <p>
 *
 * Any method is allowed to throw the
 * <code>UnsupportedOperationException</code> if the hardware controller
 * doesn't support the corresponding feature, or the information doesn't
 * make sense in the context.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerMetaData.java,v 1.3 2002-01-05 04:05:23 vtt Exp $
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
    
    // VT: FIXME: think about latency
    
    /**
     * Get the information about the controller bandwidth.
     *
     * Note that this is different from the port speed.
     *
     * @return Maximum commands per seconds.
     */
    public int getBandwidth();
}
