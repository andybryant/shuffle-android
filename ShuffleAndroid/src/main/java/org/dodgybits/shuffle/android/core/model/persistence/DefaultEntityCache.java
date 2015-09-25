package org.dodgybits.shuffle.android.core.model.persistence;

import android.util.Log;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.CacheUpdatedEvent;
import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.ItemCache;
import org.dodgybits.shuffle.android.core.util.ItemCache.ValueBuilder;

import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

import java.util.List;

@ContextSingleton
public class DefaultEntityCache<E extends Entity> implements EntityCache<E> {
    private static final String cTag = "DefaultEntityCache";

    private EntityPersister<E> mPersister;
    private Builder mBuilder;
    private ItemCache<Id, E> mCache;
    
    @Inject
    public DefaultEntityCache(EntityPersister<E> persister) {
        Log.d(cTag, "Created entity cache");
        
        mPersister = persister;
        mBuilder = new Builder();
        mCache = new ItemCache<Id, E>(mBuilder);
    }

    @Override
    public E findById(Id localId) {
        E entity = null;
        if (localId.isInitialised()) {
            entity = mCache.get(localId); 
        }
        return entity;
    }

    @Override
    public List<E> findById(List<Id> localIds) {
        List<E> entities = Lists.newArrayList();
        for (Id localId : localIds) {
            E entity = findById(localId);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    @Override
    public void flush() {
        Log.d(cTag, "Flushing cache");
        mCache.clear();
    }

    private void onCacheUpdated(@Observes CacheUpdatedEvent event) {
        flush();
    }

    private class Builder implements ValueBuilder<Id, E> {
    
        @Override
        public E build(Id key) {
            return mPersister.findById(key);
        }
    }
    
}
