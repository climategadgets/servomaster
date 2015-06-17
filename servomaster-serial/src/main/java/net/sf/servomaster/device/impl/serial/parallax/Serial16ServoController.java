package net.sf.servomaster.device.impl.serial.parallax;

import java.io.IOException;

import net.sf.servomaster.device.model.Meta;

/**
 * <a href="http://parallax.com/" target="_top">Parallax</a> controller.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 * @author Copyright &copy; Scott L'Hommedieu 2006
 */
public class Serial16ServoController extends ParallaxSerialServoController {

    public Serial16ServoController() {

        // Can't invoke this(null) because this will blow up in doInit()
    }

    public Serial16ServoController(String portName) throws IOException {

        super(portName);
    }

    /**
     * {@inheritDoc}
     */
    public int getServoCount() {

        return 16;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Meta createMeta() {

        return new Serial16Meta();
    }

    protected class Serial16Meta extends ParallaxMeta {

        protected Serial16Meta() {

            properties.put("manufacturer/model", "Serial 16-Servo");
        }
    }
}