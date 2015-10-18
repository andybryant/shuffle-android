package org.dodgybits.shuffle.android.list.event;

import org.dodgybits.shuffle.android.core.model.Id;

import java.util.Set;

public class UpdateContextsDeletedEvent extends AbstractUpdateEntitiesDeletedEvent {

    public UpdateContextsDeletedEvent(Set<Id> contextIds, boolean markAsDeleted) {
        super(contextIds, markAsDeleted);
    }

    public UpdateContextsDeletedEvent(Id contextId, boolean markAsDeleted) {
        super(contextId, markAsDeleted);
    }
}
