package net.sf.servomaster.device.model;

/**
 * Problem listener.
 *
 * This interface allows to receive notifications about the exceptions
 * thrown in the places where it is not possible to properly handle, rethrow
 * and/or log them.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2009
 */
public interface ProblemListener {

    /**
     * Accept the notification about the exception.
     *
     * @param source Object that has a problem.
     *
     * @param t The exception.
     */
    void exception(Object source, Throwable t);
}
