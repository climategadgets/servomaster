package org.freehold.servomaster.device.impl.parallax;

import java.io.IOException;

import org.freehold.servomaster.device.model.Meta;

/**
 * <a href="http://pololu.com/products/pololu/0240/" target="_top">Pololu
 * Serial 16-Servo Controller</a> controller.
 *
 * Should also work with the <a
 * href="http://www.pololu.com/products/pololu/0290/"
 * target="_top">8-Servo</a> controller, but be careful not to access servos
 * with IDs of 8 and up.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005
 * @version $Id: Serial16ServoController.java,v 1.1 2006-12-14 12:38:28 vtt Exp $
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
    protected final Meta createMeta() {

        return new Serial16Meta();
    }

    protected class Serial16Meta extends ParallaxMeta {

        public Serial16Meta() {

            properties.put("manufacturer/model", "Serial 16-Servo");
        }
    }
}
