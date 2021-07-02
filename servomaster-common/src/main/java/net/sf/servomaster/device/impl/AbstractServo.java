package net.sf.servomaster.device.impl;

import net.sf.servomaster.device.model.Meta;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.ServoController;
import net.sf.servomaster.device.model.ServoController.Feature;
import net.sf.servomaster.device.model.ServoListener;
import net.sf.servomaster.device.model.TransitionController;
import net.sf.servomaster.device.model.TransitionStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic support for servo abstraction.
 *
 * Supports the transition controller functionality. Allows instant as well as
 * time controlled positioning and feedback.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractServo implements Servo {

    private static final Random rg = new SecureRandom();

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * String key to retrieve the silent support feature.
     */
    public static final String META_SILENT = "servo/silent";

    /**
     * Thread pool for transition drivers.
     *
     * This pool requires exactly one thread.
     */
    private final ExecutorService transitionDriverExecutor = Executors.newFixedThreadPool(1);

    /**
     * {@code true} if a previous transition should complete before starting the next one,
     * {@code false} if it needs to be interrupted.
     */
    private boolean queueTransitions = false;

    /**
     * Last transition known to be running, or {@code null} if there was none.
     */
    private Future<TransitionStatus> lastTransition = null;

    /**
     * Thread pool for sending notifications.
     *
     * We don't care how many threads send notifications. The order of notifications
     * sent is undefined and irrelevant.
     */
    private final ExecutorService broadcaster = Executors.newCachedThreadPool();

    /**
     * The actual servo to control.
     *
     * If <strong>this</strong> is the servo to control, then this variable
     * is set to {@code null}.
     */
    private final Servo target;

    /**
     * The controller reference.
     */
    private final ServoController servoController;

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
    private ServoSilencer silencer;

    /**
     * @see #getMeta()
     */
    private Meta meta;

    /**
     * Whether this instance is initialized.
     *
     * Becomes {@code true} in {@link #open()}.
     */
    private boolean initialized = false;

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
    public final synchronized void open() {

        // Now that we know we're instantiated, we can finally get it
        meta = createMeta();

        initialized = true;

        startSilencer();
    }

    protected Meta createMeta() {
        throw new UnsupportedOperationException("This driver class doesn't provide metadata (most probably oversight on developer's part)");
    }

    private void startSilencer() {

        if ( getMeta().getFeatures().containsKey(META_SILENT) ) {

            silencer = new ServoSilencer(5000, 30000);
            silencer.start();
        }
    }

    private synchronized void checkInit() {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    @SuppressWarnings("resource")
    @Override
    public final synchronized void attach(TransitionController transitionController, boolean queueTransitions) {

        checkInit();

        // This operation can safely be made synchronized because it doesn't
        // use the controller's synchronized methods

        // First check if we were to cancel running transitions before

        if (!this.queueTransitions) {
            cancelLastTransition();
        }

        Servo s = getTarget();

        while (s != null) {

            if (s.getTransitionController() != null) {

                throw new IllegalStateException("Can't attach more than one transition controller in a stack");
            }

            s = s.getTarget();
        }

        this.transitionController = transitionController;
        this.queueTransitions = queueTransitions;
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
    public Future<TransitionStatus> setPosition(double position) {

        if (!enabled) {

            throw new IllegalStateException("Not enabled");
        }

        // VT: FIXME: Currently, there's no check whether the position is the same as the servo is already set to.
        // This will cause a flood of false listener notifications in case when lazy code keeps setting the same position.
        // Since the position is defined as double, it will require some sleight of hand to calculate whether
        // the hardware coordinates for two doubles sufficiently close to each other are the same, and skip the request altogether.

        // The reason it is synchronized on the controller is that the
        // setActualPosition() calls the controller's synchronized methods
        // and the deadlock can occur if *this* method was made synchronized

        try {

            synchronized (servoController) {

                this.position = position;

                if (transitionController == null) {

                    // There's no need to bother canceling the last transition because it was instantaneous

                    try {

                        setActualPosition(position);

                        return new Done(null);

                    } catch (IOException ex) {
                        return new Done(ex);
                    }
                }

                if (!this.queueTransitions) {
                    cancelLastTransition();
                }

                // VT: FIXME: transition driver needs to be aware of the status object.
                //
                // https://github.com/climategadgets/servomaster/issues/30
                // https://github.com/climategadgets/servomaster/issues/31
                //
                // It is unreasonably complex to implement everything in one commit,
                // so this is left hanging - transition driver will not properly handle
                // an exception at this point. Need to get back to this ASAP.

                long authToken = rg.nextLong();
                TransitionStatus status = new TransitionStatus(authToken);
                TransitionDriver transitionDriver = new TransitionDriver(new TransitionProxy(), position);

                lastTransition = transitionDriverExecutor.submit(transitionDriver, status);

                return lastTransition;
            }

        } finally {

            positionChanged(position);
        }
    }

    /**
     * Cancel the {@link #lastTransition last transition} that may still possibly be incomplete.
     *
     * This method doesn't check whether the transitions are {@link #queueTransitions allowed to be queued}
     * on purpose, so the surrounding code makes a visible check.
     */
    private synchronized void cancelLastTransition() {

        if (lastTransition == null) {

            // Nothing to do
            return;
        }

        ThreadContext.push("cancelTransition");

        try {

            if (lastTransition.isDone()) {

                logger.debug("already done");
                return;
            }

            // VT: NOTE: This doesn't mean the transition will be automatically canceled right away.
            // See how transition controller has to be implemented to honor this, here's a good starting point:
            //
            // https://stackoverflow.com/questions/1418033/java-executors-how-can-i-stop-submitted-tasks

            logger.debug("cancel: " + lastTransition.cancel(true));

        } finally {
            ThreadContext.pop();
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
     * @throws IOException if there was a problem communicating with the hardware controller.
     */
    protected abstract void setActualPosition(double position) throws IOException;

    /**
     * Notify the listeners about the change in requested position.
     *
     * @param position Position to broadcast.
     */
    private void positionChanged(final double position) {

        for (Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext();) {

            ServoListener l = i.next();

            broadcaster.execute(new RunnableWrapper(logger,"positionChanged") {

                @Override
                protected void doRun() {

                    l.positionChanged(AbstractServo.this, position);
                }
            });
        }
    }

    /**
     * Notify the listeners about the change in actual position.
     *
     * @param actualPosition Position to broadcast.
     */
    protected final void actualPositionChanged(double actualPosition) {

        // VT: FIXME: it may make sense to make this private and change the logic

        for (Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext();) {

            ServoListener l = i.next();

            broadcaster.execute(new RunnableWrapper(logger,"actualPositionChanged") {

                @Override
                protected void doRun() {

                    l.actualPositionChanged(AbstractServo.this, actualPosition);
                }
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) throws IOException {

        // Can't make the method synchronized, it'll result in a deadlock

        synchronized (servoController) {

            this.enabled = enabled;

            if (!enabled) {

                sleep();
                touch();

                silentStatusChanged(false);

            } else {

                // That'll wake them up
                setPosition(position);
            }
        }
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
     * @throws IllegalArgumentException if the position is out of 0...1.0 range.
     */
    protected final void checkPosition(double position) {

        if (position < 0 || position > 1.0) {

            throw new IllegalArgumentException("Position out of 0...1.0 range: " + position);
        }
    }

    @Override
    public final synchronized Meta getMeta() {

        // Meta will be created the first thing after the instance is created, in open().
        // If it hasn't been, something is wrong

        if (meta == null) {

            if (initialized) {
                throw new IllegalStateException("initialized but meta is still null, is everything all right?");
            }

            throw new IllegalStateException("calling getMeta() before open()?");
        }

        return meta;
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

            ThreadContext.push("run");

            try {

                logger.debug("Transition: " + getActualPosition() + " => " + targetPosition);

                transitionController.move(target, targetPosition);

            } finally {

                // This will help when thread pool executor is used
                ThreadContext.clearStack();

                // VT: FIXME: See if the memory leaks now, if it does, figure out the solution

                // Without this, memory leaks like a sieve in DZ3
                //NDC.remove();
            }
        }
    }

    /**
     * Check if the silent operation is supported <strong>and</strong>
     * implemented.
     *
     * @throws UnsupportedOperationException if the silent operation is either not supported or not implemented.
     */
    private synchronized void checkSilencer() {

        Meta meta = getMeta();

        // This will throw the exception if it is not declared

        boolean silentSupport = meta.getFeature(META_SILENT);

        if (!silentSupport) {

            // Oh well...

            throw new UnsupportedOperationException("Silent operation is not supported");
        }

        // Then see if it is implemented

        if ( silencer == null ) {

            throw new UnsupportedOperationException("Silent operation seems to be supported, but not implemented");
        }
    }

    /**
     * Default behavior is not to support silent operation.
     */
    @Override
    public void setSilentMode(boolean silent) throws IOException {

        checkInit();
        checkSilencer();

        boolean oldMode = getSilentMode();

        silencer.setSilentMode(silent);

        if ( silent != oldMode ) {

            silentStatusChanged(isSilentNow());
        }

        touch();
    }

    /**
     * Update the silent helper timestamp.
     *
     * This method is critical to properly support the silent mode. It
     * should be called every time the operation that should keep the
     * servo energized for some more ({@link #setPosition(double)}) is performed.
     */
    protected final void touch() {

        if ( silencer != null ) {

            silencer.touch(enabled);
        }
    }

    @Override
    public void setSilentTimeout(long timeout, long heartbeat) {

        checkInit();

        checkSilencer();

        silencer.setSilentTimeout(timeout, heartbeat);
    }

    @Override
    public final boolean getSilentMode() {

        checkInit();

        // Blow up if we don't support it
        getMeta().getFeature(META_SILENT);

        return (silencer == null) ? false : silencer.getSilentMode();
    }

    @Override
    public final boolean isSilentNow() {

        checkInit();

        // Blow up if we don't support it
        getMeta().getFeature(META_SILENT);

        return (silencer == null) ? false : silencer.isSilentNow();
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
        public Future<TransitionStatus> setPosition(double position) {

            ThreadContext.push("wrap");

            try {

                AbstractServo.this.setActualPosition(position);
                return new Done(null);

            } catch (IOException ex) {

                return new Done(ex);

            } finally {
                ThreadContext.pop();
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

        @Override
        public void open() {
            // Do absolutely nothing
        }

        @Override
        public int compareTo(Servo o) {
            return getName().compareTo(o.getName());
        }

        @Override
        public void close() throws IOException {
            // Do absolutely nothing
        }
    }

    /**
     * This future has already come.
     *
     * This object is returned by {@link AbstractServo#setPosition(double)} in absence of a transition controller.
     */
    public static class Done implements Future<TransitionStatus> {

        private final TransitionStatus done;

        /**
         * @param cause {@code null} if transition has completed successfully, and the failure cause otherwise.
         */
        public Done(Throwable cause) {

            long authToken = rg.nextLong();

            done = new TransitionStatus(authToken);
            done.complete(authToken, cause);
        }

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
         * @return successful transition status.
         */
        @Override
        public TransitionStatus get() throws InterruptedException, ExecutionException {
            return done;
        }

        /**
         * @return successful transition status.
         */
        @Override
        public TransitionStatus get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return done;
        }
    }

    /**
     * Notify the listeners about the change in the silent status.
     *
     * @param mode The new silent status. {@code false} means device is
     * sleeping, {@code true} means device is active.
     */
    protected final void silentStatusChanged(boolean mode) {

        for (Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext();) {

            ServoListener l = i.next();

            broadcaster.execute(new RunnableWrapper(logger,"silentStatusChanged") {

                @Override
                protected void doRun() {

                    l.silentStatusChanged(AbstractServo.this, mode);
                }
            });
        }
    }

    /**
     * Notify the listeners about the problem that occurred.
     *
     * @param t The exception to broadcast.
     */
    protected final void exception(Throwable t) {

        for ( Iterator<ServoListener> i = listenerSet.iterator(); i.hasNext(); ) {

            ServoListener l = i.next();

            broadcaster.execute(new RunnableWrapper(logger,"exception") {

                @Override
                protected void doRun() {

                    l.exception(AbstractServo.this, t);
                }
            });
        }
    }

    /**
     * Put the servo to sleep.
     *
     * @throws IOException if there was a problem communicating with the hardware controller.
     */
    protected void sleep() throws IOException {

        // Do absolutely nothing
        logger.debug("sleep: not implemented");
    }

    /**
     * Wake up the servo.
     *
     * @throws IOException if there was a problem communicating with the hardware controller.
     */
    protected void wakeUp() throws IOException {

        // Do absolutely nothing
        logger.debug("wakeUp: not implemented");
    }

    @Override
    public void close() throws IOException {

        if (silencer != null) {
            silencer.interrupt();
        }

        transitionDriverExecutor.shutdownNow();

        if (getMeta().getFeatures().containsKey(Feature.SILENT.name) && getMeta().getFeature(Feature.SILENT.name)) {

            // Instruct the servo to go to sleep directly, we won't be using it anymore
            sleep();
        }

        // We may get lucky and send notifications out before it's too late
        broadcaster.shutdown();

        initialized = false;
    }

    /**
     * The reason for existence of this class is that {@link AbstractServo#sleep()} and {@link AbstractServo#wakeUp()}
     * operations can't be exposed via implemented interface without violating the target integrity.
     */
    private class ServoSilencer extends Silencer {

        protected ServoSilencer(long timeout, long heartbeat) {
            super(timeout, heartbeat);
        }

        @Override
        public void sleep() {

            ThreadContext.push("sleep");

            try {

                AbstractServo.this.sleep();
                AbstractServo.this.silentStatusChanged(false);

            } catch (IOException ioex) {

                AbstractServo.this.exception(ioex);

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void wakeUp() {

            ThreadContext.push("wakeUp");

            try {

                AbstractServo.this.wakeUp();
                AbstractServo.this.silentStatusChanged(true);

            } catch (IOException ioex) {

                AbstractServo.this.exception(ioex);

            } finally {
                ThreadContext.pop();
            }
        }
    }

    @Override
    public int compareTo(Servo o) {
        return getName().compareTo(o.getName());
    }
}
