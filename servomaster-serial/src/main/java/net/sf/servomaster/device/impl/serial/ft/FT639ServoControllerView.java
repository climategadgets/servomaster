package net.sf.servomaster.device.impl.serial.ft;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.view.ServoControllerView;

/**
 * This class renders and allows to control the features specific to FT639
 * servo controller.
 *
 * <p>
 *
 * The features specific to FT639 are:
 *
 * <ul>
 *
 * <li> Selecting the 90\u00B0 vs. 180\u00B0 pulse length on the
 *      controller level (vs. servo level)
 *
 * <li> Selecting the header length on the controller level (vs. servo
 *      level) - <strong>be careful with it</strong>
 *
 * <li> Distinctly different setup and active mode (this feature is
 *      intentionally left inaccessible)
 *
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 */
@SuppressWarnings("serial")
public class FT639ServoControllerView extends JPanel implements ActionListener, ChangeListener, ServoControllerListener, ServoControllerView {

    private Logger logger = Logger.getLogger(getClass());

    /**
     * The controller to ahem, control ;)
     */
    private FT639ServoController controller;

    /**
     * Label displaying the current status of the controller.
     *
     * Two modes are possible for FT639: setup and active.
     */
    private JLabel modeLabel;

    /**
     * The group including two possible range selection buttons.
     *
     * @see #range90
     * @see #range180
     */
    private ButtonGroup rangeGroup = new ButtonGroup();

    /**
     * 90\u00B0 range selection button.
     */
    private JRadioButton range90;

    /**
     * 180\u00B0 range selection button.
     */
    private JRadioButton range180;

    /**
     * Header length control slider.
     */
    JSlider trimSlider;

    /**
     * Default constructor.
     *
     * Creates the panel with the proper border, title, range selection
     * radio button group, mode display label, and header slider.
     */
    public FT639ServoControllerView() {

        setBorder(BorderFactory.createTitledBorder("FT639 specific controls"));

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();

        setLayout(layout);

        cs.fill = GridBagConstraints.BOTH;
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        cs.gridheight = 1;
        cs.weightx = 0.5;
        cs.weighty = 1;

        JPanel rangePanel = new JPanel();

        rangePanel.setLayout(new GridLayout(2, 1));
        rangePanel.setToolTipText("Select the servo range");

        range90 = new JRadioButton("90\u00B0", true);
        range90.addActionListener(this);
        range90.setToolTipText("Select the servo range");

        rangeGroup.add(range90);
        rangePanel.add(range90);

        range180 = new JRadioButton("180\u00B0");
        range180.addActionListener(this);
        range180.setToolTipText("Select the servo range");

        rangeGroup.add(range180);
        rangePanel.add(range180);

        rangePanel.setBorder(BorderFactory.createTitledBorder("Range"));
        layout.setConstraints(rangePanel, cs);
        add(rangePanel);

        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridheight = 1;

        modeLabel = new JLabel("SETUP", JLabel.CENTER);
        modeLabel.setBorder(BorderFactory.createTitledBorder("Mode"));
        modeLabel.setToolTipText("FT639 controller mode");

        layout.setConstraints(modeLabel, cs);
        add(modeLabel);

        cs.gridx = 2;
        cs.weightx = 1;

        trimSlider = new JSlider(JSlider.HORIZONTAL, 0, 15, 0);
        trimSlider.setToolTipText("Change the FT639 header length (UNDOCUMENTED, be careful)");

        trimSlider.setBorder(BorderFactory.createTitledBorder("Header Length"));
        trimSlider.setMajorTickSpacing(4);
        trimSlider.setMinorTickSpacing(1);
        trimSlider.setPaintTicks(true);
        trimSlider.setPaintLabels(true);
        trimSlider.setSnapToTicks(true);

        layout.setConstraints(trimSlider, cs);
        add(trimSlider);

        trimSlider.addChangeListener(this);

    }

    @Override
    public void init(ServoController controller) {

        if ( controller == null ) {

            throw new IllegalArgumentException("Controller can't be null");
        }

        this.controller = (FT639ServoController)controller;

        controller.addListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {

        if ( e.getSource() == range90 ) {

            controller.setRange(false);

        } else if ( e.getSource() == range180 ) {

            controller.setRange(true);
        }

        } catch ( Throwable t ) {

            logger.error("Trying to set range:", t);
        }
    }

    /**
     * React to the notification from the {@link #controller controller}
     * about the silent status change.
     *
     * @param controller The controller which sent the message.
     *
     * @param mode The silent mode if <code>true</code>.
     */
    @Override
    public void silentStatusChanged(ServoController controller, boolean mode) {

        modeLabel.setText(mode ? "Active" : "Setup");
    }

    @Override
    public void stateChanged(ChangeEvent e) {

        Object source = e.getSource();

        if ( source == trimSlider ) {

            try {

                controller.setHeaderLength(trimSlider.getValue());

            } catch ( Throwable t ) {

                logger.error("Can't set header length, cause:", t);
            }
        }

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
