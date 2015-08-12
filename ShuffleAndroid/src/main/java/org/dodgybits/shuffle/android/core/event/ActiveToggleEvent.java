package org.dodgybits.shuffle.android.core.event;

import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.model.ListQuery;

public class ActiveToggleEvent {

    private final boolean mIsChecked;
    private final ListQuery mListQuery;
    private final ViewMode mViewMode;

    public ActiveToggleEvent(boolean isChecked, ListQuery listQuery, ViewMode viewMode) {
        mIsChecked = isChecked;
        mListQuery = listQuery;
        mViewMode = viewMode;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public ListQuery getListQuery() {
        return mListQuery;
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    @Override
    public String toString() {
        return "ActiveToggleEvent{" +
                "mIsChecked=" + mIsChecked +
                ", mListQuery=" + mListQuery +
                ", mViewMode=" + mViewMode +
                '}';
    }
}
