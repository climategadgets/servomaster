package org.freehold.servomaster.device.model.transition;

import org.freehold.servomaster.device.model.Servo;
import org.freehold.servomaster.device.model.TransitionController;
import org.freehold.servomaster.device.model.TransitionToken;

/**
 * Makes the servo crawl as fast as the servo controller and I/O allows,
 * incrementing or decrementing position one step at a time, with no regard
 * to the timing.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001
 * @version $Id: CrawlTransitionController.java,v 1.2 2002-01-03 02:56:19 vtt Exp $
 */
public class CrawlTransitionController implements TransitionController {

    public CrawlTransitionController() {
    
    }
    
    public void move(Servo target, TransitionToken token, double targetPosition) {
    
        if ( target == null || token == null ) {
        
            throw new IllegalArgumentException("Neither target nor token can be null");
        }
        
        try {
        
            // Calculate the step
            // VT: FIXME: this may throw UnsupportedOperationException
            
            final int precision = target.getController().getMetaData().getPrecision();
            final double step = 1/(double)(precision - 1);
        
            while ( true ) {
            
                double actualPosition = target.getActualPosition();
                double diff = targetPosition - actualPosition;
                
                if ( diff < 0 ) {
                
                    diff = -diff;
                }
                
                if ( diff <= step/2 ) {
                
                    token.stop();
                    return;
                }
                
                if ( actualPosition > targetPosition ) {
                
                    token.supply(actualPosition - step);
                    
                } else {
                
                    token.supply(actualPosition + step);
                }
            
            }
        
        } catch ( IllegalStateException isex ) {
        
            //System.err.println("Transition:");
            //isex.printStackTrace();
            
            token.stop();

        } catch ( Throwable t ) {
        
            // If we haven't caught it, we didn't think about it
            
            System.err.println("Transition:");
            t.printStackTrace();
            
            token.stop();
        }
    }
}
