/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.dodgybits.shuffle.android.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.inject.ContextScopedProvider;
import roboguice.inject.ContextSingleton;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that maintains references to all widgets.
 */
@ContextSingleton
public class WidgetManager {
    private static final String TAG = "WidgetManager";

    // Widget ID -> Widget
    private final static Map<Integer, TaskWidget> mWidgets =
            new ConcurrentHashMap<Integer, TaskWidget>();

    @Inject
    private ContextScopedProvider<TaskWidget> mTaskWidgetProvider;

    public synchronized void createWidgets(Context context, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            getOrCreateWidget(context, widgetId);
        }
    }

    public synchronized void deleteWidgets(Context context, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            // Find the widget in the map
            final TaskWidget widget = get(widgetId);
            if (widget != null) {
                // Stop loading and remove the widget from the map
                widget.onDeleted();
            }
            remove(context, widgetId);
        }
    }

    public synchronized void updateWidgets(Context context, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            // Find the widget in the map
            final TaskWidget widget = get(widgetId);
            if (widget != null) {
                widget.reset();
            } else {
                getOrCreateWidget(context, widgetId);
            }
        }
    }

    public synchronized TaskWidget getOrCreateWidget(Context context, int widgetId) {
        TaskWidget widget = get(widgetId);
        if (widget == null) {
            Log.d(TAG, "Create email widget; ID: " + widgetId);
            widget = mTaskWidgetProvider.get(context);
            widget.setWidgetId(widgetId);
            put(widgetId, widget);
            widget.start();
        }
        return widget;
    }

    private TaskWidget get(int widgetId) {
        return mWidgets.get(widgetId);
    }

    private void put(int widgetId, TaskWidget widget) {
        mWidgets.put(widgetId, widget);
    }

    private void remove(Context context, int widgetId) {
        mWidgets.remove(widgetId);
        WidgetManager.removeWidgetPrefs(context, widgetId);
    }

    public static void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        int n = 0;
        for (TaskWidget widget : mWidgets.values()) {
            writer.println("Widget #" + (++n));
            writer.println("    " + widget.toString());
        }
    }

    /** Saves shared preferences for the given widget */
    static void saveWidgetPrefs(Context context, int appWidgetId, Location location) {
        String queryKey = Preferences.getWidgetQueryKey(appWidgetId);
        String contextIdKey = Preferences.getWidgetContextIdKey(appWidgetId);
        String projectIdKey = Preferences.getWidgetProjectIdKey(appWidgetId);
        TaskSelector selector = TaskSelector.fromLocation(context, location);
        Preferences.getEditor(context).
                putString(queryKey, location.getListQuery().name()).
                putLong(contextIdKey, selector.getContextId().getId()).
                putLong(projectIdKey, selector.getProjectId().getId()).
                commit();
    }

    /** Removes shared preferences for the given widget */
    static void removeWidgetPrefs(Context context, int appWidgetId) {
        String queryKey = Preferences.getWidgetQueryKey(appWidgetId);
        String contextIdKey = Preferences.getWidgetContextIdKey(appWidgetId);
        String projectIdKey = Preferences.getWidgetProjectIdKey(appWidgetId);
        SharedPreferences.Editor editor = Preferences.getEditor(context);
        editor.remove(queryKey).
                remove(contextIdKey).
                remove(projectIdKey).
                apply(); // just want to clean up; don't care when preferences are actually removed
    }

    /**
     * Returns the saved list context for the given widget.
     */
    static Location loadListContextPref(Context context, int appWidgetId) {
        Location location = null;
        String contextIdKey = Preferences.getWidgetContextIdKey(appWidgetId);
        Id contextId = Preferences.getWidgetId(context, contextIdKey);
        String projectIdKey = Preferences.getWidgetProjectIdKey(appWidgetId);
        Id projectId = Preferences.getWidgetId(context, projectIdKey);
        String queryKey = Preferences.getWidgetQueryKey(appWidgetId);
        String queryName = Preferences.getWidgetQuery(context, queryKey);
        if (queryName != null) {
            ListQuery query;
            try {
                query = ListQuery.valueOf(queryName);
                location = Location.viewTaskList(query, projectId, contextId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse key " + queryName);
                // default to next tasks when can't parse key
                query = ListQuery.nextTasks;
                contextId = projectId = Id.NONE;
                location = Location.viewTaskList(query, projectId, contextId);
                saveWidgetPrefs(context, appWidgetId, location);
            }
        }
        return location;
    }

}
