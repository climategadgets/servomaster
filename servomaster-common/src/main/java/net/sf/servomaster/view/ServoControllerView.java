package net.sf.servomaster.view;

import net.sf.servomaster.device.model.ServoController;

/**
 * The servo controller view.
 *
 * <p>
 *
 * If the servo controller provides the features not available in the {@link ServoController ServoController interface},
 * the class implementing this interface should be able to add the display and control features to the
 * {@link Console servo controller console}.
 *
 * <p>
 *
 * This functionality is not mandatory, but recommended (all in all, how
 * else would people know how smart your controller is???).
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 */
public interface ServoControllerView {

    /**
     * Initialize the servo controller view with the controller.
     *
     * @param controller The controller to control and/or display the
     * features of.
     *
     * @exception IllegalArgumentException if the controller passed into
     * this method is <code>null</code>.
     */
    void init(ServoController controller);
}
