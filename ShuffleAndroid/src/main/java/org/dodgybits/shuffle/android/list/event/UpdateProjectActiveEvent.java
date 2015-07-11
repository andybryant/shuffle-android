package org.dodgybits.shuffle.android.list.event;

import org.dodgybits.shuffle.android.core.model.Id;

public class UpdateProjectActiveEvent {
    private Id mProjectId;
    private Boolean mActive;

    public UpdateProjectActiveEvent(Id projectId) {
        mProjectId = projectId;
    }

    public UpdateProjectActiveEvent(Id projectId, boolean active) {
        mProjectId = projectId;
        mActive = active;
    }

    public Id getProjectId() {
        return mProjectId;
    }

    public Boolean getActive() {
        return mActive;
    }
}
