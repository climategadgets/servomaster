package org.freehold.servomaster.device.model;

/**
 * Disconnection listener.
 *
 * This interface allows to receive notifications about the disconnectable
 * device arrivals and departures.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2005
 * @version $Id: DisconnectListener.java,v 1.3 2006-12-14 09:17:10 vtt Exp $
 */
public interface DisconnectListener {

    void deviceArrived(ServoController device);
    void deviceDeparted(ServoController device);
}
