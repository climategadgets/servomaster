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

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.view.ServoControllerView;

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

    private PhidgetServoController controller;

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

                add(new ServoPanel(i.next()));
            }

        } catch ( IOException ioex ) {

            // We're probably screwed, this shouldn't have really happened

            System.err.println("Can't get the servos, cause:");
            ioex.printStackTrace();
        }

        setBorder(BorderFactory.createTitledBorder("PhidgetServo specific controls"));
    }

    public void silentStatusChanged(ServoController controller, boolean mode) {

        // FIXME

        //System.err.println("Silent: " + mode);
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

        private JLabel vaLabel;
        private JSlider vSlider;
        private JSlider aSlider;

        private String velocity;
        private String acceleration;

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

            // Let's find out if the servo supports velocity and
            // acceleration (come in pair)

            try {

                velocity = servo.getMeta().getProperty("servo/velocity").toString();
                acceleration = servo.getMeta().getProperty("servo/acceleration").toString();

                cs.gridy += 2;
                cs.gridx = 0;
                cs.gridwidth = 2;
                cs.gridheight = 1;

                vaLabel = new JLabel(velocity + "/" + acceleration);
                vaLabel.setToolTipText("Current servo velocity and acceleration");
                vaLabel.setBorder(BorderFactory.createTitledBorder("Vel/Accel"));

                layout.setConstraints(vaLabel, cs);
                add(vaLabel);

                cs.gridy++;
                cs.gridwidth = 1;

                vSlider = new JSlider(JSlider.VERTICAL, 0, Integer.parseInt(velocity), Integer.parseInt(velocity));
                vSlider.setToolTipText("Move this to set the servo velocity");
                vSlider.setMajorTickSpacing(90);
                vSlider.setMinorTickSpacing(10);
                vSlider.setPaintTicks(true);
                vSlider.setPaintLabels(true);
                vSlider.setSnapToTicks(false);

                layout.setConstraints(vSlider, cs);
                add(vSlider);

                cs.gridx = 1;

                aSlider = new JSlider(JSlider.VERTICAL, 0, Integer.parseInt(acceleration), Integer.parseInt(acceleration));
                aSlider.setToolTipText("Move this to set the servo acceleration");
                aSlider.setMajorTickSpacing(90);
                aSlider.setMinorTickSpacing(10);
                aSlider.setPaintTicks(true);
                aSlider.setPaintLabels(true);
                aSlider.setSnapToTicks(false);

                layout.setConstraints(aSlider, cs);
                add(aSlider);

                vSlider.addChangeListener(this);
                aSlider.addChangeListener(this);

            } catch ( UnsupportedOperationException uoex ) {

                System.err.println("Servo doesn't support velocity/acceleration");
            }
        }

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

            } else if ( source == vSlider ) {

                velocity = Float.toString((float)vSlider.getValue());
                servo.getMeta().setProperty("servo/velocity", velocity);

                vaLabel.setText(velocity + "/" + acceleration);

            } else if ( source == aSlider ) {

                acceleration = Float.toString((float)aSlider.getValue());
                servo.getMeta().setProperty("servo/acceleration", acceleration);

                vaLabel.setText(velocity + "/" + acceleration);
            }
        }

        /**
         * React to checkbox status changes.
         */
        public void itemStateChanged(ItemEvent e) {

            if ( e.getSource() == silentBox ) {

                boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

                /*

                // VT: FIXME: servo silent support has to be implemented first

                try {

                    servo.setSilentMode(selected);

                } catch ( IOException ioex ) {

                    ioex.printStackTrace();
                }
                 */

                // VT: FIXME: Reflect the changes on the silentLabel
            }
        }
    }

    public void deviceArrived(ServoController device) {

        System.err.println("deviceArrived is not implemented by " + getClass().getName());
    }

    public void deviceDeparted(ServoController device) {

        System.err.println("deviceDeparted is not implemented by " + getClass().getName());
    }

    public void exception(Object source, Throwable t) {

        synchronized ( System.err ) {

            System.err.println("Problem with " + Integer.toHexString(source.hashCode()) + ":");
            t.printStackTrace();
        }
    }
}
