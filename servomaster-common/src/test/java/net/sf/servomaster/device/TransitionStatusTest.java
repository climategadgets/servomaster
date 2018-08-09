package net.sf.servomaster.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import net.sf.servomaster.device.model.TransitionStatus;

public class TransitionStatusTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAuth() {
        
        TransitionStatus target = new TransitionStatus(0);
        
        thrown.expect(IllegalAccessError.class);
        thrown.expectMessage("invalid token, refusing to set status");

        target.complete(1, null);
    }
    
    @Test
    public void testCompletion() {
        
        TransitionStatus target = new TransitionStatus(0);
        
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("haven't completed yet");

        target.isOK();
    }
    
    @Test
    public void testPass() {
        
        TransitionStatus target = new TransitionStatus(0);
        
        target.complete(0, null);
        
        assertTrue(target.isOK());
        assertEquals(null, target.getCause());
    }
    
    @Test
    public void testFail() {
        
        TransitionStatus target = new TransitionStatus(0);
        Throwable t = new IllegalStateException();
        
        target.complete(0, t);
        
        assertFalse(target.isOK());
        assertEquals(t, target.getCause());
    }
}
