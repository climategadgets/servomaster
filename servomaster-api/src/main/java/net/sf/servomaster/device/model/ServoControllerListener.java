package net.sf.servomaster.device.model;

/**
 * Allows to track the servo controller status change.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public interface ServoControllerListener extends DisconnectListener, ProblemListener<ServoController>, SilentDeviceListener {

}
