package org.freehold.servomaster.device.model;

/**
 * Disconnection listener.
 *
 * This interface allows to receive notifications about the disconnectable
 * device arrivals and departures.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: DisconnectListener.java,v 1.2 2002-03-09 05:23:16 vtt Exp $
 */
public interface DisconnectListener {

    public void deviceArrived(ServoController device);
    public void deviceDeparted(ServoController device);
}
