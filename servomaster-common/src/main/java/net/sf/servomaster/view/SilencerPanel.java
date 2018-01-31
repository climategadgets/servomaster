package net.sf.servomaster.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoControllerListener;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.SilentDevice;
import net.sf.servomaster.device.model.SilentDeviceListener;

@SuppressWarnings("serial")
public class SilencerPanel extends JPanel implements SilentDeviceListener, ItemListener, ChangeListener {
    
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The checkbox responsible for controlling the silent mode.
     */
    private JCheckBox silentBox;

    /**
     * The label that shows the silence status of the controller.
     */
    private JLabel silentLabel;

    /**
     * The current timeout label.
     */
    @SuppressWarnings("unused")
    private JLabel timeoutLabel;

    /**
     * The current heartbeat label.
     */
    @SuppressWarnings("unused")
    private JLabel heartbeatLabel;

    /**
     * The silent timeout slider.
     */
    private JSlider timeoutSlider;

    /**
     * The heartbeat slider.
     */
    private JSlider heartbeatSlider;

    /**
     * The device to be silenced.
     */
    private SilentDevice device;

    public SilencerPanel(SilentDevice device) {

        this(device, true);
    }

    public SilencerPanel(SilentDevice device, boolean horizontal) {

        this.device = device;

        // If the controller doesn't support the silent mode and this object
        // is being created, that's not my fault

        silentBox = new JCheckBox("Enable", true);
        silentBox.setToolTipText("Silent mode: stop the servo control pulse after period of inactivity");
        silentBox.addItemListener(this);

        silentLabel = new JLabel("Active");
        silentLabel.setBorder(BorderFactory.createTitledBorder(horizontal ? "Status" : ""));

        timeoutLabel = new JLabel("Timeout", JLabel.CENTER);
        //timeoutLabel.setToolTipText("Silent timeout, seconds");

        heartbeatLabel = new JLabel("Heartbeat", JLabel.CENTER);
        //heartbeatLabel.setToolTipText("Heartbeat timeout, seconds");

        timeoutSlider = new JSlider(horizontal ? JSlider.HORIZONTAL : JSlider.VERTICAL, 5, 30, 5);
        //timeoutSlider.setToolTipText("");
        timeoutSlider.addChangeListener(this);
        timeoutSlider.setMajorTickSpacing(5);
        timeoutSlider.setMinorTickSpacing(1);
        timeoutSlider.setPaintTicks(true);
        timeoutSlider.setPaintLabels(true);
        timeoutSlider.setSnapToTicks(true);
        timeoutSlider.setBorder(BorderFactory.createTitledBorder(horizontal ? "Timeout" : ""));
        timeoutSlider.setToolTipText("Servo goes to silent mode after this many seconds");

        heartbeatSlider = new JSlider(horizontal ? JSlider.HORIZONTAL : JSlider.VERTICAL, 10, 60, 10);
        //heartbeatSlider.setToolTipText("");
        heartbeatSlider.addChangeListener(this);
        heartbeatSlider.setMajorTickSpacing(10);
        heartbeatSlider.setMinorTickSpacing(5);
        heartbeatSlider.setPaintTicks(true);
        heartbeatSlider.setPaintLabels(true);
        heartbeatSlider.setSnapToTicks(true);
        heartbeatSlider.setBorder(BorderFactory.createTitledBorder(horizontal ? "Heartbeat" : ""));
        heartbeatSlider.setToolTipText("Servo will wake up once in this many seconds");

        doLayout(horizontal);

        setBorder(BorderFactory.createTitledBorder("Silent Mode"));

        addListeners(device);
    }

    private void doLayout(boolean horizontal) {

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();

        setLayout(layout);

        if ( horizontal ) {

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.weightx = 0;
            cs.gridwidth = 1;
            cs.gridheight = 1;
            cs.weighty = 0;

            layout.setConstraints(silentBox, cs);
            add(silentBox);

            cs.gridy++;
            cs.weighty = 1;

            layout.setConstraints(silentLabel, cs);
            add(silentLabel);

            cs.gridy = 0;
            cs.gridx++;
            cs.gridheight = 2;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(timeoutSlider, cs);
            add(timeoutSlider);

            cs.gridx++;

            layout.setConstraints(heartbeatSlider, cs);
            add(heartbeatSlider);

        } else {

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.weightx = 0;
            cs.weighty = 0;
            cs.gridwidth = 2;
            cs.gridheight = 1;

            layout.setConstraints(silentBox, cs);
            add(silentBox);

            cs.gridy++;

            layout.setConstraints(silentLabel, cs);
            add(silentLabel);

            cs.gridy++;
            cs.gridwidth = 1;
            cs.weighty = 1;

            layout.setConstraints(timeoutSlider, cs);
            add(timeoutSlider);

            cs.gridx++;

            layout.setConstraints(heartbeatSlider, cs);
            add(heartbeatSlider);
        }
    }

    private void addListeners(SilentDevice device) {

        if (device instanceof ServoController) {

            ((ServoController) device).addListener(new ServoControllerListener() {

                @Override
                public void silentStatusChanged(SilentDevice source, boolean mode) {
                    SilencerPanel.this.silentStatusChanged(source, mode);
                }

                @Override
                public void exception(ServoController source, Throwable t) {
                    // We don't care
                }

                @Override
                public void deviceDeparted(ServoController device) {
                    // We don't care
                }

                @Override
                public void deviceArrived(ServoController device) {
                    // We don't care
                }
            });

        } else if (device instanceof Servo) {

            ((Servo) device).addListener(new ServoListener() {

                @Override
                public void silentStatusChanged(SilentDevice source, boolean mode) {
                    SilencerPanel.this.silentStatusChanged(source, mode);
                }

                @Override
                public void exception(Servo source, Throwable t) {
                    // We don't care
                }

                @Override
                public void positionChanged(Servo source, double position) {
                    // We don't care
                }

                @Override
                public void actualPositionChanged(Servo source, double position) {
                    // We don't care
                }
            });

        } else {
            throw new IllegalStateException("Don't know what to do with " + device.getClass().getName());
        }
    }

    /**
     * React to the notification from the {@link #device controller}
     * about the silent status change.
     *
     * @param source The device which sent the message.
     *
     * @param mode The silent mode if {@code true}.
     */
    @Override
    public void silentStatusChanged(SilentDevice source, boolean mode) {

        silentLabel.setText(mode ? "Active" : "Sleeping");
    }

    /**
     * React to checkbox status changes.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {

        if ( e.getSource() == silentBox ) {

            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

            try {

                device.setSilentMode(selected);

            } catch ( IOException ioex ) {

                logger.warn("Unhandled exception", ioex);
            }

            if ( selected ) {

                timeoutSlider.setEnabled(true);
                heartbeatSlider.setEnabled(true);

            } else {

                silentLabel.setText("ACTIVE");

                timeoutSlider.setEnabled(false);
                heartbeatSlider.setEnabled(false);
            }
        }
    }

    /**
     * React to the slider events.
     */
    @Override
    public void stateChanged(ChangeEvent e) {

        int timeout = timeoutSlider.getValue();
        int heartbeat = heartbeatSlider.getValue();

        device.setSilentTimeout(timeout * 1000, heartbeat * 1000);
    }
}
