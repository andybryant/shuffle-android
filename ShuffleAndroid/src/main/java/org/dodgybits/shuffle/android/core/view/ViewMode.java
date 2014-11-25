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

/**
 * Represents the view mode for the tablet enabled MainActivity.
 * Transitions between modes should be done through this central object, and UI components that are
 * dependent on the mode should listen to changes on this object.
 */
public enum ViewMode {

    /**
     * Mode when showing a single task.
     */
    TASK,
    /**
     * Mode when showing a list of tasks
     */
    TASK_LIST,
    /**
     * Mode when showing results from user search.
     */
    SEARCH_RESULTS_LIST,
    /**
     * Mode when single result from user search.
     */
    SEARCH_RESULTS_TASK,
    /**
     * Mode when showing a list of contexts.
     */
    CONTEXT_LIST,
    /**
     * Mode when showing a list of projects.
     */
    PROJECT_LIST;



    /**
     * Return whether the current mode is considered a list mode.
     */
    public boolean isListMode() {
        return isListMode(this);
    }

    public static boolean isListMode(final ViewMode mode) {
        return mode == TASK_LIST || mode == SEARCH_RESULTS_LIST ||
                mode == CONTEXT_LIST || mode == PROJECT_LIST;
    }

    public boolean isTaskMode() {
        return isTaskMode(this);
    }

    public static boolean isTaskMode(final ViewMode mode) {
        return mode == TASK || mode == SEARCH_RESULTS_TASK;
    }

    public static boolean isSearchMode(final ViewMode mode) {
        return mode == SEARCH_RESULTS_LIST || mode == SEARCH_RESULTS_TASK;
    }


}
