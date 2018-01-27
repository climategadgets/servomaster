package net.sf.servomaster.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.servomaster.device.impl.debug.NullServoController;
import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.TransitionController;

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
 * <code>java -classpath ${CLASSPATH} net.sf.servomaster.view.Console <i>&lt;controller class name&gt; &lt;controller port name&gt;</i></code>
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
 */
public class Console implements ActionListener, WindowListener {

    private Logger logger = Logger.getLogger(getClass());
    
    /**
     * The controller to watch and control.
     */
    private ServoController controller;

    /**
     * The main Swing frame.
     */
    private JFrame mainFrame;

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

        NDC.push("run");

        try {

            controller = instantiate(resolveClass(args));

            controller.init(args.length == 2 ? args[1] : null);

            displayMetadata(controller);
            buildConsole(controller);

            // VT: FIXME: Replace this by waiting for the semaphore, make window.close()
            // (and other catastrophic things) trigger the semaphore, then park the servos right here

            while ( true ) {

                Thread.sleep(60000);
            }

        } catch ( Throwable t ) {

            logger.warn("Unhandled exception", t);

        } finally {

            logger.warn("FIXME: park the servos");
            NDC.pop();
        }
    }

    private void displayMetadata(ServoController controller2) {

        NDC.push("medatada");

        try {

            Meta controllerMeta = controller.getMeta();

            logger.info("Features:");

            for ( Iterator<Entry<String, Boolean>> i = controllerMeta.getFeatures().entrySet().iterator(); i.hasNext(); ) {

                Entry<String, Boolean> entry = i.next();

                logger.info("    " + entry.getKey() + ": " + entry.getValue());
            }

            logger.info("Properties:");

            for ( Iterator<Entry<String, Object>> i = controllerMeta.getProperties().entrySet().iterator(); i.hasNext(); ) {

                Entry<String, Object> entry = i.next();

                logger.info("    " + entry.getKey() + ": " + entry.getValue());
            }

        } catch ( UnsupportedOperationException ex ) {

            logger.info("Controller doesn't support metadata", ex);

        } catch ( IllegalStateException ex ) {

            throw new IllegalStateException("Controller is not yet connected?", ex);

        } finally {
            NDC.pop();
        }
    }

    private String resolveClass(String[] args) {

        if ( args.length > 0 ) {

            return args[0];

        } else {

            logger.info("Usage: <script> <servo controller class name> [<servo controller port name>]");
            logger.info("");
            logger.info("Example: console net.sf.servomaster.device.impl.serial.ft.FT639ServoController /dev/ttyS0");
            logger.info("Example: java -jar servomaster.jar net.sf.servomaster.device.impl.usb.phidget.PhidgetServoController");
            logger.info("");

            String targetClass = NullServoController.class.getName();

            logger.warn("Starting a demo controller (" + targetClass + ") for now");

            return targetClass;
        }
    }

    private ServoController instantiate(String targetClass) {

        NDC.push("instantiate");

        try {

            Class<?> controllerClass = Class.forName(targetClass);
            Object controllerObject = controllerClass.newInstance();

            logger.debug("Instantiated " + controllerObject.getClass().getName());

            return (ServoController)controllerObject;

        } catch (Throwable t) {

            throw new IllegalStateException("Unable to instantiate " + targetClass, t);

        } finally {
            NDC.pop();
        }
    }

    private void buildConsole(ServoController controller) throws IOException {

        NDC.push("buildConsole");

        try {

            // Figure out how many servos does the controller currently have

            List<Servo> servoSet = new LinkedList<Servo>();

            for ( Iterator<Servo> i = controller.getServos(); i.hasNext(); ) {

                servoSet.add(i.next());
            }

            int servoCount = servoSet.size();

            if ( servoCount == 0 ) {

                throw new IllegalStateException("The controller doesn't seem to have any servos now");
            }
            
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();

            mainFrame = new JFrame("Servo Controller Console, port " + controller.getPort());
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

                servoPanel[idx] = new ServoView(servoSet.get(idx));

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

                Class<?> controllerViewClass = Class.forName(controllerViewClassName);
                Object controllerViewObject = controllerViewClass.newInstance();

                if ( !(controllerViewObject instanceof JPanel) ) {

                    throw new IllegalAccessException("The servo controller view class has to extend javax.swing.JPanel, it doesn't");
                }

                if ( !(controllerViewObject instanceof ServoControllerView) ) {

                    throw new IllegalAccessException("The servo controller view class has to implement net.sf.servomaster.view.ServoControllerView, it doesn't");
                }

                JPanel controllerPanel = (JPanel)controllerViewObject;

                ((ServoControllerView)controllerPanel).init(controller);

                layout.setConstraints(controllerPanel, cs);
                console.add(controllerPanel);

                cs.gridy++;

            } catch ( Throwable t ) {

                logger.info("Couldn't instantiate the servo controller view ("
                        + controllerViewClassName
                        + ", so it will not be available. Cause:", t);
            }

            // If the controller view has been instantiated, the constraint
            // Y coordinate has been advanced. If not, we didn't need it
            // anyway

            SilencerPanel silencerPanel = createSilencerPanel(controller);

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

        } finally {
            NDC.pop();
        }
    }

    private SilencerPanel createSilencerPanel(ServoController controller) throws IOException {

        try {

            if ( controller.getMeta().getFeature("controller/silent") ) {

                controller.setSilentMode(true);
                controller.setSilentTimeout(10000, 30000);

                return new SilencerPanel(controller);
            }

        } catch ( UnsupportedOperationException ex ) {

            logger.warn("Controller doesn't support servo shutoff (reason: " + ex.getMessage() + ")");
            return null;
        }

        // VT: NOTE: getMeta() will throw an exception if the feature is not supported
        throw new IllegalStateException("We shouldn't have arrived here");
    }

    /**
     * React to the button presses.
     */
    @Override
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

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

        System.exit(0);
    }


    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    protected abstract class exec implements Runnable {

        @Override
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

                logger.fatal("Oops, controller operation failed:", t);

                logger.fatal("Controller is considered inoperable, exiting");
                System.exit(1);

            } finally {

                try {

                    cleanup();

                } catch ( Throwable t ) {

                    logger.warn("Problem trying to clean up:", t);
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

            for (int offset = 0; offset < controller.getServoCount(); offset++) {

                if (!servoPanel[offset].isEnabled()) {

                    logger.info("skipped: @" + offset);
                    continue;
                }

                Servo s = controller.getServo(Integer.toString(offset));

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

            // With some bad luck, we might start the clock right about the top of the second,
            // with drift making the demo skip seconds (). Believe it or not, this is guaranteed to happen
            // within less than 90 seconds.

            while ( true ) {

                LocalTime now = syncSecond();

                int seconds = now.getSecond();
                int minutes = now.getMinute();
                int hours   = now.getHour();

                servoSecond.setPosition((double)seconds/(double)60);
                servoMinute.setPosition((double)minutes/(double)60);
                servoHour.setPosition((double)hours/(double)24);
            }
        }

        private LocalTime syncSecond() throws InterruptedException {

            // To avoid skipping the beats, let's synchronize right after
            // the top of the second so the clock drift within the demo period won't affect the presentation

            LocalTime now = LocalTime.now();

            long timeout = 999999999 - now.getNano() + 99999999;

            TimeUnit.NANOSECONDS.sleep(timeout);

            // At this point, we're about 100 nanoseconds after the top of the second. Refresh the value, and round it for display.

            now = LocalTime.now();
            now = LocalTime.of(now.getHour(), now.getMinute(), now.getSecond());

            logger.info("now: " + now);

            return now;
        }
    }
}
