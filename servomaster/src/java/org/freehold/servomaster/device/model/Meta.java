package org.freehold.servomaster.device.model;

import java.io.IOException;
import java.util.Iterator;

/**
 * Describes an object capable of providing and adjusting the metadata.
 *
 * <p>
 *
 * Two flavors of metadata are supported: feature and property.
 *
 * <p>
 *
 * The feature is something that is either present or absent. If it is
 * supported, it can be switched on or off.
 *
 * <p>
 *
 * The property is something that can be measured, described and possibly
 * changed.
 *
 * <p>
 *
 * An example of a feature: whether the controller is able to support the
 * silent mode.
 *
 * An example of a property: how long the controller will stay inactive
 * before going into silent mode.
 *
 * <p>
 *
 * If the feature is not supported or the property is not present, the
 * attempt to get or set it will result in
 * <code>UnsupportedOperationException</code>. If the feature or property
 * value can't be changed because it's read-only, the attempt to set it will
 * result in <code>IllegalAccessError</code>. For hardware related problems,
 * <code>IOException</code> will be thrown.
 *
 * <p>
 *
 * The feature or property identifier is normally a full or partial URL. The
 * full URL points to the page containing the support documentation for this
 * feature or property, for example, <a
 * href="http://servomaster.sourceforge.net/meta/controller/precision">http://servomaster.sourceforge.net/meta/controller/precision</a>.
 * Since this is quite cumbersome, partial URLs will be accepted as well.
 * For this particular example, the identifier will look like
 * <code>controller/precision</code>.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002
 * @version $Id: Meta.java,v 1.1 2002-09-21 00:23:09 vtt Exp $
 */
public interface Meta {

    /**
     * Get the iterator on the supported features.
     */
    public Iterator getFeatures();
    
    /**
     * Get the iterator on the supported properties.
     */
    public Iterator getProperties();

    /**
     * Look up the value of the feature.
     *
     * @param id Feature identifier.
     *
     * @return the value of the feature.
     *
     * @exception UnsupportedOperationException if this feature is not
     * supported.
     *
     * @exception IOException if there was an I/O error communicating with
     * the hardware.
     */
    public boolean getFeature(String id) throws IOException;
    
    public void setFeature(String id, boolean value) throws IOException;
    
    public Object getProperty(String id) throws IOException;
    
    public void setProperty(String id, Object value) throws IOException;
}
