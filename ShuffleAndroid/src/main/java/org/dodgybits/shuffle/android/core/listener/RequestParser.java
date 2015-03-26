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
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import roboguice.context.event.OnCreateEvent;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class RequestParser {
    private static final String TAG = "RequestParser";

    private FragmentActivity mActivity;

    private EventManager mEventManager;

    private static UriMatcher sUriMatcher;

    private static int CONTEXT = 1;
    private static int PROJECT = 2;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ContextProvider.AUTHORITY, "contexts/#", CONTEXT);
        sUriMatcher.addURI(ProjectProvider.AUTHORITY, "projects/#", PROJECT);
    }

    @Inject
    public RequestParser(Activity activity, EventManager eventManager) {
        mActivity = (FragmentActivity) activity;
        mEventManager = eventManager;
    }

    private void onCreate(@Observes OnCreateEvent event) {
        final Intent intent = mActivity.getIntent();
        Bundle savedState = event.getSavedInstanceState();
        if (savedState != null) {
            handleRestore(savedState);
        } else if (intent != null) {
            handleIntent(intent);
        } else {
            MainView mainView = MainView.newBuilder().build();
            Log.d(TAG, "IN onCreate - no saved state or intent. defaulting to=" + mainView);
            mEventManager.fire(new NavigationRequestEvent(mainView));
        }
    }

    /**
     * Restoring from a saved state restores only the main view.
     *
     * @param inState
     */
    public void handleRestore(Bundle inState) {
        if (inState == null) {
            return;
        }

        MainView mainView = inState.getParcelable(AbstractMainActivity.MAIN_VIEW_KEY);
        Log.d(TAG, "IN handleRestore. mainView=" + mainView);
        mEventManager.fire(new NavigationRequestEvent(mainView));
    }


    /**
     * Handle an intent to open the app. This method is called only when there is no saved state,
     * so we need to set state that wasn't set before. It is correct to change the mainView here
     * since it has not been previously set.
     *
     * This method is called when launching the app from
     * notifications, widgets, and shortcuts.
     * @param intent intent passed to the activity.
     */
    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();
        Log.d(TAG, "IN handleIntent. action=" + intent.getAction() + " data=" + uri);

        MainView.Builder builder = MainView.newBuilder();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
//            if (shouldEnterSearchTaskMode()) {
//               mainView = MainView.createSearchListItem(query, taskId);
//            } else {
            builder.setSearchQuery(query)
                    .setListQuery(ListQuery.search);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String queryName = intent.getStringExtra(MainView.QUERY_NAME);
            if (queryName != null) {
                ListQuery query = ListQuery.valueOf(queryName);
                builder.setListQuery(query);
            } else if (uri != null) {
                int match = sUriMatcher.match(uri);
                if (match == CONTEXT) {
                    long contextId = ContentUris.parseId(uri);
                    builder.setListQuery(ListQuery.context)
                            .setEntityId(Id.create(contextId));
                } else if (match == PROJECT) {
                    long projectId = ContentUris.parseId(uri);
                    builder.setListQuery(ListQuery.project)
                            .setEntityId(Id.create(projectId));
                } else {
                    Log.e(TAG, "Unexpected intent uri" + uri);
                }
            }
        } else {
            Log.e(TAG, "Unexpected intent" + intent);
        }

        mEventManager.fire(new NavigationRequestEvent(builder.build()));
    }

}
