package org.freehold.servomaster.view;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;     
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoListener;

import org.freehold.servomaster.device.model.transform.LinearTransformer;
import org.freehold.servomaster.device.model.transform.Reverser;

import org.freehold.servomaster.device.model.transition.CrawlTransitionController;

/**
 * The servo view.
 *
 * Displays the servo status and allows to control it.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: ServoView.java,v 1.17 2002-01-03 01:52:27 vtt Exp $
 */
public class ServoView extends JPanel implements ActionListener, ChangeListener, ItemListener, ServoListener {

    /**
     * The servo name.
     */
    private String servoName;
    
    /**
     * The servo to display the status of and control.
     */
    private Servo servo;
    
    /**
     * The current target (may be the {@link #servo servo} itself, or the
     * {@link #reverse reversed mapping}, or the {@link #linear linear
     * mapping}.
     */
    private Servo target;
    
    /**
     * The reverse mapping of the {@link #servo servo}.
     */
    private Servo reverse;
    
    /**
     * The linear mapping of the {@link #servo servo}.
     */
    private Servo linear;
    
    /**
     * Checkbox responsible for enabling and disabling the servo.
     */
    private JCheckBox enableBox;
    
    /**
     * Button group for the transition controller selection.
     */
    private ButtonGroup transitionGroup = new ButtonGroup();
    
    /**
     * Panel containing the transition controller selection.
     */
    private JPanel transitionPanel;
    
    /**
     * Radio button for selecting no transition controller (default).
     */
    private JRadioButton transitionNoneBox;
    
    /**
     * Radio button for selecting the crawl transition controller.
     */
    private JRadioButton transitionCrawlBox;
    
    /**
     * Radio button for selecting the linear transition controller.
     */
    private JRadioButton transitionLinearBox;
    
    /**
     * Button group for the mapper selection.
     */
    private ButtonGroup mapperGroup = new ButtonGroup();
    
    /**
     * Panel containing the mapper selection radio buttons.
     */
    private JPanel mapperPanel;
    
    /**
     * Radio button for selecting the normal mapper (default).
     */
    private JRadioButton normalBox;
    
    /**
     * Radio button for selecting the reverse mapper.
     */
    private JRadioButton reverseBox;
    
    /**
     * Radio button for selecting the linear mapper.
     */
    private JRadioButton linearBox;
    
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
        
        this.reverse = new Reverser(this.servo);
        this.linear = new LinearTransformer(this.servo);
        this.target = this.servo;
        
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
        
        transitionPanel = new JPanel();
        
        // VT: FIXME: make it 3,1 when the linear is implemented
        
        transitionPanel.setLayout(new GridLayout(2, 1));
        transitionPanel.setToolTipText("Select the transition controller");
        
        transitionNoneBox = new JRadioButton("Instant", true);
        transitionNoneBox.addActionListener(this);
        transitionNoneBox.setToolTipText("No transition controller, instant movement");
        
        transitionGroup.add(transitionNoneBox);
        transitionPanel.add(transitionNoneBox);

        transitionCrawlBox = new JRadioButton("Crawl");
        transitionCrawlBox.addActionListener(this);
        transitionCrawlBox.setToolTipText("Crawl as fast as I/O and controller allow");
        
        transitionGroup.add(transitionCrawlBox);
        transitionPanel.add(transitionCrawlBox);

        /*
        
        VT: FIXME: linear transition controller is not implemented yet
        
        transitionLinearBox = new JRadioButton("Linear");
        transitionLinearBox.addActionListener(this);
        transitionLinearBox.setToolTipText("Linearly move in 3 seconds");
        
        transitionGroup.add(transitionLinearBox);
        transitionPanel.add(transitionLinearBox);

         */
         
        cs.gridy++;
        
        transitionPanel.setBorder(BorderFactory.createTitledBorder("Transition"));
        layout.setConstraints(transitionPanel, cs);
        add(transitionPanel);
        
        cs.gridy++;
        
        mapperPanel = new JPanel();
        
        mapperPanel.setLayout(new GridLayout(3, 1));
        mapperPanel.setToolTipText("Select the servo mapping");
        
        normalBox = new JRadioButton("Normal", true);
        normalBox.addActionListener(this);
        normalBox.setToolTipText("Servo angular position reflects slider position");
        
        mapperGroup.add(normalBox);
        mapperPanel.add(normalBox);
        
        reverseBox = new JRadioButton("Reversed");
        reverseBox.addActionListener(this);
        reverseBox.setToolTipText("Servo position is reversed");
        
        mapperGroup.add(reverseBox);
        mapperPanel.add(reverseBox);

        linearBox = new JRadioButton("Linear");
        linearBox.addActionListener(this);
        linearBox.setToolTipText("Servo position is linear, not angular");
        
        mapperGroup.add(linearBox);
        mapperPanel.add(linearBox);
        
        mapperPanel.setBorder(BorderFactory.createTitledBorder("Mapping"));
        layout.setConstraints(mapperPanel, cs);
        add(mapperPanel);

        cs.gridy++;
        
        String currentPosition = Integer.toString(precision/2);
        positionLabel = new JLabel(currentPosition + "/" + currentPosition, JLabel.CENTER);
        positionLabel.setToolTipText("Current servo position (actual/requested)");
        positionLabel.setBorder(BorderFactory.createTitledBorder("Position"));
        
        layout.setConstraints(positionLabel, cs);
        add(positionLabel);
        
        cs.gridy++;
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
    
        int iPosition = (int)Math.round(position * (precision - 1));
        
        viewSlider.setValue(iPosition);
        
        double requestedPosition = target.getPosition() * (precision - 1);
        
        positionLabel.setText(Integer.toString(iPosition) + "/" + Math.round(requestedPosition));
    }
    
    /**
     * React to the slider events.
     */
    public void stateChanged(ChangeEvent e) {
    
        Object source = e.getSource();
        
        if ( source == controlSlider ) {
        
            int position = controlSlider.getValue();
            
            try {
            
                target.setPosition((double)position/(double)(precision - 1));
                
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
    
        if ( e.getSource() == enableBox ) {
        
            enabled = !enabled;
            
            controlSlider.setEnabled(enabled);

            transitionNoneBox.setEnabled(enabled);
            transitionCrawlBox.setEnabled(enabled);
            //transitionLinearBox.setEnabled(enabled);

            normalBox.setEnabled(enabled);
            reverseBox.setEnabled(enabled);
            linearBox.setEnabled(enabled);
        }
    }
    
    public void actionPerformed(ActionEvent e) {
    
        try {
        
            Servo current = target;
        
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
            }
            
            if ( current != target ) {
            
                int position = controlSlider.getValue();
            
                target.setPosition((double)position/(double)(precision - 1));
            }
        
        } catch ( Throwable t ) {
        
            System.err.println("Setting mapper:");
            t.printStackTrace();
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
