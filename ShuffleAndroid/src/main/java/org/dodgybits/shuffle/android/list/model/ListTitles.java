package org.dodgybits.shuffle.android.list.model;

import org.dodgybits.android.shuffle.R;

import java.util.HashMap;

public class ListTitles {

    private static final HashMap<ListQuery,Integer> TITLE_ID_MAP =
            new HashMap<ListQuery,Integer>();

    static {
        TITLE_ID_MAP.put(ListQuery.inbox, R.string.title_inbox);
        TITLE_ID_MAP.put(ListQuery.nextTasks, R.string.title_next_tasks);
        TITLE_ID_MAP.put(ListQuery.dueTasks, R.string.title_due_tasks);
        TITLE_ID_MAP.put(ListQuery.project, R.string.title_project);
        TITLE_ID_MAP.put(ListQuery.context, R.string.title_context);
        TITLE_ID_MAP.put(ListQuery.deferred, R.string.title_deferred);
        TITLE_ID_MAP.put(ListQuery.deleted, R.string.title_deleted);
        TITLE_ID_MAP.put(ListQuery.search, R.string.title_search);
    }

    public static int getTitleId(ListQuery query) {
        return TITLE_ID_MAP.get(query);
    }
}
