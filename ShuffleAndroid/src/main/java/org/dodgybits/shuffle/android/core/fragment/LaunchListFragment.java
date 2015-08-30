package org.dodgybits.shuffle.android.core.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.activity.LauncherShortcutActivity;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor;
import org.dodgybits.shuffle.android.core.view.IconNameCountListAdaptor.ListItem;
import org.dodgybits.shuffle.android.core.view.ListIcons;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;

import roboguice.fragment.RoboListFragment;

import static org.dodgybits.shuffle.android.list.model.ListQuery.*;

public class LaunchListFragment extends RoboListFragment {
    private static final String TAG = "LaunchListFragment";

    private static ListItem<LaunchEntry>[] sListItems = null;

    private LauncherShortcutActivity getLauncherActivity() {
        return (LauncherShortcutActivity)getActivity();
    }
    
    private void createListItems() {
        if (sListItems == null) {
            sListItems = new ListItem[] {
                    createAddTaskListItem(R.drawable.ic_edit_black_24dp, getString(R.string.title_new_task)),
                    createTaskListItem(ListIcons.INBOX, inbox, getString(R.string.title_inbox)),
                    createTaskListItem(ListIcons.DUE_TASKS, dueTasks, getString(R.string.title_due_tasks)),
                    createTaskListItem(ListIcons.NEXT_TASKS, nextTasks, getString(R.string.title_next_tasks)),
                    createDialogListItem(ListIcons.PROJECTS, getString(R.string.title_project), LauncherShortcutActivity.PROJECT_PICKER_DIALOG),
                    createDialogListItem(ListIcons.CONTEXTS, getString(R.string.title_context), LauncherShortcutActivity.CONTEXT_PICKER_DIALOG),
                    createTaskListItem(ListIcons.DEFFERED, deferred, getString(R.string.title_deferred)),
                    createTaskListItem(ListIcons.DELETED, deleted, getString(R.string.title_deleted))
            };
        }
    }

    private static ListItem<LaunchEntry> createAddTaskListItem(int iconResId, String name) {
        LaunchEntry entry = new NewTaskLaunchEntry(name);
        return new ListItem<>(iconResId, name, entry);
    }
    
    private static ListItem<LaunchEntry> createTaskListItem(int iconResId, ListQuery query, String name) {
        LaunchEntry entry = new TaskListLaunchEntry(query, name);
        return new ListItem<>(iconResId, name, entry);
    }

    private static ListItem<LaunchEntry> createDialogListItem(int iconResId, String name, int dialogId) {
        LaunchEntry entry = new DialogLaunchEntry(dialogId);
        return new ListItem<>(iconResId, name, entry);
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
                    activity, R.mipmap.ic_launcher);
            Location location = Location.newTask();
            Intent intent = LocationParser.createIntent(activity, location);
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
            return LocationParser.createIntent(activity, Location.viewTaskList(mListQuery));
        }

        @Override
        public void onClick(LauncherShortcutActivity activity) {
            Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(
                    activity, R.mipmap.ic_launcher);
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
