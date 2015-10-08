package org.dodgybits.shuffle.android.core.util;

import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.HashMap;

/**
 * Generic in-memory cache based on Romain Guy's suggestion.
 * See http://code.google.com/events/io/2009/sessions/TurboChargeUiAndroidFast.html
 */
public class ItemCache<K,V> {
    private static final String cTag = "ItemCache";
 
    private final HashMap<K, SoftReference<V>> mCache;
    private final ValueBuilder<K,V> mBuilder;
    
    public ItemCache(ValueBuilder<K,V> builder) {
        mCache = new HashMap<>();
        mBuilder = builder;
    }
    
    public void put(K key, V value) {
        mCache.put(key, new SoftReference<V>(value));
    }
    
    public V get(K key) {
        V value = null;
        
        SoftReference<V> reference = mCache.get(key);
        if (reference != null) {
            value = reference.get();
        }
        
        // not in cache or gc'd
        if (value == null) {
            Log.d(cTag, "Cache miss for "  + key);
            value = mBuilder.build(key);
            put(key, value);
        }
        
        return value;
    }
    
    public void remove(K key) {
        mCache.remove(key);
    }
    
    public void clear() {
        mCache.clear();
    }
    
    public interface ValueBuilder<K,V> {
        
        V build(K key);
    }
    
}
