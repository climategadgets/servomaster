package org.freehold.servomaster.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;     
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoListener;

/**
 * The servo view.
 *
 * Displays the servo status and allows to control it.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoView.java,v 1.2 2001-09-01 21:48:24 vtt Exp $
 */
public class ServoView extends JPanel implements ChangeListener, ItemListener, ServoListener {

    /**
     * The servo index.
     *
     * VT: FIXME: has to be replaced with the servo name, or eliminated.
     */
    private int index;
    
    /**
     * The servo to display the status of and control.
     */
    private Servo servo;
    
    /**
     * Checkbox responsible for enabling and disabling the servo.
     */
    private JCheckBox enableBox;
    
    /**
     * Checkbox responsible for enabling and disabling the smooth mode.
     *
     * @see #smooth
     */
    private JCheckBox smoothBox;
    
    /**
     * The label displaying the current trim status.
     */
    private JLabel trimLabel;
    
    /**
     * The slider controlling the trim.
     */
    private JSlider trimSlider;
    
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
     * Smooth mode.
     *
     * <p>
     *
     * Enabled if this is set to <code>true</code>.
     *
     * @see #smoothBox
     */
    private boolean smooth = false;
    
    /**
     * Create an instance.
     *
     * VT: FIXME: Currently, it is FT639 dependent, it doesn't have to be.
     *
     * @param controller The controller to request the instance from.
     *
     * @param index Servo number.
     */
    ServoView(ServoController controller, int index) {
    
        this.index = index;
        
        try {
        
            this.servo = controller.getServo(Integer.toString(index));
            
        } catch ( Throwable t ) {
        
            throw new Error("getServo() failed: " + t.toString());
        } 
    
        setBorder(BorderFactory.createEtchedBorder());
        
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();
        
        setLayout(layout);
        
        cs.fill = GridBagConstraints.HORIZONTAL;
        
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 2;
        
        JLabel servoLabel = new JLabel("ID: " + index, JLabel.CENTER);
        servoLabel.setBorder(BorderFactory.createEtchedBorder());
        servoLabel.setForeground(Color.black);
        
        layout.setConstraints(servoLabel, cs);
        add(servoLabel);
        
        enableBox = new JCheckBox("Enabled", true);
        enableBox.addItemListener(this);
        
        cs.gridy = 1;
        
        layout.setConstraints(enableBox, cs);
        add(enableBox);

        smoothBox = new JCheckBox("Smooth");
        smoothBox.addItemListener(this);
        
        cs.gridy = 2;
        
        layout.setConstraints(smoothBox, cs);
        add(smoothBox);
        
        cs.gridy = 3;
        
        trimLabel = new JLabel("Trim: 0", JLabel.CENTER);
        
        layout.setConstraints(trimLabel, cs);
        add(trimLabel);
        
        cs.gridy = 4;
        
        trimSlider = new JSlider(JSlider.HORIZONTAL, 0, 15, 0);
        trimSlider.addChangeListener(this);
        
        layout.setConstraints(trimSlider, cs);
        add(trimSlider);
        
        cs.gridy = 5;
        
        positionLabel = new JLabel("POS: 128", JLabel.CENTER);
        
        layout.setConstraints(positionLabel, cs);
        add(positionLabel);
        
        cs.gridy = 6;
        cs.gridwidth = 1;
        cs.weighty = 1;
        cs.fill = GridBagConstraints.VERTICAL;
        
        viewSlider = new JSlider(JSlider.VERTICAL, 0, 255, 128);
        viewSlider.setEnabled(false);
        
        layout.setConstraints(viewSlider, cs);
        add(viewSlider);

        cs.gridx = 1;
        
        controlSlider = new JSlider(JSlider.VERTICAL, 0, 255, 128);
        controlSlider.addChangeListener(this);
        controlSlider.setMajorTickSpacing(32);
        controlSlider.setMinorTickSpacing(4);
        controlSlider.setPaintTicks(true);
        controlSlider.setPaintLabels(true);
        controlSlider.setSnapToTicks(false);
        
        layout.setConstraints(controlSlider, cs);
        add(controlSlider);
        
        servo.addListener(this);
    }
    
    /**
     * Reflect the change in actual position.
     */
    private void setPosition(int position) {
    
        viewSlider.setValue(position);
        positionLabel.setText("POS: " + position);
    }
    
    /**
     * React to the slider events.
     */
    public void stateChanged(ChangeEvent e) {
    
        Object source = e.getSource();
        int position;
        
        if ( source == trimSlider ) {
        
            //System.out.println("Trim " + index + ": " + trimSlider.getValue());

            position = trimSlider.getValue();
            
            try {
            
                // VT: FIXME: trim doesn't work
            
                //((FT639ServoController)controller).setTrim(position);
                trimSlider.setValue(position);
                trimLabel.setText("Trim: " + position);
                
            } catch ( Throwable t ) {
            
                System.err.println("ServoController#setTrim:");
                t.printStackTrace();
            }

        } else if ( source == controlSlider ) {
        
            position = controlSlider.getValue();
            
            try {
            
                servo.setPosition(position, smooth, 0);
                
            } catch ( Throwable t ) {
            
                System.err.println("Servo#setPosition:");
                t.printStackTrace();
            }
        }
    }
    
    /**
     * React to the checkbox events.
     */
    public void itemStateChanged(ItemEvent e) {
    
        if ( e.getSource() == smoothBox ) {
        
            smooth = !smooth;
        
        } else if ( e.getSource() == enableBox ) {
        
            enabled = !enabled;
            
            controlSlider.setEnabled(enabled);
            trimSlider.setEnabled(enabled);
            smoothBox.setEnabled(enabled);
        }
    }
    
    /**
     * Accept the notification about the change in the requested position.
     *
     * @param source The servo whose requested position has changed.
     */
    public void positionChanged(Servo source, int position) {
    
        System.err.println("Position requested: " + position);
    }
    
    /**
     * Accept the notification about the change in the actual position.
     *
     * @param source The servo whose actual position has changed.
     */
    public void actualPositionChanged(Servo source, int position) {
    
        viewSlider.setValue(position);
    }
}
