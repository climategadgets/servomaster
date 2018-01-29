package net.sf.servomaster.device.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.TransitionController;
import net.sf.servomaster.device.model.silencer.SilentHelper;
import net.sf.servomaster.device.model.silencer.SilentProxy;

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
     * Thread pool for transition drivers.
     * 
     * This pool requires exactly one thread.
     */
    private final ExecutorService transitionDriverExecutor = Executors.newFixedThreadPool(1);

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
     * The silencer.
     */
    private SilentHelper silencer;

    /**
     * The silencer proxy.
     */
    private SilentProxy silencerProxy;

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

        if (false) {

            // VT: FIXME: Just like the FIXME below says, it's a mess.
            // getMeta() called ahead of final construction completion will produce
            // a malfunctioning object.

            // Will need to get back to this when https://github.com/climategadgets/servomaster/issues/2 is done.

            try {

                // VT: FIXME: It's a mess with instantiation of these two - they can't be made final.
                // Will be fixed in the next iteration.

                if ( getMeta().getFeature("servo/silent") ) {

                    silencerProxy = new SilentGuard();
                    silencer = new SilentHelper(silencerProxy);
                    silencer.start();
                }

            } catch ( UnsupportedOperationException ex ) {

                // VT: NOTE: In this particular case, it's OK to suppress the exception trace - it's a part of the contract
                // There will be more servos than controllers, some of them transients and proxies,
                // let's identify them better to avoid confusion

                // VT: FIXME: This message may be gone altogether after https://github.com/climategadgets/servomaster/issues/16
                // is fully implemented

                logger.info(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": servo doesn't support silent operation, reason: " + ex.getMessage());

            } catch ( IllegalStateException ex ) {

                logger.warn("unexpected exception", ex);
            }
        }
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
    public Future<Throwable> setPosition(double position) throws IOException {

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

                    return new Done();
                }

                // VT: FIXME: cancel the previous transition if so instructed: https://github.com/climategadgets/servomaster/issues/22

                TransitionDriver transitionDriver = new TransitionDriver(new TransitionProxy(), position);

                return transitionDriverExecutor.submit(transitionDriver, new Throwable());
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

        private final Servo target;
        private final double targetPosition;

        TransitionDriver(Servo target, double targetPosition) {

            this.target = target;
            this.targetPosition = targetPosition;
        }

        @Override
        public void run() {
            
            NDC.push("run");
            
            try {

                logger.debug("Transition: " + getActualPosition() + " => " + targetPosition);

                transitionController.move(target, targetPosition);

            } finally {

                // This will help when thread pool executor is used
                NDC.clear();

                // Without this, memory leaks like a sieve in DZ3
                NDC.remove();
            }
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
        public Future<Throwable> setPosition(double position) throws IOException {

            NDC.push("wrap");

            try {

                AbstractServo.this.setActualPosition(position);
                return new Done();

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

    /**
     * This future has already come.
     *
     * This object is returned by {@link AbstractServo#setPosition(double)} in absence of a transition controller.
     */
    public static class Done implements Future<Throwable> {

        /**
         * @return {@code false}. We're already done.
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        /**
         * @return {@code true}. We were done before we started.
         */
        @Override
        public boolean isDone() {
            return true;
        }

        /**
         * @return {@code null}.
         */
        @Override
        public Throwable get() throws InterruptedException, ExecutionException {
            return null;
        }

        /**
         * @return {@code null}.
         */
        @Override
        public Throwable get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

    /**
     * Notify the listeners about the change in the silent status.
     *
     * @param mode The new silent status. {@code false} means device is
     * sleeping, {@code true} means device is active.
     */
    protected final void silentStatusChanged(boolean mode) {

        for ( Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().silentStatusChanged(this, mode);
        }
    }

    /**
     * Notify the listeners about the problem that occured.
     *
     * @param t The exception to broadcast.
     */
    protected final void exception(Throwable t) {

        for ( Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext(); ) {

            i.next().exception(this, t);
        }
    }

    protected void sleep() throws IOException {

        // Do absolutely nothing
        logger.debug("sleep: not implemented");
    }

    protected void wakeUp() throws IOException {

        // Do absolutely nothing
        logger.debug("wakeUp: not implemented");
    }

    /**
     * The reason for existence of this class is that {@link AbstractServo#sleep()} and {@link AbstractServo#wakeUp()}
     * operations can't be exposed via implemented interface without violating the target integrity. 
     */
    private class SilentGuard implements SilentProxy {

        @Override
        public void sleep() {

            NDC.push("sleep");

            try {

                AbstractServo.this.sleep();
                AbstractServo.this.silentStatusChanged(false);

            } catch (IOException ioex) {

                AbstractServo.this.exception(ioex);

            } finally {
                NDC.pop();
            }
        }

        @Override
        public void wakeUp() {

            NDC.push("wakeUp");

            try {

                AbstractServo.this.wakeUp();
                AbstractServo.this.silentStatusChanged(true);

            } catch (IOException ioex) {

                AbstractServo.this.exception(ioex);

            } finally {
                NDC.pop();
            }
        }
    }
}
