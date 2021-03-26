package net.sf.servomaster.device.model;

import java.util.concurrent.Future;

/**
 * Status of {@link Servo#setPosition(double)} operation returned as a {@link Future} payload.
 *
 * In the future, will be also returned by {@link TransitionController#move(Servo, double)}
 * (see https://github.com/climategadgets/servomaster/issues/30 for details).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public class TransitionStatus {

    /**
     * A primitive tamper protection mechanism.
     *
     * Unless a correct token is presented, the status of this object can't be changed, only queried.
     */
    private final long authToken;

    private Boolean ok = null;
    private Throwable cause = null;

    public TransitionStatus(long authToken) {

        this.authToken = authToken;
    }

    /**
     * @return {@code true} if the transition has completed successfully, {@code false} otherwise.
     * 
     * @throws IllegalStateException if the operation is not yet complete.
     */
    public synchronized boolean isOK() {

        checkCompletion();

        return ok;
    }

    /**
     * @return {@code null} if transition has completed successfully, and the failure cause otherwise.
     * 
     * @throws IllegalStateException if the operation is not yet complete.
     */
    public synchronized Throwable getCause() {

        checkCompletion();

        return cause;
    }

    /**
     * Check whether the transition is complete.
     * 
     * This method is intentionally private - the caller has no business polling the status,
     * they're supposed to deal with it in an asynchronous way.
     * 
     * @throws IllegalStateException if the operation is not yet complete.
     */
    private void checkCompletion() {

        if (ok == null) {
            throw new IllegalStateException("haven't completed yet");
        }
    }

    /**
     * Set the completion status.
     * 
     * {@code complete(token, null)} automatically sets {@link #ok} to {@code true}.
     * 
     * @param authToken See {@link #authToken}.
     * @param cause {@code null} if transition has completed successfully, and the failure cause otherwise.
     * 
     * @throws IllegalAccessError if the token presented is not the one this object was instantiated with.
     */
    public synchronized void complete(long authToken, Throwable cause) {

        if (this.authToken != authToken) {
            throw new IllegalAccessError("invalid token, refusing to set status");
        }

        ok = cause == null;
        this.cause = cause;
    }
}
