/*
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

package org.dodgybits.shuffle.android.core.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.activity.RoboActivity;

public class BootstrapActivity extends RoboActivity {
	private static final String TAG = "BootstrapActivity";

    @Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

        Class<? extends Activity> activityClass;
		boolean firstTime = Preferences.isFirstTime(this);
		if (firstTime) {
			Log.i(TAG, "First time using Shuffle. Show intro screen");
			activityClass = WelcomeActivity.class;
		} else {
        	activityClass = MainActivity.class;
		}
        
        startActivity(new Intent(this, activityClass));

        finish();
	}

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }
}
