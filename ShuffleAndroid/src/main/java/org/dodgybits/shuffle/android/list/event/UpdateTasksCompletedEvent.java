package org.dodgybits.shuffle.android.list.event;

import com.google.common.collect.Sets;

import org.dodgybits.shuffle.android.core.model.Id;

import java.util.HashSet;
import java.util.Set;

public class UpdateTasksCompletedEvent {
    private Set<Id> mTaskIds;
    private boolean mCompleted;

    public UpdateTasksCompletedEvent(Set<Id> taskIds, boolean completed) {
        mTaskIds = new HashSet<>(taskIds);
        mCompleted = completed;
    }

    public UpdateTasksCompletedEvent(Id taskId, boolean completed) {
        mTaskIds = Sets.newHashSet(taskId);
        mCompleted = completed;
    }

    public Set<Id> getTaskIds() {
        return mTaskIds;
    }

    public boolean isCompleted() {
        return mCompleted;
    }
}
