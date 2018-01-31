package net.sf.servomaster.device.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Provides the functionality required to support the silent mode.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2018
 */
public abstract class Silencer extends Thread {

    protected Logger logger = Logger.getLogger(getClass());

    /**
     * Device silence mode.
     *
     * Set to {@code true} if the silent mode is on. Default is off. The reason for
     * that is that the default silent timeout and heartbeat are application
     * specific and better be consciously set.
     */
    private boolean enabled = false;

    /**
     * Silent timeout, in milliseconds.
     *
     * Defines how long the device stays active after the last operation in
     * the silent mode. When this time expires, the callback method is
     * called, thus putting the target device into "sleep" mode.
     */
    private long timeout;

    /**
     * Silent heartbeat, in milliseconds.
     *
     * Defines how long the device stays deactivated after the silent
     * timeout expires.
     */
    private long heartbeat;

    /**
     * Moment in time when the target has to be shut off.
     */
    private Long silenceAt = null;

    /**
     * Moment in time when the target has to be woken up.
     */
    private Long heartbeatAt = null;

    /**
     * Thread pool for {@link #commandSleep()} and {@link #commandWakeUp().
     *
     * This pool requires exactly one thread.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    protected Silencer(long timeout, long heartbeat) {

        this.timeout = timeout;
        this.heartbeat = heartbeat;
    }

    public synchronized void setSilentMode(boolean enabled) {

        if (this.enabled == enabled) {

            // do nothing
            return;
        }

        this.enabled = enabled;

        if (!enabled) {

            // Alas, we need to wake them up
            wakeUp();

            silenceAt = null;
            heartbeatAt = null;

            // Notify run() about changes
            notify();
            return;
        }

        silenceAt = System.currentTimeMillis() + timeout;

        // Notify run() about changes
        notify();
    }

    public synchronized boolean getSilentMode() {
        return enabled;
    }

    public synchronized void setSilentTimeout(long timeout, long heartbeat) {

        // VT: FIXME: Check argument sanity

        this.timeout = timeout;
        this.heartbeat = heartbeat;

        // This recalculates the wait intervals
        touch();
    }

    public synchronized boolean isSilentNow() {

        return enabled && heartbeatAt != null;
    }

    public synchronized void touch() {

        if (!enabled) {

            // nothing to do
            return;
        }

        if (silenceAt != null) {

            // We were active, let's keep it that way
            silenceAt = System.currentTimeMillis() + timeout;

        } else {

            // We were sleeping, time to wake up
            heartbeatAt = System.currentTimeMillis();
        }

        notify();
    }

    @Override
    public synchronized final void run() {

        NDC.push("run");

        try {

            logger.info("hello " + this);

            while (true) {

                try {

                    if (!enabled) {

                        logger.debug("not silent, waiting indefinitely");
                        wait();

                        // Wait is over, let's see what's going on
                        continue;
                    }

                    long now = System.currentTimeMillis();

                    // Only one of silenceAt and heartbeatAt can be not null at any time

                    long deadline = silenceAt != null ? silenceAt : heartbeatAt;

                    // We were waiting, is the wait over?

                    if (now >= deadline) {

                        if (silenceAt != null) {

                            silenceAt = null;
                            heartbeatAt = System.currentTimeMillis() + heartbeat;

                            commandSleep();

                        } else {

                            silenceAt = System.currentTimeMillis() + timeout;
                            heartbeatAt = null;

                            commandWakeUp();
                        }

                        // Back to wait
                        continue;
                    }

                    // Hmm, wait is not over... Must've been touch()ed

                    // We don't care much that it is inexact
                    long interval = deadline - System.currentTimeMillis();

                    logger.debug("waiting " + interval + "ms " + (silenceAt != null ? "to sleep" : "for heartbeat"));

                    wait(interval);

                } catch (InterruptedException ex) {

                    // Most probably, we were stopped
                    return;
                }
            }

        } finally {

            logger.debug("stopped");

            executor.shutdownNow();

            NDC.pop();
            NDC.clear();
            NDC.remove();
        }
    }

    private void commandSleep() {

        // Calling sleep() directly *will* cause a deadlock on a controller

        executor.execute(new RunnableWrapper(logger, "commandSleep") {

            @Override
            protected void doRun() {

                sleep();
            }
        });
    }

    private void commandWakeUp() {

        // Calling wakeUp() directly *will* cause a deadlock on a controller

        executor.execute(new RunnableWrapper(logger, "commandWakeUp") {

            @Override
            protected void doRun() {

                wakeUp();
            }
        });
    }

    abstract protected void sleep();
    abstract protected void wakeUp();
}
