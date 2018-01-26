package net.sf.servomaster.device.impl.i2c.pca9685;

import java.awt.GridLayout;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.view.ServoControllerView;
import net.sf.servomaster.view.ServoRangeControl;

/**
 * This class renders and allows to control the features specific to the
 * PCA9685 servo controller.
 *
 * <p>
 *
 * The features specific to PCA9685 servo are:
 *
 * <ul>
 *
 * <li> Setting the minimum and maximum pulse length for each individual servo.
 *
 * <li> Setting the silent mode for each individual servo.
 *
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class PCA9685ServoControllerView extends JPanel implements ServoControllerListener, ServoControllerView {

    private static final long serialVersionUID = -175147191184218500L;

    private Logger logger = Logger.getLogger(getClass());

    private PCA9685ServoController controller;

    @Override
 public void init(ServoController controller) {

        this.controller = (PCA9685ServoController)controller;

        controller.addListener(this);

        List<Servo> servos = new LinkedList<Servo>();

        try {

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                servos.add(i.next());
            }

            setLayout(new GridLayout(1, servos.size()));

            for ( Iterator<Servo> i = servos.iterator(); i.hasNext(); ) {

                add(new ServoRangeControl(i.next()));
            }

        } catch ( IOException ioex ) {

            // We're probably screwed, this shouldn't have really happened

            logger.error("Can't get the servos, cause:", ioex);
        }

        setBorder(BorderFactory.createTitledBorder("PCA9685 specific controls"));
    }

    @Override
    public void silentStatusChanged(ServoController controller, boolean mode) {

        // FIXME

        //logger.debug("Silent: " + mode);
    }

    @Override
    public void deviceArrived(ServoController device) {

        logger.warn("deviceArrived is not implemented by " + controller.getClass().getName());
    }

    @Override
    public void deviceDeparted(ServoController device) {

        logger.warn("deviceDeparted is not implemented by " + controller.getClass().getName());
    }

    @Override
    public void exception(Object source, Throwable t) {

        logger.error("Problem with " + Integer.toHexString(source.hashCode()) + ":", t);
    }
}
