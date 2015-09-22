package org.dodgybits.shuffle.android.core.model.persistence;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.CacheUpdatedEvent;
import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.context.event.OnCreateEvent;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorEntityCache<E extends Entity> implements EntityCache<E>,
                LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CursorEntityCache";

    private EntityPersister<E> mPersister;
    private FragmentActivity mActivity;
    private int loaderId;
    private Map<Id,E> mEntityMap;

    @Inject
    private EventManager mEventManager;

    @Inject
    public CursorEntityCache(Activity activity, EntityPersister<E> persister) {
        this.mActivity = (FragmentActivity) activity;
        this.mPersister = persister;
        mEntityMap = new HashMap<>();
    }

    private void onCreate(@Observes OnCreateEvent<Activity> event) {
        loaderId = mPersister.hashCode();
        mActivity.getSupportLoaderManager().initLoader(loaderId, null, this);
    }


    @Override
    public synchronized E findById(Id localId) {
        return mEntityMap.get(localId);
    }

    @Override
    public synchronized List<E> findById(List<Id> localIds) {
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
    public synchronized void flush() {
        mEntityMap.clear();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        if (loaderId == this.loaderId) {
            return new CursorLoader(
                    mActivity.getApplicationContext(),
                    mPersister.getContentUri(),
                    mPersister.getFullProjection(),
                    null,            // No selection clause
                    null,            // No selection arguments
                    null             // Default sort order
            );

        }
        return null;
    }

    @Override
    public synchronized void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Cursor loaded");
        flush();
        if (cursor.moveToFirst()) {
            do {
                E e = mPersister.read(cursor);
                mEntityMap.put(e.getLocalId(), e);
            } while (cursor.moveToNext());
        }
        mEventManager.fire(new CacheUpdatedEvent());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
