package org.dodgybits.shuffle.android.list.view.context;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.ContextListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.ContextTaskCountCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.LoadCountCursorEvent;
import org.dodgybits.shuffle.android.core.event.LoadListCursorEvent;
import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.EditContextEvent;
import org.dodgybits.shuffle.android.list.event.NewContextEvent;
import org.dodgybits.shuffle.android.list.event.QuickAddEvent;
import org.dodgybits.shuffle.android.list.event.UpdateContextDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.list.view.QuickAddController;

import roboguice.activity.RoboActionBarActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboListFragment;

public class ContextListFragment extends RoboListFragment {
    private static final String TAG = "ContextListFragment";
    
    /** Argument name(s) */
    private static final String BUNDLE_LIST_STATE = "ContextListFragment.state.listState";

    @Inject
    private ContextListAdaptor mListAdapter;

    @Inject
    private TaskPersister mTaskPersister;
    
    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    @Inject
    private QuickAddController mQuickAddController;

    private Parcelable mSavedListState;

    private boolean mResumed = false;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView lv = getListView();
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(lv);

        setEmptyText(getString(R.string.no_contexts));

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
            restoreListState();
        }

        updateCursor(mCursorProvider.getContextListCursor());
        Log.d(TAG, "-onActivityCreated");
    }

    @Override
    public void onPause() {
        mSavedListState = getListView().onSaveInstanceState();
        super.onPause();

        mResumed = false;
        Log.d(TAG, "-onPause");
    }

    @Override
    public void onResume() {
        super.onResume();

        mResumed = true;
        refreshChildCount();
    }

    /**
     * Called when a context is clicked.
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        MainView mainView = MainView.newBuilder()
                .setListQuery(ListQuery.context)
                .setEntityId(Id.create(id))
                .build();
        mEventManager.fire(new MainViewUpdateEvent(mainView));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_list_context_menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        org.dodgybits.shuffle.android.core.model.Context context = mContextPersister.read(cursor);

        String entityName = getString(R.string.context_name);

        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
        deleteMenu.setVisible(!context.isDeleted());
        deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));

        MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
        undeleteMenu.setVisible(context.isDeleted());
        undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) return super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_edit:
                mEventManager.fire(new EditContextEvent(Id.create(info.id)));
                return true;
            case R.id.action_delete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), true));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.CONTEXT_LIST));
                return true;
            case R.id.action_undelete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), false));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.CONTEXT_LIST));
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void onCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        updateCursor(event.getCursor());
    }

    private void updateCursor(Cursor cursor) {
        if (cursor == null) {
            return;
        }

        Log.d(TAG, "Swapping cursor and setting adapter");
        mListAdapter.swapCursor(cursor);
        setListAdapter(mListAdapter);

        restoreListState();
    }

    private void restoreListState() {
        if (getActivity() != null && getListAdapter() != null) {
            // Restore the state -- this step has to be the last, because Some of the
            // "post processing" seems to reset the scroll position.
            if (mSavedListState != null) {
                getListView().onRestoreInstanceState(mSavedListState);
                mSavedListState = null;
            }
        }
    }

    private void onTaskCountCursorLoaded(@Observes ContextTaskCountCursorLoadedEvent event) {
        Cursor cursor = event.getCursor();
        mListAdapter.setTaskCountArray(mTaskPersister.readCountArray(cursor));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getListView().invalidateViews();
                }
            });
        }
        cursor.close();
    }

    protected RoboActionBarActivity getRoboActionBarActivity() {
        return (RoboActionBarActivity) getActivity();
    }

    private void onQuickAddEvent(@Observes QuickAddEvent event) {
        if (getUserVisibleHint() && mResumed) {
            mEventManager.fire(new NewContextEvent(event.getValue()));
        }
    }

    private void updateQuickAdd() {
        mQuickAddController.init(getActivity());
        mQuickAddController.setEnabled(ListSettingsCache.findSettings(ListQuery.context).getQuickAdd(getActivity()));
        mQuickAddController.setEntityName(getString(R.string.context_name));
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
    }


    private void refreshChildCount() {
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.CONTEXT_LIST));
    }

}