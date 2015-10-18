package org.dodgybits.shuffle.android.list.event;

import com.google.common.collect.Sets;
import org.dodgybits.shuffle.android.core.model.Id;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractUpdateEntitiesDeletedEvent {
    private Set<Id> mIds;
    private boolean mmMarkAsDeleted;

    public AbstractUpdateEntitiesDeletedEvent(Collection<Id> ids, boolean markAsDeleted) {
        mIds = new HashSet<>(ids);
        mmMarkAsDeleted = markAsDeleted;
    }

    public AbstractUpdateEntitiesDeletedEvent(Id id, boolean markAsDeleted) {
        mIds = Sets.newHashSet(id);
        mmMarkAsDeleted = markAsDeleted;
    }

    public Set<Id> getIds() {
        return mIds;
    }

    public boolean isMarkedAsDeleted() {
        return mmMarkAsDeleted;
    }

}
