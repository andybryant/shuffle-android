package org.dodgybits.shuffle.android.list.event;

import android.app.Activity;
import android.support.v4.app.Fragment;
import org.dodgybits.shuffle.android.list.model.ListQuery;

public class EditListSettingsEvent {
    private ListQuery mListQuery;
    private final Activity mActivity;
    private int mRequestCode;

    public EditListSettingsEvent(ListQuery query, Activity activity, int requestCode) {
        mListQuery = query;
        mActivity = activity;
        mRequestCode = requestCode;
    }

    public ListQuery getListQuery() {
        return mListQuery;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    public Activity getActivity() {
        return mActivity;
    }
}
