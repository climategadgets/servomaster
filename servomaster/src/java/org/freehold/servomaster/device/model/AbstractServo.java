package org.freehold.servomaster.device.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Supports the transition controller functionality.
 *
 * Allows instant and controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: AbstractServo.java,v 1.3 2002-02-10 07:23:55 vtt Exp $
 */
abstract public class AbstractServo implements Servo {

    /**
     * The actual servo to control.
     *
     * If <strong>this</strong> is the servo to control, then this variable
     * is set to <code>null</code>.
     */
    private Servo target;
    
    /**
     * The controller reference.
     */
    private ServoController servoController;
    
    /**
     * The transition controller attached to the servo.
     *
     * If the value is <code>null</code>, then there is no transition - the
     * {@link #actualPosition actual position} is identical to {@link
     * #position requested position}.
     */
    private TransitionController transitionController;
    
    /**
     * The transition driver for the current transition.
     */
    private TransitionDriver transitionDriver;
    
    /**
     * Requested position.
     *
     * Changes immediately after {@link #setPosition setPosition()} call.
     */
    private double position;
    
    /**
     * Actual position.
     *
     * Differs from {@link #position requested position} when the {@link
     * #transitionController transition controller} is attached.
     */
    protected double actualPosition;
    
    /**
     * Enabled mode.
     */
    private boolean enabled = true;
    
    /**
     * The listener set.
     */
    private Set listenerSet = new HashSet();

    /**
     * Create the stacked instance.
     *
     * @param servoController The controller this servo belongs to.
     *
     * @param target The servo to stack on top of. If it is
     * <code>null</code>, the instance is at the bottom of the stack and it
     * is the actual hardware driver object.
     */
    public AbstractServo(ServoController servoController, Servo target) {
    
        this.servoController = servoController;
        this.target = target;
    }

    public final synchronized void attach(TransitionController transitionController) {
    
        Servo s = getTarget();
        
        while ( s != null ) {
        
            if ( s.getTransitionController() != null ) {
            
                throw new IllegalStateException("Can't attach more than one transition controller in a stack");
            }
            
            s = s.getTarget();
        }
    
        this.transitionController = transitionController;
    }
    
    public final TransitionController getTransitionController() {
    
        return transitionController;
    }
    
    public final Servo getTarget() {
    
        return target;
    }
    
    public ServoController getController() {
    
        return servoController;
    }
    
    public synchronized void setPosition(double position) throws IOException {
    
        if ( !enabled ) {
        
            throw new IllegalStateException("Not enabled");
        }
    
        this.position = position;

        if ( transitionController != null ) {
        
            if ( transitionDriver != null ) {
            
                transitionDriver.stop();
            }
            
            transitionDriver = new TransitionDriver(this, position);
            new Thread(transitionDriver).start();

        } else {
        
            setActualPosition(position);
        }
        
        positionChanged();
    }

    /**
     * Set the servo position with no regard to the transition controller.
     *
     * <p>
     *
     * This method is ultimately called by the transition controller, as
     * well as directly from {@link #setPosition setPosition()} when
     * the transition controller is not attached.
     */
    abstract protected void setActualPosition(double position) throws IOException;

    /**
     * Notify the listeners about the change in requested position.
     */
    private final synchronized void positionChanged() {
    
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ServoListener)i.next()).positionChanged(this, position);
        }
    }
    
    /**
     * Notify the listeners about the change in actual position.
     */
    protected final synchronized void actualPositionChanged() {
    
        // VT: FIXME: it may make sense to make this private and change the logic
        
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ServoListener)i.next()).actualPositionChanged(this, actualPosition);
        }
    }
    
    public void setEnabled(boolean enabled) throws IOException {
    
        this.enabled = enabled;
    }
    
    public double getPosition() {
    
        return position;
    }
    
    public double getActualPosition() {
    
        return actualPosition;
    }
    
    public synchronized void addListener(ServoListener listener) {
    
        listenerSet.add(listener);
    }
    
    public synchronized void removeListener(ServoListener listener) {
    
        if ( !listenerSet.contains(listener) ) {
        
            throw new IllegalArgumentException("Not a registered listener: "
                                               + listener.getClass().getName()
                                               + "@"
                                               + listener.hashCode());
        }
        
        listenerSet.remove(listener);
    }
    
    private class TransitionDriver implements Runnable {
    
        private Servo target;
        private double targetPosition;
        private TransitionToken token = new TransitionToken();
        
        TransitionDriver(Servo target, double targetPosition) {
        
            this.target = target;
            this.targetPosition = targetPosition;
        }
    
        public void run() {
        
//            System.err.println("Transition: " + getActualPosition() + " > " + targetPosition);
            
            Runnable l = new Listener();
            
            new Thread(l).start();
            
            transitionController.move(target, token, targetPosition);
        }
        
        public void stop() {
        
            token.stop();
        }
        
        private class Listener implements Runnable {
        
            public void run() {
            
                while ( true ) {
                
                    try {
                    
                        setActualPosition(token.consume());
                        
                    } catch ( IllegalStateException isex ) {
                    
                        //System.err.println("Controller stopped the transition:");
                        //isex.printStackTrace();

                        return;
                    
                    } catch ( Throwable t ) {
                    
                        System.err.println("Unexpected transition problem:");
                        t.printStackTrace();

                        return;
                    }
                }
            }
        }
    }
}
