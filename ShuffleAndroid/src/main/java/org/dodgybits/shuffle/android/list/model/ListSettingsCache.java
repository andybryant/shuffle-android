package org.dodgybits.shuffle.android.list.model;

import android.content.Context;
import android.content.Intent;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;
import org.dodgybits.shuffle.android.list.activity.ListSettingsEditorActivity;
import org.dodgybits.shuffle.android.preference.model.ListSettings;

import java.util.HashMap;

public class ListSettingsCache {

    private static final String DUE_TASKS_SETTINGS_KEY = "due_tasks";
    private static final String NEXT_TASKS_SETTINGS_KEY = "next_tasks";

    private static ListSettings projectSettings =
            new ListSettings(ListQuery.project.name()).disableProject();
    private static ListSettings contextSettings =
            new ListSettings(ListQuery.context.name()).disableContext();
    private static ListSettings inboxSettings =
            new ListSettings(ListQuery.inbox.name());
    private static ListSettings dueTaskSettings =
            new ListSettings(DUE_TASKS_SETTINGS_KEY).
                    setDefaultCompleted(Flag.no);
    private static ListSettings ticklerSettings =
            new ListSettings(ListQuery.tickler.name()).
                    setDefaultActive(Flag.no);
    private static ListSettings nextTasksSettings =
            new ListSettings(NEXT_TASKS_SETTINGS_KEY).
                    disableCompleted().
                    disableDeleted().
                    disableActive();
    private static ListSettings searchSettings =
            new ListSettings(ListQuery.search.name()).
                    setDefaultActive(Flag.ignored).
                    setDefaultCompleted(Flag.ignored).
                    setDefaultPending(Flag.ignored);


    private static final HashMap<ListQuery,ListSettings> SPARSE_SETTINGS_MAP =
            new HashMap<ListQuery,ListSettings>();

    static {
        SPARSE_SETTINGS_MAP.put(ListQuery.inbox, inboxSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.nextTasks, nextTasksSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.dueTasks, dueTaskSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.context, contextSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.project, projectSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.tickler, ticklerSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.search, searchSettings);
    }

    public static final ListSettings findSettings(ListQuery query) {
        ListSettings settings = SPARSE_SETTINGS_MAP.get(query);
        if (settings == null) {
            // if setting is not in the map, it means the query has all the standard default settings
            // just create a new one with the right name
            settings = new ListSettings(query.name());
            SPARSE_SETTINGS_MAP.put(query, settings);
        }
        return settings;
    }

    public static Intent createListSettingsEditorIntent(Context context, ListQuery query) {
        Intent intent = new Intent(context, ListSettingsEditorActivity.class);
        intent.putExtra(ListSettingsEditorActivity.LIST_QUERY_EXTRA, query.name());
        ListSettings settings = ListSettingsCache.findSettings(query);
        settings.addToIntent(intent);
        return intent;
    }

}
