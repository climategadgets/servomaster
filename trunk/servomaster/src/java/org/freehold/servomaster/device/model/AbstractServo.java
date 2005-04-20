package org.freehold.servomaster.device.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A servo abstraction.
 *
 * Supports the transition controller functionality. Allows instant and
 * controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: AbstractServo.java,v 1.9 2005-04-20 22:01:31 vtt Exp $
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
    
        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods
        
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
    
    public TransitionCompletionToken setPosition(double position) throws IOException {
    
        if ( !enabled ) {
        
            throw new IllegalStateException("Not enabled");
        }
    
        // The reason it is synchronized on the controller is that the
        // setActualPosition() calls the controller's synchronized methods
        // and the deadlock can occur if *this* method was made synchronized
        
        TransitionCompletionToken token = null;
        
        synchronized ( servoController ) {
        
            this.position = position;

            if ( transitionController != null ) {
            
                if ( transitionDriver != null ) {
                
                    transitionDriver.stop();
                }
                
                transitionDriver = new TransitionDriver(this, position);
                new Thread(transitionDriver).start();

            } else {
            
                setActualPosition(position);
                
                token = new TCT(true);
            }
        }
        
        positionChanged();
        
        return token;
    }

    /**
     * Set the servo position with no regard to the transition controller.
     *
     * <p>
     *
     * This method is ultimately called by the transition controller, as
     * well as directly from {@link #setPosition setPosition()} when
     * the transition controller is not attached.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    abstract protected void setActualPosition(double position) throws IOException;

    /**
     * Notify the listeners about the change in requested position.
     */
    private final synchronized void positionChanged() {
    
        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods
        
        for ( Iterator i = listenerSet.iterator(); i.hasNext(); ) {
        
            ((ServoListener)i.next()).positionChanged(this, position);
        }
    }
    
    /**
     * Notify the listeners about the change in actual position.
     */
    protected final synchronized void actualPositionChanged() {
    
        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods
        
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
    
        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods
        
        listenerSet.add(listener);
    }
    
    public synchronized void removeListener(ServoListener listener) {
    
        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods
        
        if ( !listenerSet.contains(listener) ) {
        
            throw new IllegalArgumentException("Not a registered listener: "
                                               + listener.getClass().getName()
                                               + "@"
                                               + listener.hashCode());
        }
        
        listenerSet.remove(listener);
    }
    
    /**
     * Check if the value is within 0...1.0 range.
     *
     * @param position Value to check.
     *
     * @exception IllegalArgumentException if the position is out of 0...1.0
     * range.
     */
    protected final void checkPosition(double position) {
    
        if ( position < 0 || position > 1.0 ) {
        
            throw new IllegalArgumentException("Position out of 0...1.0 range: " + position);
        }
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
    
    /**
     * Implementation for the {@link TransitionCompletionToken
     * TrasitionCompletionToken} interface.
     *
     * The difference between the interface and the implementation is the
     * {@link #done done()} method the API user should have no business
     * knowing about.
     */
    protected class TCT implements TransitionCompletionToken {
    
        /**
         * Completion state.
         */
        private boolean complete;
        
        /**
         * Create an instance.
         *
         * @param complete Completion state. If this argument is
         * <code>true</code>, the token will be considered complete upon
         * creation - this is required for the case when no transition
         * controller is attached.
         */
        public TCT(boolean complete) {
        
            this.complete = complete;
        }
        
        /**
         * {@inheritDoc}
         */
        public synchronized boolean isComplete() {
        
            return complete;
        }
        
        /**
         * {@inheritDoc}
         */
        public synchronized void waitFor() throws InterruptedException {
        
            while (!complete) {
            
                wait();
            }
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void waitFor(long millis) throws InterruptedException {
        
            long start = System.currentTimeMillis();
            long timeout = millis;
            
            while (!complete) {
            
                timeout = millis - (System.currentTimeMillis() - start);
            
                if (timeout <= 0) {
                
                    // VT: FIXME: This is a wrong kind of exception...
                    
                    throw new InterruptedException("Timeout expired: " + millis + "ms");
                }
            
                wait(timeout);
            }
        }
        
        /**
         * Mark the token as complete.
         */
        public synchronized void done() {
        
            if (complete) {
            
                throw new IllegalStateException("Already done");
            }
            
            complete = true;
            
            notifyAll();
        }
    }
}
