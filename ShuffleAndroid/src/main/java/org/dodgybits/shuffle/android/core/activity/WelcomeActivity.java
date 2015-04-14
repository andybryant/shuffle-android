/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.persistence.InitialDataGenerator;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;

public class WelcomeActivity extends RoboActionBarActivity {
    private static final String TAG = "WelcomeActivity";
	
    @InjectView(R.id.sample_data_button) Button mSampleDataButton;
    @InjectView(R.id.clean_slate_button) Button mCleanSlateButton;
    @Inject InitialDataGenerator mGenerator;
    
    private Handler mHandler;
    
    @Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "onCreate");
		
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        setContentView(R.layout.welcome);
        
        mSampleDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	disableButtons();
            	performCreateSampleData();
            }
        });
        mCleanSlateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	disableButtons();
            	performCleanSlate();
            }
        });
    	mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            	updateFirstTimePref(false);
                Intent intent = LocationParser.createIntent(WelcomeActivity.this, Location.home());
                startActivity(intent);
            	finish();
            }
        };
	}
    
    private void disableButtons() {
    	mCleanSlateButton.setEnabled(false);
    	mSampleDataButton.setEnabled(false);
    }

    private void performCreateSampleData() {
    	Log.i(TAG, "Adding sample data");
        setProgressBarVisibility(true);
    	new Thread() {
    		public void run() {
    		    mGenerator.createSampleData(mHandler);
    		}
    	}.start();
    }
        
    private void performCleanSlate() {
    	Log.i(TAG, "Cleaning the slate");
        setProgressBarVisibility(true);
    	new Thread() {
    		public void run() {
    		    mGenerator.cleanSlate(mHandler);
    		}
    	}.start();
    }
    
    private void updateFirstTimePref(boolean value) {
		SharedPreferences.Editor editor = Preferences.getEditor(this);
		editor.putBoolean(Preferences.FIRST_TIME, value);
		editor.commit();
    }

}
