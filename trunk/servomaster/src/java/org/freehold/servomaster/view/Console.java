package org.freehold.servomaster.view;

import java.io.IOException;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.device.impl.ft.FT639ServoController;

/**
 * The console.
 *
 * Allows to control the FT639 controller.
 *
 * <p>
 *
 * VT: FIXME: Actually, this code is pretty generic, and if there's a
 * sequence that instantiates the controller and passes it down to us, this
 * doesn't have to be FT639-specific. It currently is just because the FT639
 * is the first controller in my possession.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Console.java,v 1.2 2001-09-01 06:53:31 vtt Exp $
 */
public class Console implements ServoControllerListener, ActionListener, ItemListener {

    /**
     * The controller to watch and control.
     */
    private ServoController controller;
    
    /**
     * The main Swing frame.
     */
    private JFrame mainFrame;
    
    /**
     * The checkbox responsible for controlling the silent mode.
     */
    private JCheckBox silentBox;
    
    /**
     * The label that shows the silence status of the controller.
     */
    private JLabel silentLabel;
    
    /**
     * Pressing this button will reset the controller.
     *
     * If the controller throws <code>IOException</code> during reset, the
     * application will terminate.
     */
    private JButton resetButton;
    
    /**
     * Set of servos the controller offers.
     */
    private ServoView servoPanel[] = new ServoView[5];
    
    public static void main(String args[]) {
    
        new Console().run(args);
    }
    
    /**
     * Run the view.
     *
     * @param args Command line arguments.
     */
    public void run(String args[]) {
    
        try {
        
            controller = new FT639ServoController("/dev/ttyS1");
            controller.setSilentMode(true);
            
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();
    
            mainFrame = new JFrame("FT639 Console");
            mainFrame.setSize(new Dimension(800, 600));
            
            // VT: FIXME: Have to terminate the application instead.
            // Currently, you have to Ctrl-Break it.
            
            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            mainFrame.getContentPane().setLayout(layout);
            
            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 5;
            cs.weightx = 1;
            
            resetButton = new JButton("Reset Controller");
            resetButton.addActionListener(this);
            
            layout.setConstraints(resetButton, cs);
            
            mainFrame.getContentPane().add(resetButton);
            
            cs.gridy = 1;
            
            silentBox = new JCheckBox("Silent", true);
            silentBox.addItemListener(this);
            
            layout.setConstraints(silentBox, cs);
            mainFrame.getContentPane().add(silentBox);
            
            silentLabel = new JLabel("Controller mode: SETUP");
            
            cs.gridy = 2;
            
            layout.setConstraints(silentLabel, cs);
            mainFrame.getContentPane().add(silentLabel);
            
            controller.addListener(this);
            
            cs.fill = GridBagConstraints.BOTH;
            cs.gridy = 3;
            cs.gridwidth = 1;
            cs.gridheight = 5;
            cs.weighty = 1;
            
            for ( int idx = 0; idx < servoPanel.length; idx++ ) {
            
                servoPanel[idx] = new ServoView(controller, idx);
                
                cs.gridx = idx;
                
                layout.setConstraints(servoPanel[idx], cs);
                
                mainFrame.getContentPane().add(servoPanel[idx]);
            }
            
            //mainFrame.pack();
            
            mainFrame.setVisible(true);
            
            while ( true ) {
            
                Thread.sleep(60000);
            }
        
        } catch ( Throwable t ) {
        
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
    
        silentLabel.setText("Controller mode: " + (mode ? "ACTIVE" : "SETUP"));
    }
    
    /**
     * React to the button presses.
     */
    public void actionPerformed(ActionEvent e) {
    
        if ( e.getSource() == resetButton ) {
        
            try {
            
                controller.reset();
                
            } catch ( IOException ioex ) {
            
                System.err.println("Oops, couldn't reset the controller:");
                
                ioex.printStackTrace();
                
                System.err.println("Controller is considered inoperable, exiting");
                System.exit(1);
            }
        }
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
            
            if ( !selected ) {
            
                silentLabel.setText("Controller mode: CONTINUOUS");
            }
        }
    }
}
