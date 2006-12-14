package org.freehold.servomaster.device.model;

/**
 * Problem listener.
 *
 * This interface allows to receive notifications about the exceptions
 * thrown in the places where it is not possible to properly handle, rethrow
 * and/or log them.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2005
 * @version $Id: ProblemListener.java,v 1.2 2006-12-14 09:17:10 vtt Exp $
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
