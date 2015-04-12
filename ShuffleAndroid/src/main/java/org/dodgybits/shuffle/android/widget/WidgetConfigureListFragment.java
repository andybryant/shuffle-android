package org.dodgybits.shuffle.android.widget;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor;
import org.dodgybits.shuffle.android.core.view.ListIcons;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.fragment.RoboListFragment;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor.ListItem;

public class WidgetConfigureListFragment extends RoboListFragment {
    private static final String TAG = "WidgetConfigListFrag";
    
    private static ListItem<WidgetEntry>[] sListItems = null;

    private void createListItems() {
        if (sListItems == null) {
            String[] perspectives = getResources().getStringArray(R.array.perspectives).clone();
            sListItems = new ListItem[] {
                    createTaskListItem(ListIcons.INBOX, ListQuery.inbox, perspectives[0]),
                    createTaskListItem(ListIcons.DUE_TASKS, ListQuery.dueTasks, getString(R.string.title_due_tasks)),
                    createTaskListItem(ListIcons.NEXT_TASKS, ListQuery.nextTasks, perspectives[2]),
                    createDialogListItem(ListIcons.PROJECTS, perspectives[3], WidgetConfigure.PROJECT_PICKER_DIALOG),
                    createDialogListItem(ListIcons.CONTEXTS, perspectives[4], WidgetConfigure.CONTEXT_PICKER_DIALOG),
                    createTaskListItem(ListIcons.CUSTOM, ListQuery.custom, perspectives[5]),
                    createTaskListItem(ListIcons.TICKLER, ListQuery.tickler, perspectives[6])
            };
        }
    }

    private static ListItem createTaskListItem(int iconResId, ListQuery query, String name) {
        WidgetEntry entry = new TaskListWidgetEntry(query);
        return new ListItem<WidgetEntry>(iconResId, name, entry);
    }

    private static ListItem createDialogListItem(int iconResId, String name, int dialogId) {
        WidgetEntry entry = new DialogWidgetEntry(dialogId);
        return new ListItem<WidgetEntry>(iconResId, name, entry);
    }

    private IconNameCountListAdaptor mAdaptor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        createListItems();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Log.d(TAG, "-onActivityCreated");
    }

    @Override
    public void onResume() {
        super.onResume();

        setupAdaptor();
    }

    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        sListItems[position].getPayload().onClick(getWidgetConfigure());
    }

    private WidgetConfigure getWidgetConfigure() {
        return (WidgetConfigure)getActivity();
    }

    private void setupAdaptor() {
        mAdaptor = new IconNameCountListAdaptor(
                getActivity(), R.layout.list_item_view, sListItems);
        setListAdapter(mAdaptor);
    }

    private static class TaskListWidgetEntry implements WidgetEntry {
        private ListQuery mListQuery;

        private TaskListWidgetEntry(ListQuery listQuery) {
            mListQuery = listQuery;
        }

        @Override
        public void onClick(WidgetConfigure activity) {
            String key = Preferences.getWidgetQueryKey(activity.getAppWidgetId());
            Preferences.getEditor(activity).putString(key, mListQuery.name()).commit();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                String message = String.format("Saving query %s under key %s", mListQuery, key);
                Log.d(TAG, message);
            }
            activity.confirmSelection();
        }
    }

    private static class DialogWidgetEntry implements WidgetEntry {
        private int mDialogId;

        private DialogWidgetEntry(int dialogId) {
            mDialogId = dialogId;
        }

        @Override
        public void onClick(WidgetConfigure activity) {
            activity.showDialog(mDialogId);
        }
    }


    private static interface WidgetEntry {
        void onClick(WidgetConfigure activity);
    }

}
