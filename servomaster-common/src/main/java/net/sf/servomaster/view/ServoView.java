package net.sf.servomaster.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.SilentDevice;
import net.sf.servomaster.device.model.transform.LimitTransformer;
import net.sf.servomaster.device.model.transform.LinearTransformer;
import net.sf.servomaster.device.model.transform.Reverser;
import net.sf.servomaster.device.model.transition.CrawlTransitionController;

/**
 * The servo view.
 *
 * Displays the servo status and allows to control it.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
@SuppressWarnings("serial")
public class ServoView extends JPanel {

    private final transient Logger logger = LogManager.getLogger(getClass());

    /**
     * The servo to display the status of and control.
     */
    final transient Servo servo;

    /**
     * The current target (may be the {@link #servo servo} itself, or the
     * {@link #reverse reversed mapping}, or the {@link #linear180 linear
     * mapping}.
     */
    private transient Servo target;

    /**
     * The reverse mapping of the {@link #servo servo}.
     */
    private final transient Servo reverse;

    /**
     * The limit 0.25..0.75 mapping of the {@link #servo servo}.
     */
    private final transient Servo limit;

    /**
     * The linear 0\u00B0-180\u00B0 mapping of the {@link #servo servo}.
     */
    private final transient Servo linear180;

    /**
     * The linear 0\u00B0-90\u00B0 mapping of the {@link #servo servo}.
     */
    private final transient Servo linear90;

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
     * @see Header#enableBox
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
    ServoView(Servo source) throws IOException {

        this.servo = source;

        // VT: NOTE: For some backwards implementations fallback to controller/precision may be necessary - or implementations need to be fixed

        precision = Integer.parseInt((String) servo.getMeta().getProperty("servo/precision"));

        reverse = new Reverser(servo);
        limit = new LimitTransformer(servo, 0.25, 0.75);
        linear180 = new LinearTransformer(servo, 0, 180);
        linear90 = new LinearTransformer(servo, 0, 90);
        target = servo;

        setBorder(BorderFactory.createEtchedBorder());

        var layout = new GridBagLayout();
        var cs = new GridBagConstraints();

        setLayout(layout);

        createHeader(layout, cs);
        createSliders(layout, cs);
        createHandlers(layout, cs);
    }

    private void createHeader(GridBagLayout layout, GridBagConstraints cs) {

        cs.fill = GridBagConstraints.HORIZONTAL;

        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 2;

        var header = new Header();

        layout.setConstraints(header, cs);
        add(header);
    }

    private void createSliders(GridBagLayout layout, GridBagConstraints cs) {

        var sliders = new Sliders();

        servo.addListener(sliders);

        cs.gridy++;

        layout.setConstraints(sliders, cs);
        add(sliders);
    }

    /**
     * All the optional controls defined by {@link Servo#getMeta()} servo metadata are displayed in this section.
     */
    private void createHandlers(GridBagLayout layout, GridBagConstraints cs) throws IOException {

        createRangeAdjuster(layout, cs);
        createSilencerPanel(layout, cs);

        // VT: NOTE: More to follow here
        // ...
    }

    private void createRangeAdjuster(GridBagLayout layout, GridBagConstraints cs) {

        ThreadContext.push("createRangeAdjuster");

        try {

            var src = new ServoRangeControl(this);

            cs.gridy++;

            layout.setConstraints(src, cs);
            add(src);

        } catch (UnsupportedOperationException ex) {

            // No big deal, we'll just don't provide this panel
            logger.info("servo doesn't seem to support range adjustment: {}", ex.getMessage());

        } finally {
            ThreadContext.pop();
        }
    }

    private void createSilencerPanel(GridBagLayout layout, GridBagConstraints cs) throws IOException {

        ThreadContext.push("createSilencerPanel");

        try {

            servo.getMeta().getFeature("servo/silent");

            servo.setSilentMode(true);
            servo.setSilentTimeout(5000, 10000);

            var sp = new SilencerPanel(servo, false);

            cs.gridy++;

            layout.setConstraints(sp, cs);
            add(sp);

        } catch (UnsupportedOperationException ex) {

            // No big deal, we'll just don't provide this panel
            logger.info("servo doesn't seem to support silent operation: {}", ex.getMessage());
            logger.error("Oops", ex);

        } finally {
            ThreadContext.pop();
        }
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
     * Hack to return the control slider to the middle position.
     *
     * May be just worth it according to "worse is better".
     */
    void reset() {

        controlSlider.setValue(precision/2);
    }

    @Override
    public boolean isEnabled() {

        return enabled;
    }

    private class Header extends JPanel implements ItemListener, ActionListener {

        /**
         * Checkbox responsible for enabling and disabling the servo.
         */
        private final JCheckBox enableBox;

        /**
         * Checkbox determining whether the transitions are supposed to be queued.
         */
        private final JCheckBox queueBox;

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

            var layout = new GridBagLayout();
            var cs = new GridBagConstraints();

            setLayout(layout);

            cs.fill = GridBagConstraints.HORIZONTAL;

            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            cs.gridheight = 1;

            var servoLabel = new JLabel("ID: " + servo.getName(), JLabel.CENTER);
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

            queueBox = new JCheckBox("Queue", true);
            queueBox.setToolTipText("Enable or disable transition queuing");
            queueBox.addItemListener(this);

            cs.gridy++;

            layout.setConstraints(queueBox, cs);
            add(queueBox);

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

            var currentPosition = Integer.toString(precision/2);
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

                queueBox.setEnabled(enabled);
                transitionComboBox.setEnabled(enabled);
                mapperComboBox.setEnabled(enabled);

                try {

                    servo.setEnabled(enabled);

                } catch (IOException ex) {

                    // Not much we can do other than complain
                    logger.error("can't change enabled state", ex);
                }
            }

            if (e.getSource() == queueBox) {

                if (transitionComboBox.getSelectedIndex() == 0) {

                    // No action required, there isn't a transition controller attached
                    return;
                }

                logger.debug("queue: {}", (e.getStateChange() == ItemEvent.SELECTED));

                // VT: FIXME: This will only work correctly as long as there's one transition controller choice

                servo.attach(new CrawlTransitionController(), e.getStateChange() == ItemEvent.SELECTED);
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

                        servo.attach(null, false);
                        break;

                    case 1:

                        // Crawler

                        servo.attach(new CrawlTransitionController(), queueBox.isSelected());
                        break;

                    default:

                        logger.error("Unknown transition controller: {}", cb.getSelectedItem());
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

                        logger.error("Unknown coordinate transformer: {}", cb.getSelectedItem());
                    }
                }

                if ( current != target ) {

                    int position = controlSlider.getValue();

                    target.setPosition((double)position/(double)(precision - 1));
                }

            } catch ( Throwable t ) { // NOSONAR Consequences have been considered

                logger.error("Setting mapper:", t);
            }
        }
    }

    private class Sliders extends JPanel implements ChangeListener, ServoListener {

        Sliders() {

            var layout = new GridBagLayout();
            var cs = new GridBagConstraints();

            setLayout(layout);

            cs.gridy++;
            cs.gridwidth = 1;
            cs.gridheight = GridBagConstraints.REMAINDER;
            cs.weighty = 1;
            cs.fill = GridBagConstraints.VERTICAL;

            viewSlider = new JSlider(SwingConstants.VERTICAL, 0, precision - 1, precision/2);
            viewSlider.setToolTipText("The actual position of the servo");
            viewSlider.setEnabled(false);

            layout.setConstraints(viewSlider, cs);
            add(viewSlider);

            cs.gridx = 1;

            controlSlider = new JSlider(SwingConstants.VERTICAL, 0, precision - 1, precision/2);
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
         * React to the slider events.
         */
        @Override
        public void stateChanged(ChangeEvent e) {

            Object source = e.getSource();

            if ( source == controlSlider ) {

                int position = controlSlider.getValue();

                try {

                    target.setPosition((double)position/(double)(precision - 1));

                } catch ( Throwable t ) { // NOSONAR Consequences have been considered

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

            //logger.debug("Position requested: {}", position); // NOSONAR
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

        @Override
        public void exception(Servo source, Throwable t) {
            logger.error("Oops: {}", source, t);
        }

        @Override
        public void silentStatusChanged(SilentDevice source, boolean silent) {
            logger.info("silent status changed to {}: {}", silent, source);
        }
    }

    /**
     * Update the {@link #controlSlider} and {@link #viewSlider} to reflect the
     * current value of {@code servo/precision} servo property.
     */
    public void updateRange() {

        precision = Integer.parseInt((String) servo.getMeta().getProperty("servo/precision"));

        viewSlider.setMaximum(precision - 1);

        controlSlider.setMaximum(precision - 1);
        controlSlider.setMajorTickSpacing(precision/4);
        controlSlider.setMinorTickSpacing(precision/32);
    }
}
