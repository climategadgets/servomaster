package org.freehold.servomaster.view;

import java.io.IOException;
import java.util.Iterator;
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
import javax.swing.WindowConstants;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;

/**
 * The console.
 *
 * Allows to control the servo controller.
 *
 * <p>
 *
 * Usage:
 *
 * <blockquote>
 *
 * <code>java -classpath ${CLASSPATH} org.freehold.servomaster.view.Console <i>&lt;controller class name&gt; &lt;controller port name&gt;</i></code>
 *
 * </blockquote>
 *
 * Works like this:
 *
 * <ol>
 *
 * <li> Instantiate the controller class.
 *
 * <li> {@link org.freehold.servomaster.device.model.ServoController#init
 *      Initialize} the controller with the port name.
 *
 * <li> {@link
 *      org.freehold.servomaster.device.model.ServoController#getServos Get
 *      the servos} from the controller.
 *
 * <li> Create the {@link ServoView servo views} and stuff them into the
 *      console.
 *
 * <li> Enjoy.
 *
 * </ol>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: Console.java,v 1.9 2002-01-02 03:51:13 vtt Exp $
 */
public class Console implements ServoControllerListener, ActionListener, ItemListener {

    /**
     * The controller to watch and control.
     */
    private ServoController controller;
    
    /**
     * The controller port name.
     */
    private String portName;
    
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
     *
     * <p>
     *
     * VT: FIXME: Someday have to fix this and make it a Vector or something
     * like that. Until I do, ArrayIndexOutOfBounds is looming.
     */
    private ServoView servoPanel[] = new ServoView[50];
    
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
        
            if ( args.length < 2 ) {
            
                System.err.println("Usage: ft_view <servo controller class name> <servo controller port name>");
                System.exit(1);
            }
        
            try {
            
                Class controllerClass = Class.forName(args[0]);
                Object controllerObject = controllerClass.newInstance();
                controller = (ServoController)controllerObject;
                
                controller.init(args[1]);
                
                portName = args[1];
                
            } catch ( Throwable t ) {
            
                System.err.println("Unable to initialize controller, cause:");
                t.printStackTrace();
                
                System.exit(1);
            }
            
            controller.setSilentMode(true);
            
            // Figure out how many servos does the controller currently have
            
            Vector servoSet = new Vector();
            
            for ( Iterator i = controller.getServos(); i.hasNext(); ) {
            
                servoSet.add(i.next());
            }
            
            int servoCount = servoSet.size();
            
            if ( servoCount == 0 ) {
            
                System.err.println("The controller doesn't seem to have any servos now");
                System.exit(1);
            }
            
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();
    
            mainFrame = new JFrame("Servo Controller Console, port " + portName);
            mainFrame.setSize(new Dimension(800, 600));
            
            // VT: FIXME: Have to terminate the application instead.
            // Currently, you have to Ctrl-Break it.
            
            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            mainFrame.getContentPane().setLayout(layout);
            
            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = servoCount;
            cs.weightx = 1;
            
            
            silentBox = new JCheckBox("Silent", true);
            silentBox.setToolTipText("Silent mode: stop the servo control pulse after period of inactivity");
            silentBox.addItemListener(this);
            
            layout.setConstraints(silentBox, cs);
            mainFrame.getContentPane().add(silentBox);
            
            silentLabel = new JLabel("Controller mode: SETUP");
            silentLabel.setToolTipText("Current status of the controller in regard to silent mode");
            
            cs.gridy++;
            
            layout.setConstraints(silentLabel, cs);
            mainFrame.getContentPane().add(silentLabel);
            
            controller.addListener(this);
            
            cs.fill = GridBagConstraints.BOTH;
            cs.gridy++;
            cs.gridwidth = 1;
            cs.gridheight = servoCount;
            cs.weighty = 1;
            
            for ( int idx = 0; idx < servoCount; idx++ ) {
            
                servoPanel[idx] = new ServoView(controller, ((Servo)servoSet.elementAt(idx)).getName());
                
                cs.gridx = idx;
                
                layout.setConstraints(servoPanel[idx], cs);
                
                mainFrame.getContentPane().add(servoPanel[idx]);
            }
            
