package net.sf.servomaster.device.impl;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

public abstract class RunnableWrapper implements Runnable {

    private final Logger logger;
    private final String marker;
    
    public RunnableWrapper(Logger logger, String marker) {
        
        this.logger = logger;
        this.marker = marker;
    }
    
    @Override
    public void run() {
        
        NDC.push(marker);
        
        try {
            
            doRun();
            
        } catch (Throwable t) {
            
            // There's nothing we can do other than complain
            logger.error("unhandled exception", t);
            
        } finally {
            
            NDC.pop();
            NDC.clear();
            NDC.remove();
        }
    }

    protected abstract void doRun();
}
