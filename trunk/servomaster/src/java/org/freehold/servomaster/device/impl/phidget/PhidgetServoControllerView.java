package org.freehold.servomaster.device.impl.phidget;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.view.ServoControllerView;

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
 * <li> Setting the minimum and maximum pulse length for each individual
 *      servo.
 *
 * <li> Setting the silent mode for each individual servo.
 *
 * <li> Enabling or disabling disconnected operation (when the actual USB
 *      device is not plugged in).
 *
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: PhidgetServoControllerView.java,v 1.3 2002-03-12 07:07:00 vtt Exp $
 */
public class PhidgetServoControllerView extends JPanel implements ServoControllerListener, ServoControllerView {

    private PhidgetServoController controller;
    
    public PhidgetServoControllerView() {
    
    }
    
    public void init(ServoController controller) {
    
        this.controller = (PhidgetServoController)controller;
        
        controller.addListener(this);
        
        LinkedList servos = new LinkedList();
        
        try {

            for ( Iterator i = controller.getServos(); i.hasNext(); ) {
            
                servos.add(i.next());
            }
            
            setLayout(new GridLayout(1, servos.size()));
            
            for ( Iterator i = servos.iterator(); i.hasNext(); ) {
            
                add(new ServoPanel((PhidgetServoController.PhidgetServo)i.next()));
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
    
    private class ServoPanel extends JPanel implements ChangeListener, ItemListener {
    
        private PhidgetServoController.PhidgetServo servo;
        private JCheckBox silentBox;
        private JLabel silentLabel;
        private JLabel rangeLabel;
        private JSlider minSlider;
        private JSlider maxSlider;
        
        ServoPanel(PhidgetServoController.PhidgetServo servo) {
        
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
            
            /*
            silentBox = new JCheckBox("Silent");
            silentBox.setToolTipText("Silent mode: stop the servo control pulse after period of inactivity");
            
            // VT: FIXME
            silentBox.setEnabled(false);
            
            layout.setConstraints(silentBox, cs);
            add(silentBox);
            
            cs.gridy++;
            
            silentLabel = new JLabel("ACTIVE", JLabel.CENTER);
            silentLabel.setToolTipText("Current servo status");
            
            // VT: FIXME
            silentLabel.setEnabled(false);
            
            layout.setConstraints(silentLabel, cs);
            add(silentLabel);
            
            cs.gridy++;
            
            */
            rangeLabel = new JLabel("1000us/2000us");
            rangeLabel.setToolTipText("Current minimum/maximum servo pulse length");
            rangeLabel.setBorder(BorderFactory.createTitledBorder("Range"));
            
            layout.setConstraints(rangeLabel, cs);
            add(rangeLabel);
            
            cs.gridy++;
            cs.gridwidth = 1;
            cs.gridheight = 2;
            
            cs.fill = GridBagConstraints.BOTH;
            cs.weighty = 1;
            
            minSlider = new JSlider(JSlider.VERTICAL, 150, 1400, 1000);
            minSlider.setToolTipText("Move this to set the minimum servo pulse length");
            minSlider.setMajorTickSpacing(250);
            minSlider.setMinorTickSpacing(50);
            minSlider.setPaintTicks(true);
            minSlider.setPaintLabels(true);
            minSlider.setSnapToTicks(false);
            
            layout.setConstraints(minSlider, cs);
            add(minSlider);
            
            cs.gridx = 1;
            
            maxSlider = new JSlider(JSlider.VERTICAL, 1600, 2500, 2000);
            maxSlider.setToolTipText("Move this to set the maximum servo pulse length");
            maxSlider.setMajorTickSpacing(250);
            maxSlider.setMinorTickSpacing(50);
            maxSlider.setPaintTicks(true);
            maxSlider.setPaintLabels(true);
            maxSlider.setSnapToTicks(false);
            
            layout.setConstraints(maxSlider, cs);
            add(maxSlider);
            
            minSlider.addChangeListener(this);
            maxSlider.addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
        
            Object source = e.getSource();
            
            int min = minSlider.getValue();
            int max = maxSlider.getValue();
            
            rangeLabel.setText(Integer.toString(min) + "us/" + max + "us");
            
            try {
            
                servo.setRange(min, max);
                
            } catch ( IOException ioex ) {
            
                ioex.printStackTrace();
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
