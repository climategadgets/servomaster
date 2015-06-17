package net.sf.servomaster.device.model;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sf.servomaster.util.ImmutableIterator;

/**
 * Abstract metadata.
 *
 * The keys in the {@link #features feature map} and {@link #properties
 * property map} are short names.
 *
 * <p>
 *
 * The feature and property values are extracted ({@link #getFeature
 * getFeature()} and {@link #getProperty getProperty()} as the map values
 * ({@link #features feature map}, {@link #properties property map}),
 * however, it is only possible to store the values into the corresponding
 * maps if the handlers succeed. The handlers must be put into the
 * corresponding maps ({@link #featureWriters featureWriters}, {@link
 * #propertyWriters propertyWriters}) in the derived class constructor.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public abstract class AbstractMeta implements Meta {

    public static final String metaPrefix = "http://servomaster.sourceforge.net/meta/";

    /**
     * Feature map.
     */
    protected Map<String, Boolean> features = new TreeMap<String, Boolean>();

    /**
     * Property map.
     */
    protected Map<String, Object> properties = new TreeMap<String, Object>();

    /**
     * Feature writer map.
     */
    protected Map<String, FeatureWriter> featureWriters = new TreeMap<String, FeatureWriter>();

    /**
     * Property map.
     */
    protected Map<String, PropertyWriter> propertyWriters = new TreeMap<String, PropertyWriter>();

    public final Iterator<String> getFeatures() {

        return new ImmutableIterator<String>(features.keySet().iterator());
    }

    public final Iterator<String> getProperties() {

        return new ImmutableIterator<String>(properties.keySet().iterator());
    }

    public final synchronized boolean getFeature(String id) {

        if ( id.startsWith(metaPrefix) ) {

            return getFeature(id.substring(metaPrefix.length()));
        }

        if ( !features.containsKey(id) ) {

            throw new UnsupportedOperationException("No feature '" + id + "'");
        }

        Object value = features.get(id);

        if ( value instanceof Boolean ) {

            return ((Boolean)value).booleanValue();

        } else {

            // This is bad, it shouldn't have happened

            throw new IllegalStateException("Bad data for key '" + id + "', class is " + value.getClass().getName());
        }
    }

    public final synchronized Object getProperty(String id) {

        if ( id.startsWith(metaPrefix) ) {

            return getProperty(id.substring(metaPrefix.length()));
        }

        if ( !properties.containsKey(id) ) {

            throw new UnsupportedOperationException("No property '" + id + "'");
        }

        return properties.get(id);
    }

    public final synchronized void setFeature(String id, boolean value) {

        FeatureWriter w = featureWriters.get(id);

        if ( w == null ) {

            throw new UnsupportedOperationException("Can't set feature '" + id + "' - don't have a writer");
        }

        w.set(id, value);

        // Now that we've succeeded, we can store the value into the map

        features.put(id, new Boolean(value));
    }

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

    protected abstract class FeatureWriter {

        public abstract void set(String key, boolean value);
    }

    protected abstract class PropertyWriter {

        public abstract void set(String key, Object value);
    }
}
