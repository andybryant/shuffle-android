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

import android.app.Activity;
import android.content.Intent;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.server.gcm.event.RegisterGcmEvent;
import org.dodgybits.shuffle.android.server.sync.SyncAlarmService;

import roboguice.context.event.OnCreateEvent;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class SyncRegister {


    @Inject
    protected EventManager mEventManager;

    @Inject
    private Activity mActivity;

    private void onCreate(@Observes OnCreateEvent event) {
        mEventManager.fire(new RegisterGcmEvent(mActivity));
        mActivity.startService(new Intent(mActivity, SyncAlarmService.class));
    }

}
