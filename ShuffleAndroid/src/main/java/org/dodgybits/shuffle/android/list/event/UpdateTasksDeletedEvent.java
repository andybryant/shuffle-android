package org.dodgybits.shuffle.android.list.event;

import org.dodgybits.shuffle.android.core.model.Id;

import java.util.Set;

public class UpdateTasksDeletedEvent extends AbstractUpdateEntitiesDeletedEvent {

    public UpdateTasksDeletedEvent(Set<Id> taskIds, boolean markAsDeleted) {
        super(taskIds, markAsDeleted);
    }

    public UpdateTasksDeletedEvent(Id taskId, boolean markAsDeleted) {
        super(taskId, markAsDeleted);
    }

}
