package org.dodgybits.shuffle.android.core.model.encoding;

import android.os.Bundle;
import com.google.inject.Singleton;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.Task.Builder;
import org.dodgybits.shuffle.android.core.util.BundleUtils;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;

import static android.provider.BaseColumns._ID;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.ACTIVE;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.DELETED;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.Tasks.*;
import static org.dodgybits.shuffle.android.persistence.provider.TaskProvider.TaskContexts.CONTEXT_ID;

@Singleton
public class TaskEncoder implements EntityEncoder<Task> {
    
    @Override
    public void save(Bundle icicle, Task task) {
        BundleUtils.putId(icicle, _ID, task.getLocalId());
        icicle.putString(DESCRIPTION, task.getDescription());
        icicle.putString(DETAILS, task.getDetails());
        BundleUtils.putId(icicle, PROJECT_ID, task.getProjectId());
        icicle.putLong(CREATED_DATE, task.getCreatedDate());
        icicle.putLong(MODIFIED_DATE, task.getModifiedDate());
        icicle.putLong(START_DATE, task.getStartDate());
        icicle.putLong(DUE_DATE, task.getDueDate());
        icicle.putString(TIMEZONE, task.getTimezone());
        BundleUtils.putId(icicle, CAL_EVENT_ID, task.getCalendarEventId());
        icicle.putInt(DISPLAY_ORDER, task.getOrder());
        icicle.putBoolean(COMPLETE, task.isComplete());
        icicle.putBoolean(ALL_DAY, task.isAllDay());
        icicle.putBoolean(HAS_ALARM, task.hasAlarms());
        icicle.putBoolean(DELETED, task.isDeleted());
        icicle.putBoolean(ACTIVE, task.isActive());
        BundleUtils.putId(icicle, GAE_ID, task.getGaeId());
        icicle.putLong(CHANGE_SET, task.getChangeSet().getChangeSet());

        BundleUtils.putIdList(icicle, CONTEXT_ID, task.getContextIds());
    }

    @Override
    public Task restore(Bundle icicle) {
        if (icicle == null) return null;

        Builder builder = Task.newBuilder();
        builder.setLocalId(BundleUtils.getId(icicle, _ID));
        builder.setDescription(icicle.getString(DESCRIPTION));
        builder.setDetails(icicle.getString(DETAILS));
        builder.setProjectId(BundleUtils.getId(icicle, PROJECT_ID));
        builder.setCreatedDate(icicle.getLong(CREATED_DATE, 0L));
        builder.setModifiedDate(icicle.getLong(MODIFIED_DATE, 0L));
        builder.setStartDate(icicle.getLong(START_DATE, 0L));
        builder.setDueDate(icicle.getLong(DUE_DATE, 0L));
        builder.setTimezone(icicle.getString(TIMEZONE));
        builder.setCalendarEventId(BundleUtils.getId(icicle, CAL_EVENT_ID));
        builder.setOrder(icicle.getInt(DISPLAY_ORDER));
        builder.setComplete(icicle.getBoolean(COMPLETE));
        builder.setAllDay(icicle.getBoolean(ALL_DAY));
        builder.setHasAlarm(icicle.getBoolean(HAS_ALARM));
        builder.setDeleted(icicle.getBoolean(DELETED));
        builder.setActive(icicle.getBoolean(ACTIVE));
        builder.setGaeId(BundleUtils.getId(icicle, GAE_ID));
        builder.setChangeSet(TaskChangeSet.fromChangeSet(icicle.getLong(CHANGE_SET)));

        builder.setContextIds(BundleUtils.getIdList(icicle, CONTEXT_ID));

        return builder.build();
    }

}
