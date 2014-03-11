package org.dodgybits.shuffle.android.core.model.persistence;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.google.common.collect.Maps;
import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider;
import org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable;
import org.dodgybits.shuffle.sync.model.EntityChangeSet;

import java.util.Collection;
import java.util.Map;

import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.CHANGE_SET;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.GAE_ID;

public abstract class AbstractEntityPersister<E extends Entity> implements EntityPersister<E> {

    protected ContentResolver mResolver;

    public AbstractEntityPersister(ContentResolver resolver) {
        mResolver = resolver;
    }
    
    @Override
    public E findById(Id localId) {
        E entity = null;
        
        if (localId.isInitialised()) {
            Cursor cursor = mResolver.query(
                    getContentUri(), 
                    getFullProjection(),
                    BaseColumns._ID + " = ?", 
                    new String[] {localId.toString()}, 
                    null);
            
            if (cursor.moveToFirst()) {
                entity = read(cursor);
            }
            cursor.close();
        }
        
        return entity;
    }

    @Override
    public E findByGaeId(Id gaeId) {
        E entity = null;

        if (gaeId.isInitialised()) {
            Cursor cursor = mResolver.query(
                    getContentUri(),
                    getFullProjection(),
                    ShuffleTable.GAE_ID + " = ?",
                    new String[] {gaeId.toString()},
                    null);

            if (cursor.moveToFirst()) {
                entity = read(cursor);
            }
            cursor.close();
        }

        return entity;
    }


    @Override
    public Uri insert(E e) {
        validate(e);
        Uri uri = mResolver.insert(getContentUri(), null);
        update(uri, e);
        return uri;
    }

    @Override
    public void bulkInsert(Collection<E> entities) {
        int numEntities = entities.size();
        if (numEntities > 0) {
            ContentValues[] valuesArray = new ContentValues[numEntities];
            int i = 0;
            for(E entity : entities) {
                validate(entity);
                ContentValues values = new ContentValues();
                writeContentValues(values, entity);
                valuesArray[i++] = values;
            }
            int rowsCreated = mResolver.bulkInsert(getContentUri(), valuesArray);
        }
    }

    @Override
    public void update(E e) {
        validate(e);
        Uri uri = getUri(e);
        update(uri, e);
    }

    @Override
    public boolean updateDeletedFlag(Id id, boolean isDeleted) {
        boolean success = false;
        Long changeSetValue = getChangeSet(id);
        if (changeSetValue != null) {
            EntityChangeSet changeSet = EntityChangeSet.fromChangeSet(changeSetValue);
            changeSet.deleteChanged();
            ContentValues values = new ContentValues();
            writeBoolean(values, ShuffleTable.DELETED, isDeleted);
            values.put(ShuffleTable.MODIFIED_DATE, System.currentTimeMillis());
            values.put(ShuffleTable.CHANGE_SET, changeSet.getChangeSet());
            success = mResolver.update(getUri(id), values, null, null) == 1;
        }
        return success;
    }

    protected Long getChangeSet(Id id) {
        Long changeSet = null;
        Cursor cursor = mResolver.query(
                getContentUri(),
                new String[] {BaseColumns._ID, ShuffleTable.CHANGE_SET},
                BaseColumns._ID + " = ?",
                new String[] {id.toString()},
                null);
        if (cursor.moveToFirst()) {
            changeSet = cursor.getLong(1);
        }
        cursor.close();
        return changeSet;
    }

    protected Map<Id, Long> getChangeSets(String selection, String[] selectionArgs) {
        Map setMap = Maps.newHashMap();
        Cursor cursor = mResolver.query(
                getContentUri(),
                new String[]{BaseColumns._ID, ShuffleTable.CHANGE_SET},
                selection,
                selectionArgs,
                null);
        if (cursor.moveToFirst()) {
            do {
                Id id = Id.create(cursor.getLong(0));
                Long changeSetValue = cursor.getLong(1);
                setMap.put(id, changeSetValue);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return setMap;
    }

    public boolean updateGaeId(Id localId, Id gaeId) {
        ContentValues values = new ContentValues();
        writeId(values, GAE_ID, gaeId);
        values.put(AbstractCollectionProvider.ShuffleTable.MODIFIED_DATE, System.currentTimeMillis());
        return (mResolver.update(getUri(localId), values, null, null) == 1);
    }

    public void clearAllGaeIds() {
        ContentValues values = new ContentValues();
        writeId(values, GAE_ID, Id.NONE);
        mResolver.update(getContentUri(), values, null, null);
    }

    public void clearAllChangeSets() {
        ContentValues values = new ContentValues();
        values.put(CHANGE_SET, EntityChangeSet.NO_CHANGES);
        mResolver.update(getContentUri(), values, null, null);
    }

    @Override
    public int emptyTrash() {
        int rowsDeleted = mResolver.delete(getContentUri(), "deleted = 1", null);
        return rowsDeleted;
    }
    
    @Override
    public boolean deletePermanently(Id id) {
        Uri uri = getUri(id);
        boolean success = (mResolver.delete(uri, null, null) == 1);
        return success;
    }

    @Override
    public boolean deletePermanentlyByGaeId(Id gaeId) {
        boolean success = false;
        if (gaeId.isInitialised()) {
            success = (mResolver.delete(getContentUri(),
                ShuffleTable.GAE_ID + " = ?",
                    new String[] {String.valueOf(gaeId.getId())}) == 1);
        }
        return success;
    }

    @Override
    public int getPositionOfItemWithId(Cursor cursor, long id) {
        int position = -1;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (id == cursor.getLong(0)) {
                position = cursor.getPosition();
                break;
            }
        }
        return position;
    }

    abstract public Uri getContentUri();
    
    abstract protected void writeContentValues(ContentValues values, E e);
    
    abstract protected String getEntityName();
    
    private void validate(E e) {
        if (e == null || !e.isValid()) {
            throw new IllegalArgumentException("Cannot persist uninitialised entity " + e);
        }
    }
    
    protected Uri getUri(E e) {
        return getUri(e.getLocalId());
    }

    protected Uri getUri(Id localId) {
        return ContentUris.appendId(
                getContentUri().buildUpon(), localId.getId()).build();
    }

    protected void update(Uri uri, E e) {
        ContentValues values = new ContentValues();
        writeContentValues(values, e);
        mResolver.update(uri, values, null, null);
    }
    
    protected static Id readId(Cursor cursor, int index) {
        Id result = Id.NONE;
        if (!cursor.isNull(index)) {
            result = Id.create(cursor.getLong(index));
        }
        return result;
    }
    
    protected static String readString(Cursor cursor, int index) {
        return (cursor.isNull(index) ? null : cursor.getString(index));
    }
    
    protected static long readLong(Cursor cursor, int index) {
        return readLong(cursor, index, 0L);
    }
    
    protected static long readLong(Cursor cursor, int index, long defaultValue) {
        long result = defaultValue;
        if (!cursor.isNull(index)) {
            result = cursor.getLong(index);
        }
        return result;
    }
    
    protected static Boolean readBoolean(Cursor cursor, int index) {
        return (cursor.getInt(index) == 1);
    }
    
    protected static void writeId(ContentValues values, String key, Id id) {
        if (id.isInitialised()) {
            values.put(key, id.getId());
        } else {
            values.putNull(key);
        }
    }
    
    protected static void writeBoolean(ContentValues values, String key, boolean value) {
        values.put(key, value ? 1 : 0);
    }
    
    protected static void writeString(ContentValues values, String key, String value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
        
    }
    
}
