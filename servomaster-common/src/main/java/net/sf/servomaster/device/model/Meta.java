package net.sf.servomaster.device.model;

import java.util.Map;

import net.sf.servomaster.device.impl.AbstractMeta;

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
 * An example of a feature: whether the controller is able to support the silent
 * mode.
 *
 * An example of a property: how long the controller will stay inactive before
 * going into silent mode.
 *
 * <p>
 *
 * If the feature is not supported or the property is not present, the attempt
 * to get or set it will result in {@code UnsupportedOperationException}. If the
 * feature or property value can't be changed because it's read-only, the
 * attempt to set it will result in {@code IllegalAccessError}. For hardware
 * related problems, {@code IOException} will be thrown.
 *
 * <p>
 *
 * The feature or property identifier is normally a full or partial URL. The
 * full URL points to the page containing the support documentation for this
 * feature or property, for example,
 * <a href="http://servomaster.sourceforge.net/meta/controller/precision" target
 * ="_top">http://servomaster.sourceforge.net/meta/controller/precision</a>.
 * Since this is quite cumbersome, partial URLs will be accepted as well. For
 * this particular example, the identifier will look like
 * {@code controller/precision}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2002-2018
 */
public interface Meta {

    /**
     * Get supported features.
     *
     * @return Immutable map of feature names to their current values.
     */
    Map<String, Boolean> getFeatures();

    /**
     * Get supported properties.
     *
     * @return Immutable map of property names to property values.
     */
    Map<String, Object> getProperties();

    /**
     * Look up the value of the feature.
     *
     * @param id Feature identifier.
     *
     * @return the value of the feature.
     *
     * @throws UnsupportedOperationException if this feature is not supported.
     *
     * @see #supportsFeature(String)
     */
    boolean getFeature(String id);

    /**
     * Set the feature.
     *
     * This method is a bit more complicated than it seems - see
     * {@link AbstractMeta#setFeature(String, boolean)} for details.
     *
     * @param id Feature name.
     *
     * @param value Feature value.
     *
     * @throws UnsupportedOperationException if this feature is not supported.
     * @throws IllegalAccessError if this feature is read only.
     */
    void setFeature(String id, boolean value);

    /**
     * Get the property.
     *
     * @param id Property name.
     *
     * @return the property value.
     *
     * @throws UnsupportedOperationException if this property is not supported.
     */
    Object getProperty(String id);

    /**
     * Set the property.
     *
     * @param id Feature name.
     *
     * @param value Feature value.
     *
     * @throws UnsupportedOperationException if this property is not supported.
     * @throws IllegalAccessError if this property is read only.
     */
    void setProperty(String id, Object value);
}
