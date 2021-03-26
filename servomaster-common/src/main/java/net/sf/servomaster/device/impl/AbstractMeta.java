package net.sf.servomaster.device.impl;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.servomaster.device.model.Meta;

/**
 * Basic metadata support.
 *
 * Implementation note: tree based structures are generally slower than hash based, but these collections
 * are small and infrequently accessed - and when they are, it's most often for human eyes, which like things sorted.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractMeta implements Meta {

    public static final String metaPrefix = "http://servomaster.sourceforge.net/meta/";

    /**
     * Features.
     *
     * The key is the feature supported, and the value is the current value of it.
     */
    protected SortedMap<String, Boolean> features = new TreeMap<String, Boolean>();

    /**
     * Property map.
     */
    protected SortedMap<String, Object> properties = new TreeMap<String, Object>();

    /**
     * Feature writer map.
     */
    protected Map<String, FeatureWriter> featureWriters = new TreeMap<String, FeatureWriter>();

    /**
     * Property writer map.
     */
    protected Map<String, PropertyWriter> propertyWriters = new TreeMap<String, PropertyWriter>();

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

        if ( id.startsWith(metaPrefix) ) {

            return getFeature(id.substring(metaPrefix.length()));
        }

        if ( !features.containsKey(id) ) {

            throw new UnsupportedOperationException("No feature '" + id + "'");
        }

        return features.get(id);
    }

    @Override
    public final synchronized Object getProperty(String id) {

        if ( id.startsWith(metaPrefix) ) {

            return getProperty(id.substring(metaPrefix.length()));
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

        features.put(id, Boolean.valueOf(value));
    }

    @Override
    public final synchronized void setProperty(String id, Object value) {

        PropertyWriter w = propertyWriters.get(id);

        if ( w == null ) {

            throw new UnsupportedOperationException("Can't set property '" + id + "' - don't have a writer");
        }

        w.set(id, value);

        // Now that we've succeeded, we can store the value into the map

        properties.put(id, value);
    }
    
    @Override
    public synchronized String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("[features: ").append(features);
        sb.append(", properties: ").append(properties);
        sb.append("]");
        
        return sb.toString();
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
