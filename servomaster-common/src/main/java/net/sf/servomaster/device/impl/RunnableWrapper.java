package net.sf.servomaster.device.impl;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public abstract class RunnableWrapper implements Runnable {

    private final Logger logger;
    private final String marker;
    
    public RunnableWrapper(Logger logger, String marker) {
        
        this.logger = logger;
        this.marker = marker;
    }
    
    @Override
    public void run() {
        
        ThreadContext.push(marker);
        
        try {
            
            doRun();
            
        } catch (Throwable t) {
            
            // There's nothing we can do other than complain
            logger.error("unhandled exception", t);
            
        } finally {
            
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    protected abstract void doRun();
}
