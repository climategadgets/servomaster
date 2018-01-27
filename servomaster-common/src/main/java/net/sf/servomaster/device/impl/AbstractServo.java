package net.sf.servomaster.device.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.TransitionCompletionToken;
import net.sf.servomaster.device.model.TransitionController;
import net.sf.servomaster.device.model.TransitionToken;

/**
 * Basic support for servo abstraction.
 *
 * Supports the transition controller functionality. Allows instant and
 * controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractServo implements Servo {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Worker queue for transition drivers.
     */
    private final BlockingQueue<Runnable> driverQueue = new LinkedBlockingQueue<Runnable>();
    
    /**
     * Worker queue for transition listeners.
     */
    private final BlockingQueue<Runnable> listenerQueue = new LinkedBlockingQueue<Runnable>();
    
    /**
     * Thread pool for transition drivers.
     * 
     * It doesn't make sense for this pool to have more than one thread.
     */
    private final ExecutorService transitionDriverExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, driverQueue);

    /**
     * Thread pool for transition listeners.
     * 
     * It doesn't make sense for this pool to have more than one thread.
     */
    private final ExecutorService listenerExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, listenerQueue);

    /**
     * The actual servo to control.
     *
     * If <strong>this</strong> is the servo to control, then this variable
     * is set to {@code null}.
     */
    private Servo target;

    /**
     * The controller reference.
     */
    private ServoController servoController;

    /**
     * The transition controller attached to the servo.
     *
     * If the value is {@code null}, then there is no transition - the
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
     * Changes immediately after {@link #setPosition setPosition()} call, unlike {@link #actualPosition}.
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
     * {@code true} if this servo is enabled.
     */
    private boolean enabled = true;

    /**
     * The listener set.
     */
    private Set<ServoListener> listenerSet = new HashSet<ServoListener>();

    /**
     * Create the stacked instance.
     *
     * @param servoController The controller this servo belongs to.
     *
     * @param target The servo to stack on top of. If it is
     * <code>null</code>, the instance is at the bottom of the stack and it
     * is the actual hardware driver object.
     */
    protected AbstractServo(ServoController servoController, Servo target) {

        this.servoController = servoController;
        this.target = target;
    }

    @Override
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

    @Override
    public final TransitionController getTransitionController() {

        return transitionController;
    }

    @Override
    public final Servo getTarget() {

        return target;
    }

    @Override
    public ServoController getController() {

        return servoController;
    }

    @Override
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

                token = new TCT(false);
                transitionDriver = new TransitionDriver(this, position, (TCT)token);
                transitionDriverExecutor.execute(transitionDriver);

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
     * @param position Position to set.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware controller.
     */
    protected abstract void setActualPosition(double position) throws IOException;

    /**
     * Notify the listeners about the change in requested position.
     */
    private synchronized void positionChanged() {

        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods

        for ( Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().positionChanged(this, position);
        }
    }

    /**
     * Notify the listeners about the change in actual position.
     */
    protected final synchronized void actualPositionChanged() {

        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods

        // VT: FIXME: it may make sense to make this private and change the logic

        for ( Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().actualPositionChanged(this, actualPosition);
        }
    }

    @Override
    public void setEnabled(boolean enabled) throws IOException {

        this.enabled = enabled;
    }

    @Override
    public double getPosition() {

        return position;
    }

    @Override
    public double getActualPosition() {

        return actualPosition;
    }

    @Override
    public synchronized void addListener(ServoListener listener) {

        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods

        listenerSet.add(listener);
    }

    @Override
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
        private TCT completionToken;

        TransitionDriver(Servo target, double targetPosition, TCT completionToken) {

            this.target = target;
            this.targetPosition = targetPosition;
            this.completionToken = completionToken;
        }

        @Override
        public void run() {

            logger.debug("Transition: " + getActualPosition() + " => " + targetPosition);

            listenerExecutor.execute(new Listener());

            transitionController.move(target, token, targetPosition);
            
            // This will help when thread pool executor is used
            NDC.clear(); 
            
            // Without this, memory leaks like a sieve in DZ3
            NDC.remove();
        }

        public void stop() {

            token.stop();
        }

        private class Listener implements Runnable {

            @Override
            public void run() {
                
                NDC.push("run");

                try {

                    while ( true ) {

                        try {

                            setActualPosition(token.consume());

                        } catch ( IllegalStateException ex ) {

                            logger.debug("Controller stopped the transition", ex);

                            return;

                        } catch ( Throwable t ) {

                            logger.error("Unexpected transition problem", t);

                            return;
                        }
                    }

                } finally {

                    completionToken.done();
                    
                    // Just in case
                    NDC.pop();

                    // This will help when thread pool executor is used
                    NDC.clear(); 
                    
                    // Without this, memory leaks like a sieve in DZ3
                    NDC.remove();
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
    protected static class TCT implements TransitionCompletionToken {

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
        protected TCT(boolean complete) {

            this.complete = complete;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized boolean isComplete() {

            return complete;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void waitFor() throws InterruptedException {

            while (!complete) {

                wait();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void waitFor(long millis) throws InterruptedException {

            long start = System.currentTimeMillis();

            while (!complete) {

                long timeout = millis - (System.currentTimeMillis() - start);

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

    /**
     * Default behavior is not to support silent operation.
     */
    @Override
    public void setSilentMode(boolean silent) throws IOException {

        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Default behavior is not to support silent operation.
     */
    @Override
    public void setSilentTimeout(long timeout, long heartbeat) {

        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Default behavior is not to support silent operation.
     */
    @Override
    public boolean isSilentNow() {

        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Default behavior is not to support silent operation.
     */
    @Override
    public boolean getSilentMode() {

        throw new UnsupportedOperationException("Not Implemented");
    }
}
