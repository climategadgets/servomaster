package org.freehold.servomaster.device.model;

import org.freehold.servomaster.device.model.AbstractMeta;
import org.freehold.servomaster.device.model.AbstractServo;
import org.freehold.servomaster.device.model.Meta;
import org.freehold.servomaster.device.model.ServoController;

/*
 * A hardware servo abstraction.
 *
 * Supports properties and operations specific to hardware servos, as
 * opposed to positioning and transition abstractions provided by {@link
 * AbstractServo AbstractServo}.
 *
 * <p>
 *
 * Note that this class doesn't support the {@link AbstractServo#target
 * target} directly.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: HardwareServo.java,v 1.1 2005-02-03 06:52:00 vtt Exp $
 */
abstract public class HardwareServo extends AbstractServo {

    /**
     * Servo identifier.
     */
    protected final int id;
    
    /**
     * Servo metadata.
     */
    private final Meta meta;
    
    /**
     * Create an instance.
     *
     * @param servoController The controller this servo belongs to.
     *
     * @param id Hardware specific servo identifier.
     */
    public HardwareServo(ServoController servoController, int id) {
    
        super(servoController, null);
        
        this.id = id;
        
        meta = createMeta();
    }
    
    /**
     * Get the servo name.
     *
     * @return Servo name.
     */
    public final String getName() {
    
        return Integer.toString(id);
    }
    
    /**
     * Create a metadata instance.
     *
     * @return A class specific metadata instance.
     */
    abstract protected Meta createMeta();
    
    /**
     * Get a metadata instance.
     *
     * @return Servo metadata.
     */
    public final Meta getMeta() {
    
        return meta;
    }
    
}
