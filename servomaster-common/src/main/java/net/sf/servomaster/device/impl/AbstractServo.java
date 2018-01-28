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

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.TransitionCompletionToken;
import net.sf.servomaster.device.model.TransitionController;

/**
 * Basic support for servo abstraction.
 *
 * Supports the transition controller functionality. Allows instant as well as
 * time controlled positioning and feedback.
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
     * Thread pool for transition drivers.
     * 
     * This pool requires exactly one thread.
     */
    private final ExecutorService transitionDriverExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, driverQueue);

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
    public final synchronized void attach(TransitionController transitionController, boolean queueTransitions) {

        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods

        Servo s = getTarget();

        while (s != null) {

            if (s.getTransitionController() != null) {

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

        if (!enabled) {

            throw new IllegalStateException("Not enabled");
        }

        // VT: FIXME: Currently, there's no check whether the position is the same as the servo is already set to.
        // This will cause a flood of false listener notifications in case when lazy code keeps setting the same position.
        // Since the position is defined as double, it will require some sleuth of hand to calculate whether
        // the hardware coordinates for two doubles sufficiently close to each other are the same, and skip the request altogether.

        // The reason it is synchronized on the controller is that the
        // setActualPosition() calls the controller's synchronized methods
        // and the deadlock can occur if *this* method was made synchronized

        try {

            synchronized (servoController) {

                this.position = position;

                if (transitionController == null) {

                    setActualPosition(position);

                    return new TCT(true);
                }

                // VT: FIXME: Decide whether to stop the currently running transition (probably yes)

                TransitionCompletionToken token = new TCT(false);
                TransitionDriver transitionDriver = new TransitionDriver(new TransitionProxy(), position, (TCT)token);

                transitionDriverExecutor.execute(transitionDriver);

                return token;
            }

        } finally {

            positionChanged();
        }
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

        for (Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext();) {

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

        for (Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext();) {

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

        if (!listenerSet.contains(listener)) {

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

        if (position < 0 || position > 1.0) {

            throw new IllegalArgumentException("Position out of 0...1.0 range: " + position);
        }
    }

    private class TransitionDriver implements Runnable {

        private Servo target;
        private double targetPosition;
        private TCT completionToken;

        TransitionDriver(Servo target, double targetPosition, TCT completionToken) {

            this.target = target;
            this.targetPosition = targetPosition;
            this.completionToken = completionToken;
        }

        @Override
        public void run() {
            
            NDC.push("run");
            
            try {

                logger.debug("Transition: " + getActualPosition() + " => " + targetPosition);

                transitionController.move(target, targetPosition);

            } finally {

                // No matter what's the cause, we're done - even though sometimes the actual position
                // may be different from desired.

                // VT: FIXME: Make sure that gets taken care of.

                completionToken.done();

                // This will help when thread pool executor is used
                NDC.clear();

                // Without this, memory leaks like a sieve in DZ3
                NDC.remove();
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

    /**
     * Gives {@link TransitionController} implementations direct access to {@link AbstractServo#setActualPosition(double)}
     * without them knowing about that.
     *
     * Generally speaking, transition controllers have no business doing a lot of things with a servo, so that will be disallowed as well.
     */
    private class TransitionProxy implements Servo {

        private static final String NONO = "Transition controller shouldn't be accessing this functionality";

        @Override
        public void setSilentMode(boolean silent) throws IOException {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public void setSilentTimeout(long timeout, long heartbeat) {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public boolean isSilentNow() {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public boolean getSilentMode() {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public String getName() {

            return AbstractServo.this.getName();
        }

        @Override
        public TransitionCompletionToken setPosition(double position) throws IOException {

            NDC.push("wrap");

            try {

                AbstractServo.this.setActualPosition(position);
                return null;

            } finally {
                NDC.pop();
            }
        }

        @Override
        public double getPosition() {

            return AbstractServo.this.getActualPosition();
        }

        @Override
        public double getActualPosition() {

            return AbstractServo.this.getActualPosition();
        }

        @Override
        public void addListener(ServoListener listener) {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public void removeListener(ServoListener listener) {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public Meta getMeta() {

            return AbstractServo.this.getMeta();
        }

        @Override
        public ServoController getController() {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public void attach(TransitionController transitionController, boolean queueTransitions) {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public TransitionController getTransitionController() {

            throw new IllegalAccessError(NONO);
        }

        @Override
        public Servo getTarget() {

            throw new IllegalAccessError(NONO);
        }
    }
}
