package org.freehold.servomaster.view;

/**
 * The servo controller view.
 *
 * <p>
 *
 * If the servo controller provides the features not available in the {@link
 * org.freehold.servomaster.device.model.ServoController ServoController
 * interface}, the class implementing this interface should be able to add
 * the display and control features to the {@link Console servo controller
 * console}.
 *
 * <p>
 *
 * This functionality is not mandatory, but recommended (all in all, how
 * else would people know how smart your controller is???).
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoControllerView.java,v 1.1 2001-09-05 05:30:23 vtt Exp $
 */
import org.freehold.servomaster.device.model.ServoController;

public interface ServoControllerView {

    /**
     * Initialize the servo controller view with the controller.
     *
     * @param controller The controller to control and/or display the
     * features of.
     */
    public void init(ServoController controller);
}
