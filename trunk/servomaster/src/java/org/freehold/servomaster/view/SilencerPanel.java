package org.freehold.servomaster.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.io.IOException;

import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;

public class SilencerPanel extends JPanel implements ServoControllerListener, ItemListener, ChangeListener {

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
    private JLabel timeoutLabel;
    
    /**
     * The current heartbeat label.
     */
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
     * The controller.
     */
    private ServoController controller;
    
    public SilencerPanel(ServoController controller) {
    
        this(controller, true);
    }
    
    public SilencerPanel(ServoController controller, boolean horizontal) {
    
        this.controller = controller;
        
        // If the controller doesn't support the silent mode and this object
        // is being created, that's not my fault
        
        silentBox = new JCheckBox("Enable", true);
        silentBox.setToolTipText("Silent mode: stop the servo control pulse after period of inactivity");
        silentBox.addItemListener(this);
        
        silentLabel = new JLabel("Active");
        silentLabel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        timeoutLabel = new JLabel("Timeout", JLabel.CENTER);
        //timeoutLabel.setToolTipText("Silent timeout, seconds");
        
        heartbeatLabel = new JLabel("Heartbeat", JLabel.CENTER);
        //heartbeatLabel.setToolTipText("Heartbeat timeout, seconds");
        
        timeoutSlider = new JSlider(horizontal ? JSlider.HORIZONTAL : JSlider.VERTICAL, 5, 30, 10);
        //timeoutSlider.setToolTipText("");
        timeoutSlider.addChangeListener(this);
        timeoutSlider.setMajorTickSpacing(5);
        timeoutSlider.setMinorTickSpacing(1);
        timeoutSlider.setPaintTicks(true);
        timeoutSlider.setPaintLabels(true);
        timeoutSlider.setSnapToTicks(true);
        timeoutSlider.setBorder(BorderFactory.createTitledBorder("Timeout: 10"));

        heartbeatSlider = new JSlider(horizontal ? JSlider.HORIZONTAL : JSlider.VERTICAL, 30, 90, 30);
        //heartbeatSlider.setToolTipText("");
        heartbeatSlider.addChangeListener(this);
        heartbeatSlider.setMajorTickSpacing(10);
        heartbeatSlider.setMinorTickSpacing(5);
        heartbeatSlider.setPaintTicks(true);
        heartbeatSlider.setPaintLabels(true);
        heartbeatSlider.setSnapToTicks(true);
        heartbeatSlider.setBorder(BorderFactory.createTitledBorder("Heartbeat: 30"));
        
        doLayout(horizontal);

        controller.addListener(this);

        setBorder(BorderFactory.createTitledBorder("Silent Mode"));
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
        
            throw new Error("Not Implemented");
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
    public void silentStatusChanged(ServoController controller, boolean mode) {
    
        silentLabel.setText(mode ? "Active" : "Sleeping");
    }
    
    public void deviceArrived(ServoController device) {
    
        System.err.println("deviceArrived is not implemented by " + getClass().getName());
    }

    public void deviceDeparted(ServoController device) {
    
        System.err.println("deviceDeparted is not implemented by " + getClass().getName());
    }
    
    /**
     * React to checkbox status changes.
     */
    public void itemStateChanged(ItemEvent e) {
    
        if ( e.getSource() == silentBox ) {
        
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        
            try {
            
                controller.setSilentMode(selected);
                
            } catch ( IOException ioex ) {
            
                ioex.printStackTrace();
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
    public void stateChanged(ChangeEvent e) {
    
        int timeout = timeoutSlider.getValue();
        int heartbeat = heartbeatSlider.getValue();
        
        controller.setSilentTimeout(timeout * 1000, heartbeat * 1000);

        timeoutSlider.setBorder(BorderFactory.createTitledBorder("Timeout: " + timeout));
        heartbeatSlider.setBorder(BorderFactory.createTitledBorder("Heartbeat: " + heartbeat));
    }
}
