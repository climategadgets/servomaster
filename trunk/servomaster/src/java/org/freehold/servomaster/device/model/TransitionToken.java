package org.freehold.servomaster.device.model;

/**
 * Exchange between the {@link AbstractServo AbstractServo} and {@link
 * TransitionController TransitionController}.
 *
 * The servo instantiates the token and passes it to the transition
 * controller. The transition controller supplies the coordinates following
 * its pattern, and checks if the transition was aborted. The servo consumes
 * the values produced and watches for "end-of-transition" signal.
 *
 * <p>
 *
 * The existence of this class is necessitated by the fact that the
 * coordinates produced by the transition controller may depend on the
 * system timing and delays.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: TransitionToken.java,v 1.2 2002-01-02 09:18:52 vtt Exp $
 */
public class TransitionToken {

    private Double position = null;
    private boolean done = false;
    
    public TransitionToken() {
    
    }
    
    /**
     * Supply the calculated position.
     *
     * @param position The next position on the transition path.
     *
     * @exception IllegalStateException when the transition is {@link #done
     * done} because the consumer {@link #stop aborted} it.
     */
    public synchronized void supply(double position) throws InterruptedException {
    
        while ( this.position != null && !done ) {
        
            wait();
        }
        
        if ( done ) {
        
            throw new IllegalStateException("Transition is over");
        }
        
        this.position = new Double(position);
        
//        System.err.println("Supply: " + this.position);
        
        notifyAll();
    }
    
    /**
     * Consume the calculated position.
     *
     * @return The next position on the transition path.
     *
     * @exception IllegalStateException when the transition is {@link #done
     * done} because the supplier {@link #stop completed} the transition.
     */
    public synchronized double consume() throws InterruptedException {
    
        while ( position == null && !done ) {
        
            wait();
        }
        
        if ( done ) {
        
            throw new IllegalStateException("Transition is over");
        }
        
        double result = position.doubleValue();
        
        position = null;
        
//        System.err.println("Consume: " + result);
        
        notifyAll();
        
        return result;
    }
    
    /**
     * Bidirectional stop.
     *
     * If invoked by the consumer, the supplier will stop at the next
     * attempt to supply the data. If invoked by the supplier, the consumer
     * will consider the transition done.
     */
    public synchronized void stop() {
    
        done = true;
        
        notifyAll();
    }
}