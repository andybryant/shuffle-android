package org.dodgybits.shuffle.android.list.event;

import org.dodgybits.shuffle.android.core.model.Id;

import java.util.Set;

public class UpdateProjectsDeletedEvent extends AbstractUpdateEntitiesDeletedEvent {

    public UpdateProjectsDeletedEvent(Set<Id> projectIds, boolean markAsDeleted) {
        super(projectIds, markAsDeleted);
    }

    public UpdateProjectsDeletedEvent(Id projectId, boolean markAsDeleted) {
        super(projectId, markAsDeleted);
    }

}
