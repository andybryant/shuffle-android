package org.dodgybits.shuffle.android.core.event;

public class MoveEnabledChangeEvent {
    private boolean isEnabled;

    public MoveEnabledChangeEvent(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
