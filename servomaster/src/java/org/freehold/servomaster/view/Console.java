package org.freehold.servomaster.view;

import java.io.IOException;

import java.util.Vector;

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
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoListener;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.device.impl.ft.FT639ServoController;

/**
 * The console.
 *
 * Allows to control the FT639 controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Console.java,v 1.1 2001-08-31 21:38:00 vtt Exp $
 */
public class Console implements ServoControllerListener, ActionListener, ItemListener {

    private ServoController controller;
    private JFrame mainFrame;
    private JCheckBox silentBox;
    private JLabel silentLabel;
    private JButton resetButton;
    private ServoView servoPanel[] = new ServoView[5];
    
    private Vector eventQueue = new Vector();
    
    public static void main(String args[]) {
    
        new Console().run(args);
    }
    
    public void run(String args[]) {
    
        try {
        
            controller = new FT639ServoController("/dev/ttyS1");
            controller.setSilentMode(true);
            
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();
    
            mainFrame = new JFrame("FT639 Console");
            mainFrame.setSize(new Dimension(800, 600));
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
                
                
                /*
                synchronized ( this ) {
                
                    wait();
                    
                    while ( !eventQueue.isEmpty() ) {
                    
                        ServoEvent e = (ServoEvent)eventQueue.elementAt(0);
                        eventQueue.removeElementAt(0);
                        
                        //servoPanel[e.index].setPosition(e.position);
                    }
                }
                */
            }
        
        } catch ( Throwable t ) {
        
            t.printStackTrace();
        }
    }
    
    public void silentStatusChanged(ServoController controller, boolean mode) {
    
        silentLabel.setText("Controller mode: " + (mode ? "ACTIVE" : "SETUP"));
    }
    
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
    
/*    protected synchronized void enqueue(int index, int position) {
    
        eventQueue.addElement(new ServoEvent(index, position));
    
        notifyAll();
    }
 */   
    protected class ServoView extends JPanel implements ChangeListener, ItemListener, ServoListener {
    
        private int index;
        private Servo servo;
        
        private JCheckBox smoothBox;
        private JLabel trimLabel;
        private JSlider trimSlider;
        private JLabel positionLabel;
        private JSlider viewSlider;
        private JSlider controlSlider;
        
        private boolean smooth = false;
        
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
            
            JLabel servoLabel = new JLabel("#" + index, JLabel.CENTER);
            
            layout.setConstraints(servoLabel, cs);
            add(servoLabel);
            
            smoothBox = new JCheckBox("Smooth");
            smoothBox.addItemListener(this);
            
            cs.gridy = 1;
            
            layout.setConstraints(smoothBox, cs);
            add(smoothBox);
            
            cs.gridy = 2;
            
            trimLabel = new JLabel("Trim: 0", JLabel.CENTER);
            
            layout.setConstraints(trimLabel, cs);
            add(trimLabel);
            
            cs.gridy = 3;
            
            trimSlider = new JSlider(JSlider.HORIZONTAL, 0, 15, 0);
            trimSlider.addChangeListener(this);
            
            layout.setConstraints(trimSlider, cs);
            add(trimSlider);
            
            cs.gridy = 4;
            
            positionLabel = new JLabel("POS: 128", JLabel.CENTER);
            
            layout.setConstraints(positionLabel, cs);
            add(positionLabel);
            
            cs.gridy = 5;
            cs.gridwidth = 1;
            cs.weighty = 1;
            cs.fill = GridBagConstraints.VERTICAL;
            
            viewSlider = new JSlider(JSlider.VERTICAL, 0, 255, 128);
            
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
        
        public void setPosition(int position) {
        
            viewSlider.setValue(position);
            positionLabel.setText("POS: " + position);
        }
        
        public void stateChanged(ChangeEvent e) {
        
            Object source = e.getSource();
            int position;
            
            if ( source == trimSlider ) {
            
                //System.out.println("Trim " + index + ": " + trimSlider.getValue());

                position = trimSlider.getValue();
                
                try {
                
                    ((FT639ServoController)controller).setTrim(position);
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
                    
//                    enqueue(index, position);
                    
                } catch ( Throwable t ) {
                
                    System.err.println("Servo#setPosition:");
                    t.printStackTrace();
                }
            }
        }
        
        public void itemStateChanged(ItemEvent e) {
        
            if ( e.getSource() == smoothBox ) {
            
                smooth = !smooth;
            }
        }
        
        public void positionChanged(Servo source, int position) {
        
            System.err.println("Position requested: " + position);
        }
        
        public void actualPositionChanged(Servo source, int position) {
        
            viewSlider.setValue(position);
        }
    }
    
/*    protected class ServoEvent {
    
        int index;
        int position;
        
        ServoEvent(int index, int position) {
        
            this.index = index;
            this.position = position;
        }
    }
 */
}
