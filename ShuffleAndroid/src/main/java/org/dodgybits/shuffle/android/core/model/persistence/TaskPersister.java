package org.dodgybits.shuffle.android.core.model.persistence;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseIntArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.Task.Builder;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;
import roboguice.inject.ContentResolverProvider;
import roboguice.inject.ContextSingleton;

import java.util.*;

import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.ACTIVE;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.DELETED;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.MODIFIED_DATE;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.TaskContexts.CONTEXT_ID;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.TaskContexts.TASK_ID;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.Tasks.*;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.Tasks.CHANGE_SET;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.Tasks.GAE_ID;

@ContextSingleton
public class TaskPersister extends AbstractEntityPersister<Task> {
    private static final String TAG = "TaskPersister";

    private static final int ID_INDEX = 0;
    private static final int DESCRIPTION_INDEX = ID_INDEX + 1;
    private static final int DETAILS_INDEX = DESCRIPTION_INDEX + 1;
    private static final int PROJECT_INDEX = DETAILS_INDEX + 1;
    private static final int CREATED_INDEX = PROJECT_INDEX + 1;
    private static final int MODIFIED_INDEX = CREATED_INDEX + 1;
    private static final int START_INDEX = MODIFIED_INDEX + 1;
    private static final int DUE_INDEX = START_INDEX + 1;
    private static final int TIMEZONE_INDEX = DUE_INDEX + 1;
    private static final int CAL_EVENT_INDEX = TIMEZONE_INDEX + 1;
    private static final int DISPLAY_ORDER_INDEX = CAL_EVENT_INDEX + 1;
    private static final int COMPLETE_INDEX = DISPLAY_ORDER_INDEX + 1;
    private static final int ALL_DAY_INDEX = COMPLETE_INDEX + 1;
    private static final int HAS_ALARM_INDEX = ALL_DAY_INDEX + 1;
    private static final int DELETED_INDEX = HAS_ALARM_INDEX + 1;
    private static final int ACTIVE_INDEX = DELETED_INDEX + 1;
    private static final int GAE_ID_INDEX = ACTIVE_INDEX + 1;
    private static final int CHANGE_SET_INDEX = GAE_ID_INDEX + 1;

    private static final int TASK_CONTEXTS_TASK_ID_INDEX = 0;
    private static final int TASK_CONTEXTS_CONTEXT_ID_INDEX = 1;

    @Inject
    public TaskPersister(ContentResolverProvider provider) {
        super(provider.get());
    }

    @Override
    public Task read(Cursor cursor) {
        return read(cursor, true);
    }

    public Task read(Cursor cursor, boolean includeContextIds) {
        Builder builder = Task.newBuilder();
        builder
                .setLocalId(readId(cursor, ID_INDEX))
                .setDescription(readString(cursor, DESCRIPTION_INDEX))
                .setDetails(readString(cursor, DETAILS_INDEX))
                .setProjectId(readId(cursor, PROJECT_INDEX))
                .setCreatedDate(readLong(cursor, CREATED_INDEX))
                .setModifiedDate(readLong(cursor, MODIFIED_INDEX))
                .setStartDate(readLong(cursor, START_INDEX))
                .setDueDate(readLong(cursor, DUE_INDEX))
                .setTimezone(readString(cursor, TIMEZONE_INDEX))
                .setCalendarEventId(readId(cursor, CAL_EVENT_INDEX))
                .setOrder(cursor.getInt(DISPLAY_ORDER_INDEX))
                .setComplete(readBoolean(cursor, COMPLETE_INDEX))
                .setAllDay(readBoolean(cursor, ALL_DAY_INDEX))
                .setHasAlarm(readBoolean(cursor, HAS_ALARM_INDEX))
                .setDeleted(readBoolean(cursor, DELETED_INDEX))
                .setActive(readBoolean(cursor, ACTIVE_INDEX))
                .setGaeId(readId(cursor, GAE_ID_INDEX))
                .setChangeSet(TaskChangeSet.fromChangeSet(cursor.getLong(CHANGE_SET_INDEX)));

        if (includeContextIds) {
            Cursor contextCursor = mResolver.query(TaskProvider.TaskContexts.CONTENT_URI,
                    TaskProvider.TaskContexts.FULL_PROJECTION,
                    TaskProvider.TaskContexts.TASK_ID + "=?",
                    new String[]{String.valueOf(builder.getLocalId().getId())},
                    TaskProvider.TaskContexts.TASK_ID);
            List<Id> contextIds = Lists.newArrayList();
            while (contextCursor.moveToNext()) {
                long id = contextCursor.getLong(TASK_CONTEXTS_CONTEXT_ID_INDEX);
                contextIds.add(Id.create(id));
            }
            contextCursor.close();
            builder.setContextIds(contextIds);
        }

        return builder.build();
    }

