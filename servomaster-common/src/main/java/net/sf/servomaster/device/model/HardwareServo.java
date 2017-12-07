package net.sf.servomaster.device.model;

/**
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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
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
    @Override
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
    @Override
    public final Meta getMeta() {

        return meta;
    }

}
