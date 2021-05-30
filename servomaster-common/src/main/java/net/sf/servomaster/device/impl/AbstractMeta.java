package net.sf.servomaster.device.impl;

import net.sf.servomaster.device.model.Meta;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Basic metadata support.
 *
 * Implementation note: tree based structures are generally slower than hash based, but these collections
 * are small and infrequently accessed - and when they are, it's most often for human eyes, which like things sorted.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractMeta implements Meta {

    public static final String META_PREFIX = "http://servomaster.sourceforge.net/meta/";

    /**
     * Features.
     *
     * The key is the feature supported, and the value is the current value of it.
     */
    protected SortedMap<String, Boolean> features = new TreeMap<>();

    /**
     * Property map.
     */
    protected SortedMap<String, Object> properties = new TreeMap<>();

    /**
     * Feature writer map.
     */
    protected Map<String, FeatureWriter> featureWriters = new TreeMap<>();

    /**
     * Property writer map.
     */
    protected Map<String, PropertyWriter> propertyWriters = new TreeMap<>();

    @Override
    public final Map<String, Boolean> getFeatures() {

        return Collections.unmodifiableSortedMap(features);
    }

    @Override
    public final Map<String, Object> getProperties() {

        return Collections.unmodifiableSortedMap(properties);
    }

    @Override
    public final synchronized boolean getFeature(String id) {

        if ( id.startsWith(META_PREFIX) ) {

            return getFeature(id.substring(META_PREFIX.length()));
        }

        if ( !features.containsKey(id) ) {

            throw new UnsupportedOperationException("No feature '" + id + "'");
        }

        return features.get(id);
    }

    @Override
    public final synchronized Object getProperty(String id) {

        if ( id.startsWith(META_PREFIX) ) {
            return getProperty(id.substring(META_PREFIX.length()));
        }

        if ( !properties.containsKey(id) ) {
            throw new UnsupportedOperationException("No property '" + id + "'");
        }

        return properties.get(id);
    }

    @Override
    public final synchronized void setFeature(String id, boolean value) {

        FeatureWriter w = featureWriters.get(id);

        if ( w == null ) {
            throw new UnsupportedOperationException("Can't set feature '" + id + "' - don't have a writer");
        }

        w.set(id, value);

        // Now that we've succeeded, we can store the value into the map

        features.put(id, value);
    }

    @Override
    public final synchronized void setProperty(String id, Object value) {

        var w = propertyWriters.get(id);

        if ( w == null ) {
            throw new UnsupportedOperationException("Can't set property '" + id + "' - don't have a writer");
        }

        w.set(id, value);

        // Now that we've succeeded, we can store the value into the map
        properties.put(id, value);
    }

    @Override
    public synchronized String toString() {
        return "[features: " + features + ", properties: " + properties + "]";
    }

    /**
     * Setting feature values will most probably involve talking to hardware, and changing driver internal state.
     * This interface is the entry point for that.
     */
    protected interface FeatureWriter {
        void set(String key, boolean value);
    }

    /**
     * Setting property values will most probably involve talking to hardware, and changing driver internal state.
     * This interface is the entry point for that.
     */
    protected interface PropertyWriter {
        void set(String key, Object value);
    }
}
