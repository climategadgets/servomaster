package org.freehold.servomaster.device.model.silencer;

import org.freehold.servomaster.device.model.SilentDevice;

/**
 * Provides the functionality required to support the silent mode.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: SilentHelper.java,v 1.2 2002-03-09 05:23:16 vtt Exp $
 */
public class SilentHelper extends Thread implements SilentDevice {

    /**
     * Device silence mode.
     *
     * Set to <code>true</code> if the silent mode is on. Default is off.
     * The reason for that is that the default silent timeout and heartbeat
     * are application specific and better be consciously set.
     */
    private boolean silent = false;
    
    /**
     * Silent timeout, in milliseconds.
     *
     * Defines how long the device stays active after the last operation in
     * the silent mode. When this time expires, the callback method is
     * called, thus putting the target device into "sleep" mode.
     *
     * <p>
     *
     * Default is 10 seconds.
     */
    private long timeout = 10000;
    
    /**
     * Silent heartbeat, in milliseconds.
     *
     * Defines how long the device stays deactivated after the silent
     * timeout expires.
     *
     * <p>
     *
     * Default is 5 minutes.
     */
    private long heartbeat = 1000 * 60 * 5;
    
    /**
     * Last time when the operation was performed.
     */
    private long lastOperation = System.currentTimeMillis();
    
    /**
     * The proxy object.
     *
     * Helps to activate and passivate the target.
     *
     * It is recommended that the proxy is implemented as an inner class,
     * inaccessible from outside the target.
     */
    private SilentProxy proxy;
    
    /**
     * State handler.
     *
     * @see SilentHelper.Active
     * @see SilentHelper.Passive
     */
    private StateHandler stateHandler = new Active();
    
    /**
     * @param proxy The proxy that controls the target device.
     */
    public SilentHelper(SilentProxy proxy) {
    
        this.proxy = proxy;
    }
    
    /**
     * Update the timestamp.
     */
    public synchronized void touch() {
     
        //complain("touch", null);
        
        lastOperation = System.currentTimeMillis();
        notifyAll();
    }
    
    public synchronized void setSilentMode(boolean silent) {
    
        this.silent = silent;
        notifyAll();
    }
    
    public synchronized void setSilentTimeout(long timeout, long heartbeat) {
    
        if ( timeout <= 0 ) {
        
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        if ( heartbeat < 0 ) {
        
            throw new IllegalArgumentException("Heartbeat must be positive");
        }
    
        this.timeout = timeout;
        this.heartbeat = heartbeat;
        
        notifyAll();
    }
    
    public boolean isSilentNow() {
    
        return (stateHandler instanceof Passive);
    }
    
    public boolean getSilentMode() {
    
        return silent;
    }
    
    /**
     * Keep watching the device.
     */
    public void run() {
    
        while ( true ) {
        
            //complain("run:" + silent, null);
            
            try {
            
                int s = silent ? 1 : 0;
                
                switch ( s ) {
                
                    case 0:
                    
                        // Not silent
                        
                        // We have nothing to do here but just wait.
                        
                        _wait();
                        break;
                        
                    case 1:
                    
                        // Silent
                        
                        //complain("wait:" + stateHandler.getClass().getName(), null);
                        
                        stateHandler.handleWait();
                }
                
            } catch ( InterruptedException iex ) {
            
                // Most probably, we were stopped
                
                complain("Interrupted", iex);
                return;
            
            } catch ( Throwable t ) {
            
                complain("Screwed up, ignored:", t);
            }
        }
    }
    
    private synchronized void _wait() throws InterruptedException {
    
        //complain("_wait", null);
        wait();
    }

    private synchronized void _wait(long millis) throws InterruptedException {
    
        //complain("_wait(" + millis + ")", null);
        wait(millis);
    }
    
    private synchronized void complain(String message, Throwable t) {
    
        if ( message != null ) {
        
            System.err.println(message);
        }
        
        if ( t != null ) {
        
            t.printStackTrace();
        }
    }
    
    abstract protected class StateHandler {
    
        /**
         * Wait until wait is over and handle the state.
         */
        abstract void handleWait() throws InterruptedException;
        
        /**
         * Calculate how much time is left to wait.
         */
        abstract long left();
    }
    
    protected class Active extends StateHandler {
    
        void handleWait() throws InterruptedException {
        
            long left = left();
            
            if ( left <= 0 ) {
            
                return;
            }
        
            _wait(left);
            
            if ( !silent ) {
            
                // Damn!
                
                return;
            }
            
            if ( left() <= 0 ) {
            
                // VT: FIXME: Verify the order
                
                proxy.sleep();
                stateHandler = new Passive();
            }
        }
        
        long left() {
        
            return timeout - (System.currentTimeMillis() - lastOperation);
        }
    }

    protected class Passive extends StateHandler {
    
        void handleWait() throws InterruptedException {
        
            if ( heartbeat == 0 ) {
            
                _wait();
                
            } else {
            
                long left = left();
                
                if ( left <= 0 ) {
                
                    return;
                }
            
                _wait(left);
            }

            if ( !silent ) {
            
                // Damn!
                
                // Better wake them up
                
                proxy.wakeUp();
                
                return;
            }
            
            stateHandler = new Active();
            proxy.wakeUp();
            
            // Just in case
            
            touch();
        }

        long left() {
        
            return (timeout + heartbeat) - (System.currentTimeMillis() - lastOperation);
        }
    }
}