            cs.gridx = 0;
            cs.gridy++;
            cs.gridy += servoCount;
            cs.gridwidth = servoCount;
            cs.gridheight = 1;
            cs.weightx = 1;
            cs.weighty = 0;
            cs.fill = GridBagConstraints.HORIZONTAL;
            
            String controllerClassName = controller.getClass().getName();
            
            // The controller panel class name is the controller class name
            // with "View" appended to it
            
            String controllerViewClassName = controllerClassName + "View";

            try {
            
                Class controllerViewClass = Class.forName(controllerViewClassName);
                Object controllerViewObject = controllerViewClass.newInstance();
                
                if ( !(controllerViewObject instanceof JPanel) ) {
                
                    throw new IllegalAccessException("The servo controller view class has to extend javax.swing.JPanel, it doesn't");
                }
                
                if ( !(controllerViewObject instanceof ServoControllerView) ) {
                
                    throw new IllegalAccessException("The servo controller view class has to implement org.freehold.servomaster.view.ServoControllerView, it doesn't");
                }
                
                JPanel controllerPanel = (JPanel)controllerViewObject;
                
                ((ServoControllerView)controllerPanel).init(controller);
                
                layout.setConstraints(controllerPanel, cs);
                mainFrame.getContentPane().add(controllerPanel);
                
                cs.gridy++;
                
            } catch ( Throwable t ) {
            
                System.err.println("Couldn't instantiate the servo controller view ("
                	+ controllerViewClassName
                	+ ", so it will not be available. Cause:");
                t.printStackTrace();
            }
            
            // If the controller view has instantiated, the constraint Y
            // coordinate has been advanced. If not, we didn't need it
            // anyway
            
            JPanel buttonContainer = new JPanel();
            
            GridBagLayout bcLayout = new GridBagLayout();
            GridBagConstraints bcCs = new GridBagConstraints();
            
            buttonContainer.setLayout(bcLayout);
            
            bcCs.fill = GridBagConstraints.BOTH;
            bcCs.gridx = 0;
            bcCs.weightx = 1.0;
            bcCs.weighty = 1.0;
            
            resetButton = new JButton("Reset Controller");
            resetButton.setToolTipText("Reset controller, swing to the left, swing to the right, center");
            resetButton.addActionListener(this);
            
            bcLayout.setConstraints(resetButton, bcCs);
            buttonContainer.add(resetButton);
            
            bcCs.gridx++;
            
            JButton swingDemoButton = new JButton("Swing Demo");
            
            bcLayout.setConstraints(swingDemoButton, bcCs);
            buttonContainer.add(swingDemoButton);

            bcCs.gridx++;
            
            JButton clockDemoButton = new JButton("Clock Demo");
            
            bcLayout.setConstraints(clockDemoButton, bcCs);
            buttonContainer.add(clockDemoButton);
            
            // VT: FIXME: Enable them when the demo code is ready
            
            swingDemoButton.setEnabled(false);
            clockDemoButton.setEnabled(false);
            
            layout.setConstraints(buttonContainer, cs);
            mainFrame.getContentPane().add(buttonContainer);
            
            //mainFrame.getContentPane().invalidate();
            mainFrame.pack();
            
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
    
        silentLabel.setText("Controller mode: " + (mode ? "ACTIVE" : "Sleeping"));
    }
    
    /**
     * React to the button presses.
     */
    public void actionPerformed(ActionEvent e) {
    
        if ( e.getSource() == resetButton ) {
        
            try {
                
                controller.reset();
                
                // VT: FIXME: Remove the transition controller, if any, and
                // reattach it later
                
                for ( Iterator i = controller.getServos(); i.hasNext(); ) {
                
                    ((Servo)i.next()).setPosition(0);
                }
                
                Thread.sleep(1000);
                
                for ( Iterator i = controller.getServos(); i.hasNext(); ) {
                
                    ((Servo)i.next()).setPosition(1);
                }
                
                Thread.sleep(1000);
                
                for ( Iterator i = controller.getServos(); i.hasNext(); ) {
                
                    ((Servo)i.next()).setPosition(0.5);
                }
                
                for ( int idx = 0; servoPanel[idx] != null; idx++ ) {
                
                    servoPanel[idx].reset();
                }

            } catch ( Throwable t ) {
            
                System.err.println("Oops, couldn't reset the controller:");
                
                t.printStackTrace();
                
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
