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
package org.dodgybits.shuffle.android.core.listener;

import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import roboguice.activity.event.OnCreateEvent;
import roboguice.event.Observes;

public class NavigationDrawerLoader {

    @Inject
    private FragmentActivity mActivity;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    public void onCreate(@Observes OnCreateEvent event) {
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                mActivity.getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        DrawerLayout drawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                drawerLayout);

    }

}