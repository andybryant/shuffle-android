package org.dodgybits.shuffle.android.core.model.persistence;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Project.Builder;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;

import roboguice.inject.ContentResolverProvider;
import roboguice.inject.ContextSingleton;

import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.ACTIVE;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.DELETED;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.MODIFIED_DATE;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.*;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.CHANGE_SET;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.GAE_ID;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects._ID;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.Tasks.DISPLAY_ORDER;

@ContextSingleton
public class ProjectPersister extends AbstractEntityPersister<Project> {
    private static final String TAG = "ProjectPersister";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int DEFAULT_CONTEXT_INDEX = 2;
    private static final int MODIFIED_INDEX = 3;
    private static final int PARALLEL_INDEX = 4;
    private static final int ARCHIVED_INDEX = 5;
    private static final int DELETED_INDEX = 6;
    private static final int ACTIVE_INDEX = 7;
    private static final int GAE_ID_INDEX = 8;
    private static final int CHANGE_SET_INDEX = 9;
    private static final int DISPLAY_ORDER_INDEX = 10;

    @Inject
    public ProjectPersister(ContentResolverProvider provider) {
        super(provider.get());
    }
    
    @Override
    public Project read(Cursor cursor) {
        Builder builder = Project.newBuilder();
        builder
            .setLocalId(readId(cursor, ID_INDEX))
            .setModifiedDate(cursor.getLong(MODIFIED_INDEX))
            .setName(readString(cursor, NAME_INDEX))
            .setDefaultContextId(readId(cursor, DEFAULT_CONTEXT_INDEX))
            .setParallel(readBoolean(cursor, PARALLEL_INDEX))
            .setArchived(readBoolean(cursor, ARCHIVED_INDEX))
            .setDeleted(readBoolean(cursor, DELETED_INDEX))
            .setActive(readBoolean(cursor, ACTIVE_INDEX))
            .setGaeId(readId(cursor, GAE_ID_INDEX))
            .setChangeSet(ProjectChangeSet.fromChangeSet(cursor.getLong(CHANGE_SET_INDEX)))
            .setOrder(cursor.getInt(DISPLAY_ORDER_INDEX));

        return builder.build();
    }
    
    @Override
    protected void writeContentValues(ContentValues values, Project project) {
        // never write id since it's auto generated
        values.put(MODIFIED_DATE, project.getModifiedDate());
        writeString(values, NAME, project.getName());
        writeId(values, DEFAULT_CONTEXT_ID, project.getDefaultContextId());
        writeBoolean(values, PARALLEL, project.isParallel());
        writeBoolean(values, ARCHIVED, project.isArchived());
        writeBoolean(values, DELETED, project.isDeleted());
        writeBoolean(values, ACTIVE, project.isActive());
        writeId(values, GAE_ID, project.getGaeId());
        values.put(CHANGE_SET, project.getChangeSet().getChangeSet());
        values.put(DISPLAY_ORDER, project.getOrder());
    }

    @Override
    Project[] createArray(int size) {
        return new Project[size];
    }

    @Override
    protected String getEntityName() {
        return "project";
    }
    
    @Override
    public Uri getContentUri() {
        return ProjectProvider.Projects.CONTENT_URI;
    }
    
    @Override
    public String[] getFullProjection() {
        return ProjectProvider.Projects.FULL_PROJECTION;
    }

    public void reorderProjects() {
        Cursor c = mResolver.query(
                getContentUri(),
                new String[] {_ID, DISPLAY_ORDER, CHANGE_SET},
                null, null,
                DISPLAY_ORDER + " ASC, " + NAME + " ASC");
        int newOrder = 0;
        ContentValues values = new ContentValues();
        while (c.moveToNext()) {
            long id = c.getLong(0);
            int displayOrder = c.getInt(1);
            ProjectChangeSet changeSet = ProjectChangeSet.fromChangeSet(c.getLong(2));

            newOrder++;
            if (newOrder != displayOrder) {
                changeSet.orderChanged();
                values.put(CHANGE_SET, changeSet.getChangeSet());
                values.put(DISPLAY_ORDER, newOrder);
                mResolver.update(getUri(Id.create(id)), values, null, null);
                values.clear();
            }

        }
        c.close();
    }

    public void moveProject(int fromPosition, int toPosition, Cursor cursor) {
        if (fromPosition == toPosition) return;

        if (fromPosition < toPosition) {
            moveProjects(cursor, fromPosition + 1, toPosition, true);
        } else {
            moveProjects(cursor, toPosition, fromPosition - 1, false);
        }
    }

    /**
     * Moves a range of projects up or down
     *
     * @param cursor
     * @param pos1   start of range
     * @param pos2   end of range
     * @param moveUp are the projects moving up the list
     */
    private void moveProjects(Cursor cursor, int pos1, int pos2, boolean moveUp) {
        ContentValues values = new ContentValues();
        cursor.moveToPosition(moveUp ? pos1 - 1 : pos2 + 1);
        Id initialId = readId(cursor, ID_INDEX);
        int newOrder = cursor.getInt(DISPLAY_ORDER_INDEX);
        ProjectChangeSet originalChangeSet = readChangeSet(cursor);

        if (moveUp) {
            for (int position = pos1; position <= pos2; position++) {
                cursor.moveToPosition(position);
                Id id = readId(cursor, ID_INDEX);
                int order = cursor.getInt(DISPLAY_ORDER_INDEX);
                ProjectChangeSet changeSet = readChangeSet(cursor);
                updateOrder(id.getId(), newOrder, changeSet, values);
                newOrder = order;
            }
        } else {
            for (int position = pos2; position >= pos1; position--) {
                cursor.moveToPosition(position);
                Id id = readId(cursor, ID_INDEX);
                int order = cursor.getInt(DISPLAY_ORDER_INDEX);
                ProjectChangeSet changeSet = readChangeSet(cursor);
                updateOrder(id.getId(), newOrder, changeSet, values);
                newOrder = order;
            }

        }
        updateOrder(initialId.getId(), newOrder, originalChangeSet, values);
    }

    private void updateOrder(long id, int order, ProjectChangeSet changeSet, ContentValues values) {
        Log.d(TAG, "Updating project " + id + " order to " + order);
        Uri uri = ContentUris.withAppendedId(getContentUri(), id);
        values.clear();
        values.put(DISPLAY_ORDER, order);
        values.put(MODIFIED_DATE, System.currentTimeMillis());
        changeSet.orderChanged();
        values.put(CHANGE_SET, changeSet.getChangeSet());
        mResolver.update(uri, values, null, null);
    }

    public ProjectChangeSet readChangeSet(Cursor cursor) {
        return readChangeSet(cursor, CHANGE_SET_INDEX);
    }

    public ProjectChangeSet readChangeSet(Cursor cursor, int index) {
        return ProjectChangeSet.fromChangeSet(readLong(cursor, index));
    }

}
