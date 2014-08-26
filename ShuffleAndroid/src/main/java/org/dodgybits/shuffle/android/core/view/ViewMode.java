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
package org.dodgybits.shuffle.android.core.view;

import android.os.Bundle;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.event.ModeChangeEvent;
import roboguice.event.EventManager;
import roboguice.inject.ContextSingleton;

/**
 * Represents the view mode for the tablet enabled MainActivity.
 * Transitions between modes should be done through this central object, and UI components that are
 * dependent on the mode should listen to changes on this object.
 */
@ContextSingleton
public class ViewMode {
    public static final String LOG_TAG = "ViewMode";

    /**
     * Mode when showing a single task.
     */
    public static final int TASK = 1;
    /**
     * Mode when showing a list of tasks
     */
    public static final int TASK_LIST = 2;
    /**
     * Mode when showing results from user search.
     */
    public static final int SEARCH_RESULTS_LIST = 3;
    /**
     * Mode when single result from user search.
     */
    public static final int SEARCH_RESULTS_TASK = 4;
    /**
     * Mode when showing a list of contexts.
     */
    public static final int CONTEXT_LIST = 5;
    /**
     * Mode when showing a list of projects.
     */
    public static final int PROJECT_LIST = 6;
    /**
     * Uncertain mode. The mode has not been initialized.
     */
    public static final int UNKNOWN = 0;

    // Key used to save this {@link ViewMode}.
    private static final String VIEW_MODE_KEY = "view-mode";


    // friendly names (not user-facing) for each view mode, indexed by ordinal value.
    private static final String[] MODE_NAMES = {
            "Unknown",
            "Task",
            "Task list",
            "Search results list",
            "Search results task",
            "Context list",
            "Project list"
    };

    /**
     * The actual mode the activity is in. We start out with an UNKNOWN mode, and require entering
     * a valid mode after the object has been created.
     */
    private int mMode = UNKNOWN;

    @Inject
    private EventManager mEventManager;

    @Override
    public String toString() {
        return "[mode=" + MODE_NAMES[mMode] + "]";
    }

    public String getModeString() {
        return MODE_NAMES[mMode];
    }

    /**
     * Dispatches a change event for the mode.
     * Always happens in the UI thread.
     */
    private void dispatchModeChange() {
        mEventManager.fire(new ModeChangeEvent(mMode));
    }

    /**
     * Requests a transition of the mode to show the task list as the prominent view.
     *
     */
    public void enterTaskListMode() {
        setModeInternal(TASK_LIST);
    }

    /**
     * Requests a transition of the mode to show a task as the prominent view.
     *
     */
    public void enterTaskMode() {
        setModeInternal(TASK);
    }

    /**
     * Requests a transition of the mode to show a list of search results as the
     * prominent view.
     *
     */
    public void enterSearchResultsListMode() {
        setModeInternal(SEARCH_RESULTS_LIST);
    }

    /**
     * Requests a transition of the mode to show a task that was part of
     * search results.
     *
     */
    public void enterSearchResultsTaskMode() {
        setModeInternal(SEARCH_RESULTS_TASK);
    }

    /**
     * Requests a transition of the mode to show the "waiting for sync" messages
     *
     */
    public void enterContextListMode() {
        setModeInternal(CONTEXT_LIST);
    }

    /**
     * Requests a transition of the mode to show an ad.
     */
    public void enterProjectListMode() {
        setModeInternal(PROJECT_LIST);
    }

    /**
     * @return The current mode.
     */
    public int getMode() {
        return mMode;
    }

    /**
     * Return whether the current mode is considered a list mode.
     */
    public boolean isListMode() {
        return isListMode(mMode);
    }

    public static boolean isListMode(final int mode) {
        return mode == TASK_LIST || mode == SEARCH_RESULTS_LIST ||
                mode == CONTEXT_LIST || mode == PROJECT_LIST;
    }

    public boolean isTaskMode() {
        return isTaskMode(mMode);
    }

    public static boolean isTaskMode(final int mode) {
        return mode == TASK || mode == SEARCH_RESULTS_TASK;
    }

    public static boolean isSearchMode(final int mode) {
        return mode == SEARCH_RESULTS_LIST || mode == SEARCH_RESULTS_TASK;
    }

    /**
     * Restoring from a saved state restores only the mode.
     *
     * @param inState
     */
    public void handleRestore(Bundle inState) {
        if (inState == null) {
            return;
        }
        // Restore the previous mode, and UNKNOWN if nothing exists.
        final int newMode = inState.getInt(VIEW_MODE_KEY, UNKNOWN);
        if (newMode != UNKNOWN) {
            setModeInternal(newMode);
        }
    }

    public void handleRestore()

    /**
     * Save the existing mode only. Does not save the existing listeners.
     * @param outState
     */
    public void handleSaveInstanceState(Bundle outState) {
        if (outState == null) {
            return;
        }
        outState.putInt(VIEW_MODE_KEY, mMode);
    }

    /**
     * Sets the internal mode.
     * @return Whether or not a change occurred.
     */
    private boolean setModeInternal(int mode) {
        if (mMode == mode) {
            Log.i(LOG_TAG, "ViewMode: debouncing change attempt mode=" + mode);
            return false;
        }
        Log.i(LOG_TAG, "ViewMode: executing change old=" + mMode + " new=" + mode);

        mMode = mode;
        dispatchModeChange();
        return true;
    }
}
