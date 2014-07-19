/**
 * Copyright (C) 2014 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dodgybits.shuffle.android.core.controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.event.ModeChangeEvent;
import org.dodgybits.shuffle.android.core.view.TaskSelectionSet;
import org.dodgybits.shuffle.android.core.view.TwoPaneLayout;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import roboguice.RoboGuice;
import roboguice.event.Observes;

public class TwoPaneController extends AbstractActivityController {
    private static final String TAG = "TwoPaneController";

    private TwoPaneLayout mLayout;


    public TwoPaneController(MainActivity activity, ViewMode viewMode) {
        super(activity, viewMode);
    }

    @Override
    public boolean onCreate(Bundle savedState) {
        mActivity.setContentView(R.layout.two_pane_activity);
        mLayout = (TwoPaneLayout) mActivity.findViewById(R.id.two_pane_activity);
        if (mLayout == null) {
            // We need the layout for everything. Crash/Return early if it is null.
            Log.wtf(TAG, "mLayout is null!");
            return false;
        }
        boolean isSearchList = Intent.ACTION_SEARCH.equals(mActivity.getIntent().getAction());
        mLayout.setController(this, isSearchList);

        return super.onCreate(savedState);
    }


    public void onViewModeChanged(@Observes ModeChangeEvent modeChangeEvent) {
        Log.d(TAG, "Received modeChange" + modeChangeEvent);
    }

    @Override
    protected boolean handleBackPress() {
        return false;
    }

    @Override
    protected boolean handleUpPress() {
        return false;
    }

    @Override
    public boolean isDrawerEnabled() {
        return false;
    }

    @Override
    public void onSetEmpty() {

    }

    @Override
    public void onSetPopulated(TaskSelectionSet set) {

    }

    @Override
    public void onSetChanged(TaskSelectionSet set) {

    }
}