    public Id readLocalId(Cursor cursor) {
        return readId(cursor, ID_INDEX);
    }

    public boolean readDeleted(Cursor cursor) {
        return readBoolean(cursor, DELETED_INDEX);
    }

    public boolean readComplete(Cursor cursor) {
        return readBoolean(cursor, COMPLETE_INDEX);
    }

    public TaskChangeSet readChangeSet(Cursor cursor) {
        return readChangeSet(cursor, CHANGE_SET_INDEX);
    }

    public TaskChangeSet readChangeSet(Cursor cursor, int index) {
        return TaskChangeSet.fromChangeSet(readLong(cursor, index));
    }


    @Override
    protected void writeContentValues(ContentValues values, Task task) {
        // never write id since it's auto generated
        writeString(values, DESCRIPTION, task.getDescription());
        writeString(values, DETAILS, task.getDetails());
        writeId(values, PROJECT_ID, task.getProjectId());
        values.put(CREATED_DATE, task.getCreatedDate());
        values.put(MODIFIED_DATE, task.getModifiedDate());
        values.put(START_DATE, task.getStartDate());
        values.put(DUE_DATE, task.getDueDate());
        writeBoolean(values, DELETED, task.isDeleted());
        writeBoolean(values, ACTIVE, task.isActive());

        String timezone = task.getTimezone();
        if (TextUtils.isEmpty(timezone)) {
            if (task.isAllDay()) {
                timezone = Time.TIMEZONE_UTC;
            } else {
                timezone = TimeZone.getDefault().getID();
            }
        }
        values.put(TIMEZONE, timezone);

        writeId(values, CAL_EVENT_ID, task.getCalendarEventId());
        values.put(DISPLAY_ORDER, task.getOrder());
        writeBoolean(values, COMPLETE, task.isComplete());
        writeBoolean(values, ALL_DAY, task.isAllDay());
        writeBoolean(values, HAS_ALARM, task.hasAlarms());
        writeId(values, GAE_ID, task.getGaeId());
        values.put(CHANGE_SET, task.getChangeSet().getChangeSet());
    }

    @Override
    public void bulkInsert(Collection<Task> entities) {
        bulkInsert(entities, true);
    }

