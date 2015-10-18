package org.dodgybits.shuffle.android.core.event;

import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.model.ListQuery;

public class CompletedToggleEvent {

    private final boolean mIsChecked;
    private final Location mLocation;

    public CompletedToggleEvent(boolean isChecked, Location location) {
        mIsChecked = isChecked;
        mLocation = location;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public Location getLocation() {
        return mLocation;
    }

    @Override
    public String toString() {
        return "CompletedToggleEvent{" +
                "mIsChecked=" + mIsChecked +
                ", mLocation=" + mLocation +
                '}';
    }
}
