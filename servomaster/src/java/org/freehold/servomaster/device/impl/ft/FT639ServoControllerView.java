package org.freehold.servomaster.device.impl.ft;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.view.ServoControllerView;

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
 * <li> Selecting the 90 degree vs. 180 degree pulse length on the
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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: FT639ServoControllerView.java,v 1.1 2001-09-05 05:29:22 vtt Exp $
 */
public class FT639ServoControllerView extends JPanel implements ActionListener, ServoControllerListener, ServoControllerView {

    private FT639ServoController controller;
    
    private ButtonGroup rangeGroup = new ButtonGroup();
    private JLabel modeLabel;
    private JRadioButton range90;
    private JRadioButton range180;
    
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
        
        rangePanel.setLayout(new GridLayout(2,1));

        range90 = new JRadioButton("90\u00B0", true);
        range90.addActionListener(this);
        
        rangeGroup.add(range90);
        rangePanel.add(range90);
        
        range180 = new JRadioButton("180\u00B0");
        range180.addActionListener(this);
        
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

        layout.setConstraints(modeLabel, cs);
        add(modeLabel);
        
        cs.gridx = 2;
        cs.weightx = 1;
        
        JSlider trimSlider = new JSlider(JSlider.HORIZONTAL, 0, 15, 0);
        
        trimSlider.setBorder(BorderFactory.createTitledBorder("Header Length"));
        trimSlider.setMajorTickSpacing(4);
        trimSlider.setMinorTickSpacing(1);
        trimSlider.setPaintTicks(true);
        trimSlider.setPaintLabels(true);
        trimSlider.setSnapToTicks(true);
        
        layout.setConstraints(trimSlider, cs);
        add(trimSlider);
        
    }
    
    public void init(ServoController controller) {
    
        this.controller = (FT639ServoController)controller;
        
        controller.addListener(this);
    }
    
    public void actionPerformed(ActionEvent e) {
    
        try {
        
        if ( e.getSource() == range90 ) {
        
            controller.setRange(false);
        
        } else if ( e.getSource() == range180 ) {
        
            controller.setRange(true);
        }
        
        } catch ( Throwable t ) {
        
            System.err.println("Trying to set range:");
            t.printStackTrace();
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
    
        modeLabel.setText(mode ? "Active" : "Setup");
    }
    
}
