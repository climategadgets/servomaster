package org.freehold.servomaster.test.integration;

import java.io.IOException;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.ServoController;
import org.freehold.servomaster.device.model.ServoControllerListener;
import org.freehold.servomaster.device.model.ServoControllerMetaData;

/**
 * Synchronization check.
 *
 * <p>
 *
 * The proper operation of the servo controller and the servo abstraction
 * requires proper synchronization. The proper synchronization may be broken
 * by transaction adapters, silent mode handlers and concurrent operation.
 * In order to check the proper synchronization, the servo controller and
 * the servo abstractions must be hit by a stress test.
 *
 * This <strong>is</strong> the stress test.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001,2002
 * @version $Id: SyncCheck.java,v 1.1 2002-02-20 08:25:12 vtt Exp $
 */
public class SyncCheck {

    /**
     * The controller to watch and control.
     */
    private ServoController controller;
    
    /**
     * The controller port name.
     */
    private String portName;
    
    /**
     * Controller's servo set.
     */
    private Servo servoSet[];
    
    private Random rg = new Random();
    
    /**
     * Whether the test is still enabled.
     */
    private boolean enabled = true;
    
    /**
     * Whether the stresser should sleep now.
     */
    private boolean sleeping = false;
    
    public static void main(String args[]) {
    
        new SyncCheck().run(args);
    }
    
    public void run(String args[]) {
    
        try {
        
            if ( args.length < 2 ) {
            
                System.err.println("Usage: <script> <servo controller class name> <servo controller port name>");
                System.err.println("");
                System.err.println("Example: sync_check org.freehold.servomaster.device.impl.ft.FT639ServoController /dev/ttyS0");
                System.err.println("Example: java -classpath servomaster.jar org.freehold.servomaster.test.integration.SyncCheck \\");
                System.err.println("                                         org.freehold.servomaster.device.impl.ft.FT639ServoController /dev/ttyS0");
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
            
            // Figure out how many servos does the controller currently have
            
            Vector servos = new Vector();
            
            for ( Iterator i = controller.getServos(); i.hasNext(); ) {
            
                servos.add(i.next());
            }
            
            int servoCount = servos.size();
            
            if ( servoCount == 0 ) {
            
                System.err.println("The controller doesn't seem to have any servos now");
                System.exit(1);
            }
            
            servoSet = new Servo[servos.size()];
            
            for ( int i = 0; i < servos.size(); i++ ) {
            
                servoSet[i] = (Servo)servos.elementAt(i);
            }
            
            // Set up the controller
            
            try {
            
                controller.setSilentMode(true);
                controller.setSilentTimeout(2000, 2000);
                
            } catch ( UnsupportedOperationException uoex ) {
            
                System.err.println("Controller doesn't support silent mode:");
                uoex.printStackTrace();
            }
            
            // Now, run the test
            
            stress();

        } catch ( Throwable t ) {
        
            t.printStackTrace();
        }
    }
    
    private synchronized void wake() {
    
        sleeping = false;
        notifyAll();
    }
    
    private synchronized void sleep(int id) {
    
        while ( sleeping ) {
        
            //System.out.println("S " + Integer.toHexString(id));
            
            try {
            
                wait();
            
            } catch ( InterruptedException iex ) {
            
            }
        }
    }
    
    private final int COUNT = 50;
    private Thread stressSet[] = new Thread[COUNT];
    
    private void stress() throws Throwable {
    
        for ( int i = 0; i < COUNT; i++ ) {
        
            Stresser s = new Stresser(i);

            stressSet[i] = new Thread(s);
        }

        for ( int i = 0; i < COUNT; i++ ) {
        
            stressSet[i].start();
        }
        
        Sleeper p = new Sleeper();
        
        Thread pt = new Thread(p);
        
        pt.start();
        
        Thread.sleep(300000);
        
        System.err.println("STRESS TEST FINISHED");
        
        enabled = false;
        wake();
        
        for ( int i = 0; i < COUNT; i++ ) {
        
            stressSet[i].interrupt();
        }
        
        pt.interrupt();
    }
    
    protected class Stresser implements Runnable {
    
        int id;
        
        Stresser(int id) {
        
            this.id = id;
        }
    
        public void run() {
        
            while ( enabled ) {
            
                sleep(id);
            
                int servo = rg.nextInt(servoSet.length);
                double position = Math.random();

                try {
                
                    servoSet[servo].setPosition(position);
//                    System.out.print(".");
//                    System.out.println("+ " + Integer.toHexString(id) + "/" + servo + ": " + position);
                    
                } catch ( IOException ioex ) {
                
                    System.out.println("E " + Integer.toHexString(id) + "/" + servo + ": " + ioex.getMessage());
                }
            }
        }
    }
    
    protected class Sleeper implements Runnable {
    
        public void run() {
        
            while ( enabled ) {
            
                try {

                    System.err.println("W");
                    Thread.sleep((long)(Math.random() * 3000));
                    
                    sleeping = true;
                    
                    System.err.println("S");
                    Thread.sleep((long)(Math.random() * 3000));
                    
                    wake();

                } catch ( InterruptedException iex ) {
                
                }
            }
        }
    }
}
