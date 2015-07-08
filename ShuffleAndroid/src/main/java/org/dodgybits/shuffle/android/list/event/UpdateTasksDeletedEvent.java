package org.dodgybits.shuffle.android.list.event;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class UpdateTasksDeletedEvent {
    private Set<Long> mTaskIds;
    private boolean mDeleted;

    public UpdateTasksDeletedEvent(Set<Long> taskIds, boolean deleted) {
        mTaskIds = new HashSet<>(taskIds);
        mDeleted = deleted;
    }

    public UpdateTasksDeletedEvent(Long taskId, boolean deleted) {
        mTaskIds = Sets.newHashSet(taskId);
        mDeleted = deleted;
    }

    public Set<Long> getTaskIds() {
        return mTaskIds;
    }

    public boolean isDeleted() {
        return mDeleted;
    }
}