    public void bulkInsert(Collection<Task> entities, boolean includeContextIds) {
        if (includeContextIds) {
            // we can't bulk insert if we're adding context ids as we need the newly generated task ids
            List<ContentValues> valuesList = Lists.newArrayList();
            for (Task task : entities) {
                Uri uri = mResolver.insert(getContentUri(), null);
                super.update(uri, task);
                final long taskId = ContentUris.parseId(uri);
                List<Id> contextIds = task.getContextIds();
                for (Id contextId : contextIds) {
                    ContentValues values = new ContentValues();
                    values.put(TASK_ID, taskId);
                    values.put(CONTEXT_ID, contextId.getId());
                    valuesList.add(values);
                }
            }

            if (!valuesList.isEmpty()) {
                ContentValues[] valuesArray = new ContentValues[valuesList.size()];
                valuesList.toArray(valuesArray);
                int created = mResolver.bulkInsert(TaskProvider.TaskContexts.CONTENT_URI, valuesArray);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Created " + created + " rows from " + valuesArray.length + " entries");
                }
            }
        } else {
            super.bulkInsert(entities);
        }
    }

    @Override
    protected void update(Uri uri, Task task) {
        super.update(uri, task);
        saveContextIds(uri, task);
    }

    @Override
    protected String getEntityName() {
        return "task";
    }

    @Override
    public Uri getContentUri() {
        return TaskProvider.Tasks.CONTENT_URI;
    }

    @Override
    public String[] getFullProjection() {
        return TaskProvider.Tasks.FULL_PROJECTION;
    }

    @Override
    public int emptyTrash() {
        // find tasks that are deleted or who's project is deleted
        TaskSelector selector = TaskSelector.newBuilder().setDeleted(Flag.yes).build();

        Cursor cursor = mResolver.query(getContentUri(),
                new String[]{BaseColumns._ID},
                selector.getSelection(null),
                selector.getSelectionArgs(),
                selector.getSortOrder());
        List<String> ids = new ArrayList<String>();
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(ID_INDEX));
        }
        cursor.close();

        int rowsDeleted = 0;
        if (ids.size() > 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "About to delete tasks " + ids);
            }
            String queryString = "_id IN (" + StringUtils.join(ids, ",") + ")";
            rowsDeleted = mResolver.delete(getContentUri(), queryString, null);
        }

        return rowsDeleted;
    }


    public int deleteCompletedTasks() {
        int deletedRows = updateDeletedFlag(TaskProvider.Tasks.COMPLETE + " = 1", null, true);
        Log.d(TAG, "Deleting " + deletedRows + " completed tasks.");

        return deletedRows;
    }


    public void updateCompleteFlag(Id id, boolean isComplete) {
        Long changeSetValue = getChangeSet(id);
        if (changeSetValue != null) {
            TaskChangeSet changeSet = TaskChangeSet.fromChangeSet(changeSetValue);
            changeSet.completeChanged();
            ContentValues values = new ContentValues();
            writeBoolean(values, COMPLETE, isComplete);
            values.put(MODIFIED_DATE, System.currentTimeMillis());
            values.put(CHANGE_SET, changeSet.getChangeSet());
            mResolver.update(getUri(id), values, null, null);
        }
    }

    /**
     * Set deleted flag for entities that match the criteria to isDeleted.
     *
     * @param selection where clause
     * @param selectionArgs parameter values from where clause
     * @param isDeleted flag to set deleted flag to
     * @return number of entities updates
     */
    public int updateDeletedFlag(String selection, String[] selectionArgs, boolean isDeleted) {
        Map<Id, Long> changeSets = getChangeSets(selection, selectionArgs);
        int count = 0;
        if (!changeSets.isEmpty()) {
            long now = System.currentTimeMillis();
            for (Map.Entry<Id, Long> entry : changeSets.entrySet()) {
                ContentValues values = new ContentValues();
                Id id = entry.getKey();
                TaskChangeSet changeSet = TaskChangeSet.fromChangeSet(entry.getValue());
                changeSet.deleteChanged();
                values.put(CHANGE_SET, changeSet.getChangeSet());
                writeBoolean(values, AbstractCollectionProvider.ShuffleTable.DELETED, isDeleted);
                values.put(MODIFIED_DATE, now);
                count += mResolver.update(getUri(id), values, null, null);
            }
        }
        return count;
    }

    /**
     * Calculate where this task should appear on the list for the given project.
     * If no project is defined, order is meaningless, so less as is.
     * <p/>
     * New tasks go to the top of the list.
     * <p/>
     * For existing tasks, check if the project changed, and if so
     * treat like a new task, otherwise leave the order as is.
     *
     * @param originalTask the task before any changes or null if this is a new task
     * @param newProjectId the project selected for this task
     * @return order of task when displayed in the project view
     */
    public int calculateTaskOrder(Task originalTask, Id newProjectId) {
        if (!newProjectId.isInitialised()) return -1;
        int order;
        if (originalTask == null || !originalTask.getProjectId().equals(newProjectId)) {
            // get current highest order value
            Cursor cursor = mResolver.query(
                    TaskProvider.Tasks.CONTENT_URI,
                    new String[]{BaseColumns._ID, TaskProvider.Tasks.DISPLAY_ORDER},
                    TaskProvider.Tasks.PROJECT_ID + " = ?",
                    new String[]{String.valueOf(newProjectId.getId())},
                    TaskProvider.Tasks.DISPLAY_ORDER + " asc");
            if (cursor.moveToFirst()) {
                int lowestOrder = cursor.getInt(1);
                order = lowestOrder - 1;
            } else {
                // no tasks in the project yet.
                order = 0;
            }
            cursor.close();
        } else {
            // leave as is for old tasks where project hasn't changed
            order = originalTask.getOrder();
        }
        return order;
    }

    private void moveFollowingTasks(Map<Long, Integer> updateValues) {
        Set<Long> ids = updateValues.keySet();
        ContentValues values = new ContentValues();

        for (long id : ids) {
            values.clear();
            values.put(DISPLAY_ORDER, updateValues.get(id));
            values.put(MODIFIED_DATE, System.currentTimeMillis());
            Uri uri = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_URI, id);
            mResolver.update(uri, values, null, null);
        }
    }

    public void moveTasksWithinProject(Set<Long> taskIds, Cursor cursor, boolean moveUp) {
        Map<Integer, Integer> positions = Maps.newHashMap();

        cursor.moveToPosition(-1);
        int startPosition = -1;
        while (cursor.moveToNext()) {
            if (startPosition == -1) {
                startPosition = cursor.getPosition();
            }
            Id id = readLocalId(cursor);
            if (taskIds.contains(id.getId())) {
                if (positions.containsKey(startPosition)) {
                    positions.put(startPosition, positions.get(startPosition) + 1);
                } else {
                    positions.put(startPosition, startPosition);
                }
            } else {
                startPosition = -1;
            }
        }

        for (int position : positions.keySet()) {
            moveTask(cursor, position, positions.get(position), moveUp);
        }
    }

    /**
     * There may be clashing orders for the given list of projects. Regenerate order values
     * for all tasks to insure they don't clash.
     *
     * @param projectIds set of projects to update
     */
    public void reorderProjects(Set<Id> projectIds) {
        StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append(TaskProvider.Tasks.PROJECT_ID).
                append(" in (").
                append(StringUtils.repeat(projectIds.size(), "?", ",")).
                append(")");

        String[] whereArgs = new String[projectIds.size()];
        int index = 0;
        for (Id id : projectIds) {
            whereArgs[index] = String.valueOf(id.getId());
            index++;
        }

        Cursor cursor = mResolver.query(
                TaskProvider.Tasks.CONTENT_URI,
                new String[]{
                        BaseColumns._ID,
                        TaskProvider.Tasks.PROJECT_ID,
                        TaskProvider.Tasks.DISPLAY_ORDER,
                        TaskProvider.Tasks.CHANGE_SET
                },
                whereBuilder.toString(),
                whereArgs,
                TaskProvider.Tasks.PROJECT_ID + " ASC, " + TaskProvider.Tasks.DISPLAY_ORDER + " ASC");

        cursor.moveToPosition(-1);
        Id currentProjectId = Id.NONE;
        int previousOrder = Integer.MIN_VALUE;
        ContentValues values = new ContentValues();
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            Id projectId = readId(cursor, 1);
            int order = cursor.getInt(2);

            if (!projectId.equals(currentProjectId)) {
                currentProjectId = projectId;
                previousOrder = Integer.MIN_VALUE;
            }

            if (order == previousOrder) {
                TaskChangeSet changeSet = readChangeSet(cursor, 3);
                order++;
                updateOrder(id, order, changeSet, values);
            }
            previousOrder = order;
        }

    }

    private static final int TASK_COUNT_INDEX = 1;

    public SparseIntArray readCountArray(Cursor cursor) {

        SparseIntArray countMap = new SparseIntArray();
        while (cursor.moveToNext()) {
            Integer id = cursor.getInt(ID_INDEX);
            Integer count = cursor.getInt(TASK_COUNT_INDEX);
            countMap.put(id, count);
        }
        return countMap;
    }

    /**
     * Moves a range of tasks up or down within a project
     *
     * @param cursor
     * @param pos1   start of range
     * @param pos2   end of range
     * @param moveUp are the tasks moving up the list
     */
    private void moveTask(Cursor cursor, int pos1, int pos2, boolean moveUp) {
        ContentValues values = new ContentValues();
        cursor.moveToPosition(moveUp ? pos1 - 1 : pos2 + 1);
        Id initialId = readId(cursor, ID_INDEX);
        int newOrder = cursor.getInt(DISPLAY_ORDER_INDEX);
        TaskChangeSet originalChangeSet = readChangeSet(cursor);

        if (moveUp) {
            for (int position = pos1; position <= pos2; position++) {
                cursor.moveToPosition(position);
                Id id = readId(cursor, ID_INDEX);
                int order = cursor.getInt(DISPLAY_ORDER_INDEX);
                TaskChangeSet changeSet = readChangeSet(cursor);
                updateOrder(id.getId(), newOrder, changeSet, values);
                newOrder = order;
            }
        } else {
            for (int position = pos2; position >= pos1; position--) {
                cursor.moveToPosition(position);
                Id id = readId(cursor, ID_INDEX);
                int order = cursor.getInt(DISPLAY_ORDER_INDEX);
                TaskChangeSet changeSet = readChangeSet(cursor);
                updateOrder(id.getId(), newOrder, changeSet, values);
                newOrder = order;
            }

        }
        updateOrder(initialId.getId(), newOrder, originalChangeSet, values);
    }

    private void updateOrder(long id, int order, TaskChangeSet changeSet, ContentValues values) {
        Uri uri = ContentUris.withAppendedId(getContentUri(), id);
        values.clear();
        values.put(DISPLAY_ORDER, order);
        values.put(MODIFIED_DATE, System.currentTimeMillis());
        changeSet.orderChanged();
        values.put(CHANGE_SET, changeSet.getChangeSet());
        mResolver.update(uri, values, null, null);
    }

    private void saveContextIds(Uri uri, Task task) {
        final long taskId = ContentUris.parseId(uri);
        int deletedRows = mResolver.delete(TaskProvider.TaskContexts.CONTENT_URI,
                TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        Log.d(TAG, "Deleted " + deletedRows + " existing context links for task " + taskId);

        final List<Id> contextIds = task.getContextIds();
        if (!contextIds.isEmpty()) {
            final int count = contextIds.size();
            ContentValues[] valuesArray = new ContentValues[count];
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();
                values.put(TASK_ID, taskId);
                values.put(CONTEXT_ID, contextIds.get(i).getId());
                valuesArray[i] = values;
            }
            mResolver.bulkInsert(TaskProvider.TaskContexts.CONTENT_URI, valuesArray);
        }

    }

}
