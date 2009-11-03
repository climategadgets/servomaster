package net.sf.servomaster.device.model;

/**
 * A token that can be used to track the completion of {@link
 * TransitionController transitioned} {@link Servo#setPosition servo
 * movement}, either {@link #waitFor synchronously} or {@link #isComplete
 * asynchronously}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public interface TransitionCompletionToken {

    /**
     * Check if the transition is complete.
     *
     * @return <code>true</code> if transition is complete,
     * <code>false</code> otherwise.
     */
    public boolean isComplete();

    /**
     * Wait for the transition to complete.
     */
    public void waitFor() throws InterruptedException;

    /**
     * Wait for either timeout expiration or transition completion.
     *
     * @param millis Time to wait.
     */
    public void waitFor(long millis) throws InterruptedException;
}
