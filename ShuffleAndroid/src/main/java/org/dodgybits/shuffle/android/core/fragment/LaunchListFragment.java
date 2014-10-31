package org.dodgybits.shuffle.android.core.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.LauncherShortcutActivity;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.controller.AbstractActivityController;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.IntentUtils;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor.ListItem;
import org.dodgybits.shuffle.android.core.view.ListIcons;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.model.ListQuery;

import roboguice.fragment.RoboListFragment;

public class LaunchListFragment extends RoboListFragment {
    private static final String TAG = "LaunchListFragment";

    private static ListItem<LaunchEntry>[] sListItems = null;

    private LauncherShortcutActivity getLauncherActivity() {
        return (LauncherShortcutActivity)getActivity();
    }
    
    private void createListItems() {
        if (sListItems == null) {
            String[] perspectives = getResources().getStringArray(R.array.perspectives).clone();
            sListItems = new ListItem[] {
                    createAddTaskListItem(R.drawable.ic_menu_add_field_holo_light, getString(R.string.title_new_task)),
                    createTaskListItem(ListIcons.INBOX, ListQuery.inbox, perspectives[0]),
                    createTaskListItem(ListIcons.DUE_TODAY, ListQuery.dueToday, getString(R.string.title_due_today)),
                    createTaskListItem(ListIcons.DUE_NEXT_WEEK, ListQuery.dueNextWeek, getString(R.string.title_due_next_week)),
                    createTaskListItem(ListIcons.DUE_NEXT_MONTH, ListQuery.dueNextMonth, getString(R.string.title_due_next_month)),
                    createTaskListItem(ListIcons.NEXT_TASKS, ListQuery.nextTasks, perspectives[2]),
                    createDialogListItem(ListIcons.PROJECTS, perspectives[3], LauncherShortcutActivity.PROJECT_PICKER_DIALOG),
                    createDialogListItem(ListIcons.CONTEXTS, perspectives[4], LauncherShortcutActivity.CONTEXT_PICKER_DIALOG),
                    createTaskListItem(ListIcons.CUSTOM, ListQuery.custom, perspectives[5]),
                    createTaskListItem(ListIcons.TICKLER, ListQuery.tickler, perspectives[6])
            };
        }
    }

    private static ListItem createAddTaskListItem(int iconResId, String name) {
        LaunchEntry entry = new NewTaskLaunchEntry(name);
        return new ListItem<LaunchEntry>(iconResId, name, entry);
    }
    
    private static ListItem createTaskListItem(int iconResId, ListQuery query, String name) {
        LaunchEntry entry = new TaskListLaunchEntry(query, name);
        return new ListItem<LaunchEntry>(iconResId, name, entry);
    }

    private static ListItem createDialogListItem(int iconResId, String name, int dialogId) {
        LaunchEntry entry = new DialogLaunchEntry(dialogId);
        return new ListItem<LaunchEntry>(iconResId, name, entry);
    }
    
    
    private IconNameCountListAdaptor mAdaptor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
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

    private void setupAdaptor() {
        mAdaptor = new IconNameCountListAdaptor(
                getActivity(), R.layout.list_item_view, sListItems);
        setListAdapter(mAdaptor);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        sListItems[position].getPayload().onClick(getLauncherActivity());
    }

    private static class NewTaskLaunchEntry implements LaunchEntry {
        private String mName;

        private NewTaskLaunchEntry(String name) {
            mName = name;
        }

        @Override
        public void onClick(LauncherShortcutActivity activity) {
            Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(
                    activity, R.drawable.shuffle_icon_add);
            Intent intent = IntentUtils.createNewTaskIntent(null, Id.NONE, Id.NONE);
            activity.returnShortcut(intent, mName, iconResource);
        }
    }

    
    private static class TaskListLaunchEntry implements LaunchEntry {
        private ListQuery mListQuery;
        private String mName;

        private TaskListLaunchEntry(ListQuery listQuery, String name) {
            mListQuery = listQuery;
            mName = name;
        }

        private Intent getIntent(Activity activity) {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra(MainView.QUERY_NAME, mListQuery.name());
            return intent;
        }

        @Override
        public void onClick(LauncherShortcutActivity activity) {
            Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(
                    activity, R.drawable.shuffle_icon);
            activity.returnShortcut(getIntent(activity), mName, iconResource);
        }
    }
    
    private static class DialogLaunchEntry implements LaunchEntry {
        private int mDialogId;

        private DialogLaunchEntry(int dialogId) {
            mDialogId = dialogId;
        }

        @Override
        public void onClick(LauncherShortcutActivity activity) {
            activity.showDialog(mDialogId);
        }
    }
    
    private interface LaunchEntry {
        void onClick(LauncherShortcutActivity activity);
    }
    
}
