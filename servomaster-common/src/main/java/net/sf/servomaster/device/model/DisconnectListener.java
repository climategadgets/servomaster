package net.sf.servomaster.device.model;

/**
 * Disconnection listener.
 *
 * This interface allows to receive notifications about the disconnectable
 * device arrivals and departures.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2002-2009
 */
public interface DisconnectListener {

    void deviceArrived(ServoController device);
    void deviceDeparted(ServoController device);
}
