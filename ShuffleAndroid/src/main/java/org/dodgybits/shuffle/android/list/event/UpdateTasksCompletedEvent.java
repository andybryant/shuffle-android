package org.dodgybits.shuffle.android.list.event;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class UpdateTasksCompletedEvent {
    private Set<Long> mTaskIds;
    private boolean mCompleted;

    public UpdateTasksCompletedEvent(Set<Long> taskIds, boolean completed) {
        mTaskIds = new HashSet<>(taskIds);
        mCompleted = completed;
    }

    public UpdateTasksCompletedEvent(Long taskId, boolean completed) {
        mTaskIds = Sets.newHashSet(taskId);
        mCompleted = completed;
    }

    public Set<Long> getTaskIds() {
        return mTaskIds;
    }

    public boolean isCompleted() {
        return mCompleted;
    }
}
