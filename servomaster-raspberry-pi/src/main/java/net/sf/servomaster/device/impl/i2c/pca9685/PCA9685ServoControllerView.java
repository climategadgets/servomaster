package net.sf.servomaster.device.impl.i2c.pca9685;

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

                add(new ServoPanel(i.next()));
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

    @SuppressWarnings("serial")
    private class ServoPanel extends JPanel implements ChangeListener, ItemListener {

        private Servo servo;
        private JCheckBox silentBox;
        private JLabel silentLabel;

        private JLabel rangeLabel;
        private JSlider minSlider;
        private JSlider maxSlider;

        private String units;
        private int defaultRangeMin;
        private int defaultRangeMax;
        private int min;
        private int max;

        ServoPanel(Servo servo) {

            this.servo = servo;

            setBorder(BorderFactory.createTitledBorder("ID: " + servo.getName()));

            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();

            setLayout(layout);

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 2;
            cs.gridheight = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            // The servo range now can be calculated based on the metadata
            // provided by the servo itself. Note that in this case we a)
            // know that the controller supports this particular metadata b)
            // we're taking the controller meta, not the servo meta, 'cause
            // we need the defaults.

            Meta meta = servo.getController().getMeta();
            units = meta.getProperty("servo/range/units").toString();

            String min = meta.getProperty("servo/range/min").toString();
            String max = meta.getProperty("servo/range/max").toString();

            defaultRangeMin = Integer.parseInt(min);
            defaultRangeMax = Integer.parseInt(max);

            rangeLabel = new JLabel(min + units + "/" + max + units);
            rangeLabel.setToolTipText("Current minimum/maximum servo position");
            rangeLabel.setBorder(BorderFactory.createTitledBorder("Range"));

            layout.setConstraints(rangeLabel, cs);
            add(rangeLabel);

            cs.gridy++;
            cs.gridwidth = 1;
            cs.gridheight = 2;

            cs.fill = GridBagConstraints.BOTH;
            cs.weighty = 1;

            // Now that we have the metadata, we can properly calculate the
            // minimum and maximum slider positions. Let's suppose that the
            // default servo controller setup is OK for most servos, and
            // allow 25% edge outside the default range. This is a demo
            // console, all in all, so the exact range doesn't really
            // matter.

            // VT: NOTE: What it does matter for, though, is determining the
            // physical range of each particular servo.

            int delta = (defaultRangeMax - defaultRangeMin) / 4;

            // Same goes for ticks.

            int majorTicks = delta;
            int minorTicks = majorTicks / 10;

            minSlider = new JSlider(JSlider.VERTICAL, defaultRangeMin - delta, defaultRangeMin + delta, defaultRangeMin);
            minSlider.setToolTipText("Move this to set the minimum servo position");
            minSlider.setMajorTickSpacing(majorTicks);
            minSlider.setMinorTickSpacing(minorTicks);
            minSlider.setPaintTicks(true);
            minSlider.setPaintLabels(true);
            minSlider.setSnapToTicks(false);

            layout.setConstraints(minSlider, cs);
            add(minSlider);

            cs.gridx = 1;

            maxSlider = new JSlider(JSlider.VERTICAL, defaultRangeMax - delta, defaultRangeMax + delta, defaultRangeMax);
            maxSlider.setToolTipText("Move this to set the maximum servo position");
            maxSlider.setMajorTickSpacing(majorTicks);
            maxSlider.setMinorTickSpacing(minorTicks);
            maxSlider.setPaintTicks(true);
            maxSlider.setPaintLabels(true);
            maxSlider.setSnapToTicks(false);

            layout.setConstraints(maxSlider, cs);
            add(maxSlider);

            minSlider.addChangeListener(this);
            maxSlider.addChangeListener(this);

        }

        @Override
        public void stateChanged(ChangeEvent e) {

            Object source = e.getSource();

            if ( source == minSlider ) {

                min = minSlider.getValue();

                servo.getMeta().setProperty("servo/range/min", Integer.toString(min));
                rangeLabel.setText(Integer.toString(min) + units + "/" + max + units);

            } else if ( source == maxSlider ) {

                max = maxSlider.getValue();

                servo.getMeta().setProperty("servo/range/max", Integer.toString(max));
                rangeLabel.setText(Integer.toString(min) + units + "/" + max + units);
            }
        }

        /**
         * React to checkbox status changes.
         */
        @Override
        public void itemStateChanged(ItemEvent e) {

            if ( e.getSource() == silentBox ) {

                boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

                /*

                // VT: FIXME: servo silent support has to be implemented first

                try {

                    servo.setSilentMode(selected);

                } catch ( IOException ioex ) {

                    logger.warn("Unhandled exception", ioex);
                }
                 */

                // VT: FIXME: Reflect the changes on the silentLabel
            }
        }
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
