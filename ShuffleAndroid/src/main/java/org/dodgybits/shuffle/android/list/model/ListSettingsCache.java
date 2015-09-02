package org.dodgybits.shuffle.android.list.model;

import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;
import org.dodgybits.shuffle.android.preference.model.ListSettings;

import java.util.HashMap;

public class ListSettingsCache {

    private static final String DUE_TASKS_SETTINGS_KEY = "due_tasks";
    private static final String NEXT_TASKS_SETTINGS_KEY = "next_tasks";

    private static ListSettings inboxSettings =
            new ListSettings(ListQuery.inbox.name())
                    .setDefaultActive(Flag.ignored)
                    .enableCompleted();
    private static ListSettings nextTasksSettings =
            new ListSettings(NEXT_TASKS_SETTINGS_KEY)
                    .setDefaultCompleted(Flag.ignored)
                    .setDefaultActive(Flag.ignored);
    private static ListSettings dueTaskSettings =
            new ListSettings(DUE_TASKS_SETTINGS_KEY)
                    .enableCompleted();
    private static ListSettings projectSettings =
            new ListSettings(ListQuery.project.name())
                    .enableCompleted()
                    .enableActive();
    private static ListSettings contextSettings =
            new ListSettings(ListQuery.context.name())
                    .enableCompleted()
                    .enableActive();
    private static ListSettings deferredSettings =
            new ListSettings(ListQuery.deferred.name())
                    .enableCompleted();
    private static ListSettings deletedSettings =
            new ListSettings(ListQuery.deleted.name())
                    .setDefaultDeleted(Flag.yes)
                    .setDefaultActive(Flag.ignored)
                    .enableCompleted();
    private static ListSettings searchSettings =
            new ListSettings(ListQuery.search.name())
                    .setDefaultCompleted(Flag.ignored)
                    .setDefaultActive(Flag.ignored)
                    .enableCompleted();


    private static final HashMap<ListQuery,ListSettings> SPARSE_SETTINGS_MAP =
            new HashMap<>();

    static {
        SPARSE_SETTINGS_MAP.put(ListQuery.inbox, inboxSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.nextTasks, nextTasksSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.dueTasks, dueTaskSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.project, projectSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.context, contextSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.deferred, deferredSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.deleted, deletedSettings);
        SPARSE_SETTINGS_MAP.put(ListQuery.search, searchSettings);
    }

    public static ListSettings findSettings(ListQuery query) {
        ListSettings settings = SPARSE_SETTINGS_MAP.get(query);
        if (settings == null) {
            // if setting is not in the map, it means the query has all the standard default settings
            // just create a new one with the right name
            settings = new ListSettings(query.name());
            SPARSE_SETTINGS_MAP.put(query, settings);
        }
        return settings;
    }

}
