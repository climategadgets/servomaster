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
 * @version $Id: ServoView.java,v 1.8 2001-12-29 06:33:19 vtt Exp $
 */
public class ServoView extends JPanel implements ChangeListener, ItemListener, ServoListener {

    /**
     * The servo name.
     */
    private String servoName;
    
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
     * Number of steps the controller can provide for this servo.
     *
     * Default is set to 256, however, this is absolutely not true for most
     * controllers.
     */
    private int precision = 256;
    
    /**
     * Create an instance.
     *
     * @param controller The controller to request the instance from.
     *
     * @param servoName Servo name.
     */
    ServoView(ServoController controller, String servoName) {
    
        this.servoName = servoName;
        
        try {
        
            this.precision = controller.getMetaData().getPrecision();
        
        } catch ( UnsupportedOperationException ex ) {
        
            System.err.println("Controller doesn't provide metadata, precision set to 256");
        }
        
        try {
        
            this.servo = controller.getServo(servoName);
            
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
        
        JLabel servoLabel = new JLabel("ID: " + servoName, JLabel.CENTER);
        servoLabel.setToolTipText("The servo name");
        servoLabel.setBorder(BorderFactory.createEtchedBorder());
        servoLabel.setForeground(Color.black);
        
        layout.setConstraints(servoLabel, cs);
        add(servoLabel);
        
        enableBox = new JCheckBox("Enabled", true);
        enableBox.setToolTipText("Enable or disable this servo");
        enableBox.addItemListener(this);
        
        cs.gridy = 1;
        
        layout.setConstraints(enableBox, cs);
        add(enableBox);

        smoothBox = new JCheckBox("Smooth");
        smoothBox.setToolTipText("Enable or disable the smooth servo movement");
        smoothBox.addItemListener(this);
        
        cs.gridy = 2;
        
        layout.setConstraints(smoothBox, cs);
        add(smoothBox);
        
        cs.gridy = 3;
        
        positionLabel = new JLabel(Integer.toString(precision/2), JLabel.CENTER);
        positionLabel.setToolTipText("Current servo position");
        positionLabel.setBorder(BorderFactory.createTitledBorder("Position"));
        
        layout.setConstraints(positionLabel, cs);
        add(positionLabel);
        
        cs.gridy = 4;
        cs.gridwidth = 1;
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
        controlSlider.setMajorTickSpacing(precision/8);
        controlSlider.setMinorTickSpacing(precision/64);
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
    private void setPosition(double position) {
    
        int iPosition = (int)(position * (precision - 1));
        
        viewSlider.setValue(iPosition);
        positionLabel.setText(Integer.toString(iPosition));
    }
    
    /**
     * React to the slider events.
     */
    public void stateChanged(ChangeEvent e) {
    
        Object source = e.getSource();
        int position;
        
        if ( source == controlSlider ) {
        
            position = controlSlider.getValue();
            
            try {
            
                servo.setPosition(position/255.0, smooth, 0);
                
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
            smoothBox.setEnabled(enabled);
        }
    }
    
    /**
     * Accept the notification about the change in the requested position.
     *
     * @param source The servo whose requested position has changed.
     */
    public void positionChanged(Servo source, double position) {
    
        // This notification doesn't have to be visibly reflected
    
        //System.err.println("Position requested: " + position);
    }
    
    /**
     * Accept the notification about the change in the actual position.
     *
     * @param source The servo whose actual position has changed.
     */
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
}
