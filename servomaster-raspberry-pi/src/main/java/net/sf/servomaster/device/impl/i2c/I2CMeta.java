package net.sf.servomaster.device.impl.i2c;

import net.sf.servomaster.device.impl.AbstractMeta;

/**
 * I2C controller metadata.
 *
 * <p>
 *
 * All I2C controllers share at least two features: they are not
 * disconnectable and they support {@code controller/protocol/i2c}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public abstract class I2CMeta extends AbstractMeta {

    protected I2CMeta() {

        features.put("controller/allow_disconnect", Boolean.valueOf(false));
        features.put("controller/protocol/i2c", Boolean.valueOf(true));
    }
}
