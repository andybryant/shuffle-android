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

package org.dodgybits.shuffle.android.view.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.list.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.list.listener.NavigationListener;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;
import org.dodgybits.shuffle.android.view.fragment.TaskViewFragment;

/**
 * A generic activity for viewing a task.
 */
public class TaskViewActivity extends RoboActionBarActivity {
    private static final String TAG = "TaskViewActivity";

    private static final int LOADER_ID_TASK_LOADER = 1;

    private static int TASK_UPDATED = 123;

    @Inject private TaskPersister mPersister;
    @Inject private TaskEncoder mEncoder;

    @Inject
    private NavigationListener mNavigationListener;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    private Uri mUri;
    private Task mTask;
    private Handler mTaskUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TASK_UPDATED) {
                TaskViewFragment viewFragment = TaskViewFragment.newInstance(msg.getData());
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, viewFragment);
                ft.commit();
            }
        }
    };;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(TAG, "onCreate+");
        super.onCreate(icicle);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        setContentView(R.layout.fragment_container);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);

        mUri = getIntent().getData();
        loadCursor();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
        }

        return false;
    }

    private void loadCursor() {
        Log.d(TAG, "Creating list cursor");
        final LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_ID_TASK_LOADER, getIntent().getExtras(), LOADER_CALLBACKS);
    }

    /**
     * Loader callbacks for task list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new TaskCursorLoader(TaskViewActivity.this, mUri);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    c.moveToFirst();
                    mTask = mPersister.read(c);

                    final Bundle args = new Bundle();
                    mEncoder.save(args, mTask);

                    Log.d(TAG, "Trigger loading of task view fragment");
                    Message msg = new Message();
                    msg.what = TASK_UPDATED;
                    msg.setData(args);
                    mTaskUpdateHandler.
                            sendMessage(msg);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private static class TaskCursorLoader extends CursorLoader {
        protected final Context mContext;

        public TaskCursorLoader(Context context, Uri uri) {
            // Initialize with no where clause.  We'll set it later.
            super(context, uri,
                    TaskProvider.Tasks.FULL_PROJECTION, null, null,
                    null);
            mContext = context;
        }

    }

}
