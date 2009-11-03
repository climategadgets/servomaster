package net.sf.servomaster.device.model;

/**
 * Allows to track the servo position changes.
 *
 * <h3>Implementation note</h3>
 *
 * Tracking may be expensive for the servo implementation. Be sure to make
 * sending notifications asynchronous from the servo operation itself.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2005
 * @version $Id: ServoListener.java,v 1.4 2006-12-14 09:17:10 vtt Exp $
 */
public interface ServoListener {

    /**
     * Accept the requested servo position change notification.
     *
     * <p>
     *
     * The requested position may be different from the actual position if
     * the servo is operating in a smooth mode.
     *
     * @param source The servo whose position has changed.
     *
     * @param position New position.
     */
    void positionChanged(Servo source, double position);

    /**
     * Accept the actual servo position change notification.
     *
     * @param source The servo whose position has changed.
     *
     * @param position New position.
     */
    void actualPositionChanged(Servo source, double position);
}
