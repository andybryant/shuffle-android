package org.dodgybits.shuffle.android.core.event;

public class CompletedToggleEvent {

    private final boolean isChecked;

    public CompletedToggleEvent(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public boolean isChecked() {
        return isChecked;
    }
}
