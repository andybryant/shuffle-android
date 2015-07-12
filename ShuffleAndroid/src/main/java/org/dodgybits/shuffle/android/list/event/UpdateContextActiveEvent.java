package org.dodgybits.shuffle.android.list.event;

import org.dodgybits.shuffle.android.core.model.Id;

public class UpdateContextActiveEvent {
    private Id mContextId;
    private Boolean mActive;

    public UpdateContextActiveEvent(Id contextId) {
        mContextId = contextId;
    }

    public UpdateContextActiveEvent(Id contextId, boolean active) {
        mContextId = contextId;
        mActive = active;
    }

    public Id getContextId() {
        return mContextId;
    }

    public Boolean getActive() {
        return mActive;
    }
}
