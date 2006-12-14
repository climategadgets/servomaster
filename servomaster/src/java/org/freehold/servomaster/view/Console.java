package org.freehold.servomaster.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.TransitionController;

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
 * <li> {@link ServoController#init Initialize} the controller with the port name.
 *
 * <li> {@link ServoController#getServos Get the servos} from the controller.
 *
 * <li> Create the {@link ServoView servo views} and stuff them into the
 *      console.
 *
 * <li> Enjoy.
 *
 * </ol>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: Console.java,v 1.21 2006-12-14 09:17:11 vtt Exp $
 */
public class Console implements ActionListener, WindowListener {

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
     * The controller silent status display.
     */
    private SilencerPanel silencerPanel;

    /**
     * Pressing this button will reset the controller.
     *
     * If the controller throws <code>IOException</code> during reset, the
     * application will terminate.
     */
    private JButton resetButton;

    /**
     * Pressing this button will start the swing demo.
     */
    private JButton swingDemoButton;

    /**
     * Pressing this button will start the clock demo.
     */
    private JButton clockDemoButton;

    /**
     * Pressing this button will start the wave demo.
     */
    private JButton waveDemoButton;

    /**
     * Currently running demo thread.
     */
    private Thread demo;

    /**
     * Set of servos the controller offers.
     *
     * <p>
     *
     * VT: FIXME: Someday have to fix this and make it a Vector or something
     * like that. Until I do, ArrayIndexOutOfBounds is looming.
     */
    private ServoView[] servoPanel = new ServoView[50];

    public static void main(String[] args) {

        new Console().run(args);
    }

    /**
     * Run the view.
     *
     * @param args Command line arguments.
     */
    public void run(String[] args) {

        try {

            if ( args.length < 1 ) {

                System.err.println("Usage: <script> <servo controller class name> [<servo controller port name>]");
                System.err.println("");
                System.err.println("Example: console org.freehold.servomaster.device.impl.ft.FT639ServoController /dev/ttyS0");
                System.err.println("Example: java -jar servomaster.jar org.freehold.servomaster.device.impl.phidget.PhidgetServoController");
                System.exit(1);
            }

            try {

                Class controllerClass = Class.forName(args[0]);
                Object controllerObject = controllerClass.newInstance();
                controller = (ServoController)controllerObject;

                if ( args.length == 2 ) {

                    portName = args[1];
                }

                controller.init(portName);

                // If the original port name wasn't specified, it is defined
                // in the controller by now

                portName = controller.getPort();

            } catch ( Throwable t ) {

                System.err.println("Unable to initialize controller, cause:");
                t.printStackTrace();

                System.exit(1);
            }

            // Let's see if they support metadata

            Meta controllerMeta = null;

            try {

                controllerMeta = controller.getMeta();

                System.out.println("Features:");

                for ( Iterator<String> cif = controllerMeta.getFeatures(); cif.hasNext(); ) {

                    String key = cif.next();

                    System.out.println("    " + key + ": " + controllerMeta.getFeature(key));
                }

                System.out.println("Properties:");

                for ( Iterator<String> cip = controllerMeta.getProperties(); cip.hasNext(); ) {

                    String key = cip.next();

                    System.out.println("    " + key + ": " + controllerMeta.getProperty(key));
                }

                try {

                    if ( controllerMeta.getFeature("controller/silent") ) {

                        controller.setSilentMode(true);
                        controller.setSilentTimeout(10000, 30000);
                        silencerPanel = new SilencerPanel(controller);
                    }

                } catch ( UnsupportedOperationException ex ) {

                    System.out.println("Controller doesn't support servo shutoff (" + ex.getMessage() + ")");
                }


            } catch ( UnsupportedOperationException ex ) {

                System.err.println("Controller doesn't support metadata");
                ex.printStackTrace();

            } catch ( IllegalStateException isex ) {

                System.err.println("Controller is not yet connected?");
                isex.printStackTrace();
                System.err.println("");

                throw new IllegalStateException("Can't get controller metadata, can't build the console, good bye");
            }

            // Figure out how many servos does the controller currently have

            List<Servo> servoSet = new LinkedList<Servo>();

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

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

            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            mainFrame.addWindowListener(this);

            JPanel console = new JPanel();
            JScrollPane scroller = new JScrollPane(console);

            mainFrame.setContentPane(scroller);

            console.setLayout(layout);


            cs.fill = GridBagConstraints.BOTH;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            cs.gridheight = servoCount;
            cs.weightx = 1;
            cs.weighty = 1;

            for ( int idx = 0; idx < servoCount; idx++ ) {

                servoPanel[idx] = new ServoView(controller, servoSet.get(idx).getName());

                cs.gridx = idx;

                layout.setConstraints(servoPanel[idx], cs);

                console.add(servoPanel[idx]);
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
                console.add(controllerPanel);

                cs.gridy++;

            } catch ( Throwable t ) {

                System.err.println("Couldn't instantiate the servo controller view ("
                	+ controllerViewClassName
                	+ ", so it will not be available. Cause:");
                t.printStackTrace();
            }

            // If the controller view has been instantiated, the constraint
            // Y coordinate has been advanced. If not, we didn't need it
            // anyway

            if ( silencerPanel != null ) {

                cs.fill = GridBagConstraints.HORIZONTAL;
                cs.gridx = 0;
                cs.gridy++;
                cs.gridwidth = servoCount;
                cs.gridheight = 1;
                cs.weightx = 1;
                cs.weighty = 0;

                layout.setConstraints(silencerPanel, cs);
                console.add(silencerPanel);

                cs.gridy++;
            }

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

            swingDemoButton = new JButton("Swing Demo");
            swingDemoButton.addActionListener(this);

            bcLayout.setConstraints(swingDemoButton, bcCs);
            buttonContainer.add(swingDemoButton);

            bcCs.gridx++;

            clockDemoButton = new JButton("Clock Demo");
            clockDemoButton.addActionListener(this);

            bcLayout.setConstraints(clockDemoButton, bcCs);
            buttonContainer.add(clockDemoButton);

            bcCs.gridx++;

            waveDemoButton = new JButton("Wave Demo");
            waveDemoButton.addActionListener(this);

            bcLayout.setConstraints(waveDemoButton, bcCs);
            buttonContainer.add(waveDemoButton);

            // VT: FIXME: Enable them when the demo code is ready

            waveDemoButton.setEnabled(false);

            layout.setConstraints(buttonContainer, cs);
            console.add(buttonContainer);

            //console.invalidate();
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
     * React to the button presses.
     */
    public void actionPerformed(ActionEvent e) {

        if ( demo != null ) {

            demo.interrupt();
            return;
        }

        if ( e.getSource() == resetButton ) {

            new Thread(new reset()).start();

        } else if ( e.getSource() == swingDemoButton ) {

            demo = new Thread(new swing());
            demo.start();

        } else if ( e.getSource() == clockDemoButton ) {

            demo = new Thread(new clock());
            demo.start();

        } else if ( e.getSource() == waveDemoButton ) {

            demo = new Thread(new swing());
            demo.start();
        }
    }

    // WindowListener methods

    public void windowOpened(WindowEvent e) {

    }

    public void windowClosing(WindowEvent e) {

        System.exit(0);
    }


    public void windowClosed(WindowEvent e) {

    }

    public void windowIconified(WindowEvent e) {

    }

    public void windowDeiconified(WindowEvent e) {

    }

    public void windowActivated(WindowEvent e) {

    }

    public void windowDeactivated(WindowEvent e) {

    }

    protected abstract class exec implements Runnable {

        public void run() {

            try {

                swingDemoButton.setEnabled(false);
                clockDemoButton.setEnabled(false);
                waveDemoButton.setEnabled(false);

                controller.reset();

                Map<Servo, TransitionController> trans = new HashMap<Servo, TransitionController>();
                Map<Servo, Double> position = new HashMap<Servo, Double>();

                for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                    Servo s = i.next();

                    trans.put(s, s.getTransitionController());
                    position.put(s, new Double(s.getPosition()));

                    s.attach(null);
                }

                prepare();
                execute();

                for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                    Servo s = i.next();

                    s.attach(trans.get(s));

                    s.setPosition((position.get(s)).doubleValue());
                }
            } catch (InterruptedException iex) {

                // No big deal
                return;

            } catch ( Throwable t ) {

                System.err.println("Oops, controller operation failed:");

                t.printStackTrace();

                System.err.println("Controller is considered inoperable, exiting");
                System.exit(1);

            } finally {

                try {

                    cleanup();

                } catch ( Throwable t ) {

                    System.err.println("Problem trying to clean up:");
                    t.printStackTrace();
                }

                demo = null;

                swingDemoButton.setEnabled(true);
                clockDemoButton.setEnabled(true);

                // VT: FIXME

                waveDemoButton.setEnabled(false);
            }
        }

        protected abstract void prepare() throws Throwable;
        protected abstract void execute() throws Throwable;
        protected abstract void cleanup() throws Throwable;
    }

    protected class reset extends exec {

        @Override
        protected void prepare() {

            resetButton.setEnabled(false);
        }

        @Override
        protected void execute() throws Throwable {

                for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                    i.next().setPosition(0);
                }

                Thread.sleep(1000);

                for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                    i.next().setPosition(1);
                }

                Thread.sleep(1000);

                for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                    i.next().setPosition(0.5);
                }

                for ( int idx = 0; servoPanel[idx] != null; idx++ ) {

                    servoPanel[idx].reset();
                }
        }

        @Override
        protected void cleanup() {

            resetButton.setEnabled(true);
        }
    }

    protected class demo extends exec {

        @Override
        protected final void prepare() {

            resetButton.setText("Stop Demo");
        }

        @Override
        protected void execute() throws Throwable {

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                i.next().setPosition(0);
            }

            Thread.sleep(1000);

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                i.next().setPosition(1);
            }

            Thread.sleep(1000);

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                i.next().setPosition(0.5);
            }

            for ( int idx = 0; servoPanel[idx] != null; idx++ ) {

                servoPanel[idx].reset();
            }
        }

        @Override
        protected final void cleanup() throws Throwable {

            resetButton.setText("Reset Controller");

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                i.next().setPosition(0.5);
            }
        }
    }

    protected class swing extends demo {

        @Override
        protected void execute() throws Throwable {

            List<Servo> servos = new LinkedList<Servo>();

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                Servo s = i.next();

                servos.add(s);

                s.setPosition(0);
            }

            Thread.sleep(1000);

            int max = servos.size();

            // VT: NOTE: Bold assumption: controller contains more than one
            // servo

            int current = 0;
            int trailer = ((current - 1) + max) % max;

            while ( true ) {

                Servo currentServo = servos.get(current);
                Servo trailerServo = servos.get(trailer);

                currentServo.setPosition(1);
                trailerServo.setPosition(0);

                current = (current + 1) % max;
                trailer = ((current - 1) + max) % max;

                Thread.sleep(800);
            }
        }
    }

    /**
     * A clock.
     *
     * Servo 0 is the second hand, servo 1 is the minute hand, servo 2 is
     * the hour in 24 hour mode.
     */
    protected class clock extends demo {

        @Override
        protected void execute() throws Throwable {

            List<Servo> servos = new LinkedList<Servo>();

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                Servo s = i.next();

                servos.add(s);

                s.setPosition(0);
            }

            // Bold assumption: the controller supports at least 3 servos

            Servo servoSecond = servos.get(0);
            Servo servoMinute = servos.get(1);
            Servo servoHour = servos.get(2);

            Thread.sleep(1000);

            while ( true ) {

                Calendar c = new GregorianCalendar();

                int seconds = c.get(Calendar.SECOND);
                int minutes = c.get(Calendar.MINUTE);
                int hours   = c.get(Calendar.HOUR_OF_DAY);

                servoSecond.setPosition((double)seconds/(double)60);
                servoMinute.setPosition((double)minutes/(double)60);
                servoHour.setPosition((double)hours/(double)24);

                Thread.sleep(50);
            }
        }
    }
}
