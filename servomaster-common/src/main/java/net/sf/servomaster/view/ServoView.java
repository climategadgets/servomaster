package net.sf.servomaster.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.transform.LimitTransformer;
import net.sf.servomaster.device.model.transform.LinearTransformer;
import net.sf.servomaster.device.model.transform.Reverser;
import net.sf.servomaster.device.model.transition.CrawlTransitionController;

/**
 * The servo view.
 *
 * Displays the servo status and allows to control it.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
@SuppressWarnings("serial")
public class ServoView extends JPanel implements ChangeListener, ServoListener {

    private Logger logger = Logger.getLogger(getClass());

    /**
     * The servo to display the status of and control.
     */
    private final Servo servo;

    /**
     * The current target (may be the {@link #servo servo} itself, or the
     * {@link #reverse reversed mapping}, or the {@link #linear180 linear
     * mapping}.
     */
    private Servo target;

    /**
     * The reverse mapping of the {@link #servo servo}.
     */
    private Servo reverse;

    /**
     * The limit 0.25..0.75 mapping of the {@link #servo servo}.
     */
    private Servo limit;

    /**
     * The linear 0\u00B0-180\u00B0 mapping of the {@link #servo servo}.
     */
    private Servo linear180;

    /**
     * The linear 0\u00B0-90\u00B0 mapping of the {@link #servo servo}.
     */
    private Servo linear90;

    /**
     * The label displaying the current position.
     */
    private JLabel positionLabel;

    /**
     * The slider <strong>displaying</strong> the actual servo position.
     */
    private JSlider viewSlider;

    /**
     * The slider <strong>controlling</strong> the servo position.
     *
     * Sets the <strong>requested</strong> position (may be different from
     * <strong>actual</strong> if the servo is in a smooth mode.
     */
    private JSlider controlSlider;

    /**
     * Enabled status.
     *
     * @see #enableBox
     */
    private boolean enabled = true;

    /**
     * Number of steps the controller can provide for this servo.
     *
     * Note that this value may change as servo range is adjusted.
     */
    private int precision;

    /**
     * Create an instance.
     *
     * @param source Servo to create the view for.
     */
    ServoView(Servo source) {

        this.servo = source;

        // VT: NOTE: For some backwards implementations fallback to controller/precision may be necessary - or implementations need to be fixed
        // VT: FIXME: this has to be dynamic: https://github.com/climategadgets/servomaster/issues/15
        precision = Integer.parseInt((String)servo.getMeta().getProperty("servo/precision"));

        reverse = new Reverser(servo);
        limit = new LimitTransformer(servo, 0.25, 0.75);
        linear180 = new LinearTransformer(servo, 0, 180);
        linear90 = new LinearTransformer(servo, 0, 90);
        target = servo;

        setBorder(BorderFactory.createEtchedBorder());

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();

        setLayout(layout);

        createHeader(layout, cs);
        createSliders(layout, cs);

        servo.addListener(this);
    }

    private void createHeader(GridBagLayout layout, GridBagConstraints cs) {

        cs.fill = GridBagConstraints.HORIZONTAL;

        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 2;

        Header header = new Header();

        layout.setConstraints(header, cs);
        add(header);
    }

    private void createSliders(GridBagLayout layout, GridBagConstraints cs) {

        cs.gridy++;
        cs.gridwidth = 1;
        cs.gridheight = GridBagConstraints.REMAINDER;
        cs.weighty = 1;
        cs.fill = GridBagConstraints.VERTICAL;

        viewSlider = new JSlider(JSlider.VERTICAL, 0, precision - 1, precision/2);
        viewSlider.setToolTipText("The actual position of the servo");
        viewSlider.setEnabled(false);

        layout.setConstraints(viewSlider, cs);
        add(viewSlider);

        cs.gridx = 1;

        controlSlider = new JSlider(JSlider.VERTICAL, 0, precision - 1, precision/2);
        controlSlider.setToolTipText("Move this to make the servo move");
        controlSlider.addChangeListener(this);
        controlSlider.setMajorTickSpacing(precision/4);
        controlSlider.setMinorTickSpacing(precision/32);
        controlSlider.setPaintTicks(true);
        controlSlider.setPaintLabels(true);
        controlSlider.setSnapToTicks(false);

        layout.setConstraints(controlSlider, cs);
        add(controlSlider);
    }

    /**
     * Reflect the change in actual position.
     * 
     * @param position The actual position.
     */
    private void setPosition(double position) {

        int iPosition = (int)Math.round(position * (precision - 1));

        viewSlider.setValue(iPosition);

        double requestedPosition = target.getPosition() * (precision - 1);

        positionLabel.setText(Integer.toString(iPosition) + "/" + Math.round(requestedPosition));
    }

    /**
     * React to the slider events.
     */
    @Override
    public void stateChanged(ChangeEvent e) {

        Object source = e.getSource();

        if ( source == controlSlider ) {

            int position = controlSlider.getValue();

            try {

                target.setPosition((double)position/(double)(precision - 1));

            } catch ( Throwable t ) {

                logger.error("Servo#setPosition:", t);
            }
        }
    }

    /**
     * Accept the notification about the change in the requested position.
     *
     * @param source The servo whose requested position has changed.
     */
    @Override
    public void positionChanged(Servo source, double position) {

        // This notification doesn't have to be visibly reflected

        //logger.debug("Position requested: " + position);
    }

    /**
     * Accept the notification about the change in the actual position.
     *
     * @param source The servo whose actual position has changed.
     */
    @Override
    public void actualPositionChanged(Servo source, double position) {

        setPosition(position);
    }

    /**
     * Hack to return the control slider to the middle position.
     *
     * May be just worth it according to "worse is better".
     */
    void reset() {

        controlSlider.setValue(precision/2);
    }

    public boolean isEnabled() {

        return enabled;
    }

    private class Header extends JPanel implements ItemListener, ActionListener {

        /**
         * Checkbox responsible for enabling and disabling the servo.
         */
        private final JCheckBox enableBox;

        /**
         * Combo box for selecting the transition controller.
         */
        private final JComboBox<String> transitionComboBox;

        /**
         * Combo box for selecting the mapper.
         */
        private final JComboBox<String> mapperComboBox;

        Header() {

            // VT: NOTE: Swing layout management sucked in 2000, and it still sucks in 2018.
            // GridLayout renders fat items and won't change aspect ratio unless you override paintComponent(),
            // and working with GroupLayout is a pain similar or exceeding the one of working with GridBagLayout.

            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();

            setLayout(layout);

            cs.fill = GridBagConstraints.HORIZONTAL;

            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            cs.gridheight = 1;

            JLabel servoLabel = new JLabel("ID: " + servo.getName(), JLabel.CENTER);
            servoLabel.setToolTipText("The servo name");
            servoLabel.setBorder(BorderFactory.createEtchedBorder());
            servoLabel.setForeground(Color.black);

            layout.setConstraints(servoLabel, cs);
            add(servoLabel);

            enableBox = new JCheckBox("Enabled", true);
            enableBox.setToolTipText("Enable or disable this servo");
            enableBox.addItemListener(this);

            cs.gridy++;

            layout.setConstraints(enableBox, cs);
            add(enableBox);

            String[] transition = { "Instant", "Crawl" };

            transitionComboBox = new JComboBox<String>(transition);
            transitionComboBox.setToolTipText("Select the transition controller");
            transitionComboBox.addActionListener(this);

            cs.gridy++;

            layout.setConstraints(transitionComboBox, cs);
            add(transitionComboBox);

            cs.gridy++;

            String[] mapping = { "Direct", "Reverse", "Limit", "Linear 180\u00B0", "Linear 90\u00B0" };

            mapperComboBox = new JComboBox<String>(mapping);
            mapperComboBox.setToolTipText("Select the coordinate transformer");
            mapperComboBox.addActionListener(this);

            layout.setConstraints(mapperComboBox, cs);
            add(mapperComboBox);

            cs.gridy++;
            cs.gridheight = GridBagConstraints.REMAINDER;

            String currentPosition = Integer.toString(precision/2);
            positionLabel = new JLabel(currentPosition + "/" + currentPosition, JLabel.CENTER);
            positionLabel.setToolTipText("Current servo position (actual/requested)");
            positionLabel.setBorder(BorderFactory.createTitledBorder("Position"));

            layout.setConstraints(positionLabel, cs);
            add(positionLabel);
        }

        /**
         * React to the checkbox events.
         */
        @Override
        public void itemStateChanged(ItemEvent e) {

            if ( e.getSource() == enableBox ) {

                enabled = !enabled;

                controlSlider.setEnabled(enabled);

                transitionComboBox.setEnabled(enabled);
                mapperComboBox.setEnabled(enabled);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {

            try {

                Servo current = target;
                Object source = e.getSource();
                JComboBox<String> cb = null;

                if ( source == transitionComboBox ) {

                    cb = (JComboBox<String>)source;

                    switch ( cb.getSelectedIndex() ) {

                    case 0:

                        // No transition controller

                        servo.attach(null);
                        break;

                    case 1:

                        // Crawler

                        servo.attach(new CrawlTransitionController());
                        break;

                    default:

                        logger.error("Unknown transition controller: " + cb.getSelectedItem());
                    }

                } else if ( source == mapperComboBox ) {

                    cb = (JComboBox<String>)source;

                    switch ( cb.getSelectedIndex() ) {

                    case 0:

                        // No coordinate transformation

                        target = servo;
                        break;

                    case 1:

                        // Reverser

                        target = reverse;
                        break;

                    case 2:

                        // Limit 0.25..0.75

                        target = limit;
                        break;

                    case 3:

                        // Linear 0-180

                        target = linear180;
                        break;

                    case 4:

                        // Linear 0-90

                        target = linear90;
                        break;

                    default:

                        logger.error("Unknown coordinate transformer: " + cb.getSelectedItem());
                    }
                }

                /*
                if ( e.getSource() == normalBox ) {

                    target = servo;

                } else if ( e.getSource() == reverseBox ) {

                    target = reverse;

                } else if ( e.getSource() == linearBox ) {

                    target = linear;

                } else if ( e.getSource() == transitionNoneBox ) {

                    servo.attach(null);

                } else if ( e.getSource() == transitionCrawlBox ) {

                    servo.attach(new CrawlTransitionController());
                } */

                if ( current != target ) {

                    int position = controlSlider.getValue();

                    target.setPosition((double)position/(double)(precision - 1));
                }

            } catch ( Throwable t ) {

                logger.error("Setting mapper:", t);
            }
        }
    }
}
