package net.sf.servomaster.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;

/**
 * UI control for adjusting servo range.
 * 
 * Units and initial range are taken from {@link ServoController#getMeta() servo controller metadata}.
 * 
 * This control will not appear on the {@link Console} if {@code servo/range/min} or {@code servo/range/max}
 * property is not available from the controller (NOTE: the controller, not the servo. Read the code for details).
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class ServoRangeControl extends JPanel implements ChangeListener {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    private static final long serialVersionUID = -5085651276509068130L;

    private Servo servo;

    private JLabel rangeLabel;
    private JSlider minSlider;
    private JSlider maxSlider;

    private int defaultRangeMin;
    private int defaultRangeMax;
    private int min;
    private int max;

    public ServoRangeControl(Servo servo) {

        this.servo = servo;

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
        String units;
        
        try {
            
            units = meta.getProperty("servo/range/units").toString();
            
        } catch (UnsupportedOperationException ex) {
            
            units = "ticks";
            
            logger.warn("set range unit to default ('" + units + "')", ex);
        }
        
        // Again, we need the defaults, so taking min/max from the controller,
        // not from the servo

        String min = meta.getProperty("servo/range/min").toString();
        String max = meta.getProperty("servo/range/max").toString();

        defaultRangeMin = Integer.parseInt(min);
        defaultRangeMax = Integer.parseInt(max);

        this.min = defaultRangeMin;
        this.max = defaultRangeMax;

        rangeLabel = new JLabel(renderRange(defaultRangeMin, defaultRangeMax), JLabel.CENTER);
        rangeLabel.setToolTipText("Current minimum/maximum servo position");
        rangeLabel.setBorder(BorderFactory.createTitledBorder("Range, " + units));

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
            rangeLabel.setText(renderRange(min, max));

        } else if ( source == maxSlider ) {

            max = maxSlider.getValue();

            servo.getMeta().setProperty("servo/range/max", Integer.toString(max));
            rangeLabel.setText(renderRange(min, max));
        }
    }

    private String renderRange(int min, int max) {

        StringBuilder sb = new StringBuilder();

        sb.append(Integer.toString(min));
        sb.append(" - ");
        sb.append(Integer.toString(max));

        return sb.toString();
    }
}
