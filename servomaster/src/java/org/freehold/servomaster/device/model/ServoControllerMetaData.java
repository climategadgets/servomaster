package org.freehold.servomaster.device.model;

/**
 * Provides the information about the servo controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerMetaData.java,v 1.1 2001-12-14 22:08:09 vtt Exp $
 */
public interface ServoControllerMetaData {

    /**
     * Get the manufacturer URL.
     *
     * @return The manufacturer URL as string.
     */
    public String getManufacturerURL();
    
    /**
     * Get the maximum number of servos supported.
     *
     * @return Maximum number of servos supported.
     */
    public int getMaxServos();
    
    // VT: FIXME: think about latency and bandwidth
}
