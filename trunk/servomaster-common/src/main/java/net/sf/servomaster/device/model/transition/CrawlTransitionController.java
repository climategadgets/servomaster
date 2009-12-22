package net.sf.servomaster.device.model.transition;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.TransitionController;
import net.sf.servomaster.device.model.TransitionToken;

/**
 * Makes the servo crawl as fast as the servo controller and I/O allows,
 * incrementing or decrementing position one step at a time, with no regard
 * to the timing.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class CrawlTransitionController implements TransitionController {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Exists to make {@code Class.newInstance()} happy.
     */
    public CrawlTransitionController() {

    }

    public void move(Servo target, TransitionToken token, double targetPosition) {

        if ( target == null || token == null ) {

            throw new IllegalArgumentException("Neither target nor token can be null");
        }

        NDC.push("move");
        
        try {

            // Calculate the step
            // VT: FIXME: this may throw UnsupportedOperationException

            final int precision = Integer.parseInt(target.getMeta().getProperty("servo/precision").toString());
            final double step = 1/(double)(precision - 1);
            
            logger.debug("precision=" + precision + ", step=" + step);

            while ( true ) {

                double actualPosition = target.getActualPosition();
                double diff = targetPosition - actualPosition;

                if ( diff < 0 ) {

                    diff = -diff;
                }

                if ( diff <= step/2 ) {

                    // We came close enough

                    token.stop();
                    return;
                }

                double newPosition = 0;

                if ( actualPosition > targetPosition ) {

                    newPosition = actualPosition - step;

                } else {

                    newPosition = actualPosition + step;
                }

                if ( newPosition < 0.0 || newPosition > 1.0 ) {

                    // We hit the limit

                    token.stop();
                    return;
                }

                token.supply(newPosition);
            }

        } catch ( IllegalStateException ex ) {

            logger.debug("Ignored, stopping", ex);

            token.stop();

        } catch ( Throwable t ) {

            // If we haven't caught it, we didn't think about it

            logger.error("Unexpected exception, stopping", t);

            token.stop();
            
        } finally {
            
            NDC.pop();
        }
    }
}
