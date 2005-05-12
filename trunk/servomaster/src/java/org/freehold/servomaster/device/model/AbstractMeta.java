package org.freehold.servomaster.device.model;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.freehold.servomaster.util.ImmutableIterator;

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
 * @version $Id: AbstractMeta.java,v 1.3 2005-05-12 20:55:12 vtt Exp $
 */
abstract public class AbstractMeta implements Meta {

    public static final String metaPrefix = "http://servomaster.sourceforge.net/meta/";
    
    /**
     * Feature map.
     */
    protected Map features = new TreeMap();
    
    /**
     * Property map.
     */
    protected Map properties = new TreeMap();

    /**
     * Feature writer map.
     */
    protected Map featureWriters = new TreeMap();
    
    /**
     * Property map.
     */
    protected Map propertyWriters = new TreeMap();

    public final Iterator getFeatures() {
    
        return new ImmutableIterator(features.keySet().iterator());
    }

    public final Iterator getProperties() {
    
        return new ImmutableIterator(properties.keySet().iterator());
    }
    
    public final boolean getFeature(String key) {
    
        if ( key.startsWith(metaPrefix) ) {
        
            return getFeature(key.substring(metaPrefix.length()));
        }
        
        if ( !features.containsKey(key) ) {
        
            throw new UnsupportedOperationException("No feature '" + key + "'");
        }
        
        Object value = features.get(key);
        
        if ( value instanceof Boolean ) {
        
            return ((Boolean)value).booleanValue();

        } else {
        
            // This is bad, it shouldn't have happened
            
            throw new IllegalStateException("Bad data for key '" + key + "', class is " + value.getClass().getName());
        }
    }

    public final Object getProperty(String key) {
    
        if ( key.startsWith(metaPrefix) ) {
        
            return getProperty(key.substring(metaPrefix.length()));
        }
        
        if ( !properties.containsKey(key) ) {
        
            throw new UnsupportedOperationException("No property '" + key + "'");
        }
        
        return properties.get(key);
    }
    
    public final synchronized void setFeature(String id, boolean value) {
    
        FeatureWriter w = (FeatureWriter)featureWriters.get(id);
        
        if ( w == null ) {
        
            throw new UnsupportedOperationException("Can't set feature '" + id + "' - don't have a writer");
        }
        
        w.set(id, value);
        
        // Now that we've succeeded, we can store the value into the map
        
        features.put(id, new Boolean(value));
    }
    
    public final synchronized void setProperty(String id, Object value) {
    
        PropertyWriter w = (PropertyWriter)propertyWriters.get(id);
        
        if ( w == null ) {
        
            throw new UnsupportedOperationException("Can't set property '" + id + "' - don't have a writer");
        }
        
        w.set(id, value);
        
        // Now that we've succeeded, we can store the value into the map
        
        properties.put(id, value);
    }
    
    protected abstract class FeatureWriter {
    
        abstract public void set(String key, boolean value);
    }

    protected abstract class PropertyWriter {
    
        abstract public void set(String key, Object value);
    }
}
