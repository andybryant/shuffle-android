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
package org.dodgybits.shuffle.android.core;

import android.app.Application;
import android.support.multidex.MultiDexApplication;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import org.dodgybits.android.shuffle.R;

import java.util.HashMap;

public class ShuffleApplication extends MultiDexApplication {

    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-36045118-2";

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    synchronized public Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t;
            if (trackerId == TrackerName.ECOMMERCE_TRACKER) {
                t = analytics.newTracker(PROPERTY_ID);
            } else {
                t = analytics.newTracker(R.xml.analytics);
            }
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
