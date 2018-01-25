package net.sf.servomaster.device.impl.usb.phidget;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.view.ServoControllerView;
import net.sf.servomaster.view.ServoRangeControl;

/**
 * This class renders and allows to control the features specific to the
 * PhidgetServo servo controller.
 *
 * <p>
 *
 * The features specific to PhidgetServo are:
 *
 * <ul>
 *
 * <li> Setting the minimum and maximum pulse length (for Phidget v3
 *      protocol) or range (for Phidget v4 protocol) for each individual
 *      servo.
 *
 * <li> Setting the silent mode for each individual servo (for Phidget v3
 *      protocol).
 *
 * <li> Setting the velocity and acceleration for each individual servo (for
 *      Phidget v4 protocol).
 *
 * <li> Enabling or disabling disconnected operation (when the actual USB
 *      device is not plugged in).
 *
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class PhidgetServoControllerView extends JPanel implements ServoControllerListener, ServoControllerView {

    private Logger logger = Logger.getLogger(getClass());

    private PhidgetServoController controller;

    @Override
 public void init(ServoController controller) {

        this.controller = (PhidgetServoController)controller;

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

        setBorder(BorderFactory.createTitledBorder("PhidgetServo specific controls"));
    }

    @Override
    public void silentStatusChanged(ServoController controller, boolean mode) {

        // FIXME

        //logger.debug("Silent: " + mode);
    }

    @Override
    public void deviceArrived(ServoController device) {

        logger.warn("deviceArrived is not implemented by " + getClass().getName());
    }

    @Override
    public void deviceDeparted(ServoController device) {

        logger.warn("deviceDeparted is not implemented by " + getClass().getName());
    }

    @Override
    public void exception(Object source, Throwable t) {

        logger.error("Problem with " + Integer.toHexString(source.hashCode()) + ":", t);
    }
}
