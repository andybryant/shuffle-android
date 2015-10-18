package org.dodgybits.shuffle.android.list.view;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.CacheUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.DefaultEntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.EntityUtils;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.activity.TaskListActivity;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.list.view.task.TaskListItem;
import org.dodgybits.shuffle.android.preference.model.ListFeatures;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

public class DeletedRecyclerFragment extends RoboFragment {
    private static final String TAG = "DeletedRecFrag";

    private static final String SELECTED_ITEMS = "DeletedRecyclerFragment.selected";

    @Inject
    private TaskPersister mTaskPersister;

    @Inject
    private ProjectPersister mProjectPersister;

    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private ContextScopedProvider<EntityListItem> mEntityListItemProvider;

    @Inject
    private ContextScopedProvider<TaskListItem> mTaskListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private DefaultEntityCache<Project> mProjectCache;

    @Inject
    private CursorProvider mCursorProvider;

    @Inject
    private LocationProvider mLocationProvider;

    private Location mLocation;

    private RecyclerView mRecyclerView;
    private CombinedListAdapter mListAdapter;

    private MultiSelector mMultiSelector = new MultiSelector();
    private ActionMode mActionMode = null;
    private Comparator<Task> mComparator;
    private ModalMultiSelectorCallback mEditMode = new CombinedModalMultiSelectorCallback(mMultiSelector);

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        mComparator = EntityUtils.createComparator(mProjectCache);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());

        mRecyclerView.setHasFixedSize(true);
        mListAdapter = new CombinedListAdapter();
        mListAdapter.setHasStableIds(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        checkCursors();

        if (mMultiSelector != null) {
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(SELECTED_ITEMS));
            }

            if (mMultiSelector.isSelectable()) {
                if (mEditMode != null) {
                    mEditMode.setClearOnPrepare(false);
                    mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                    mActionMode.invalidate();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Saving state");
        outState.putBundle(SELECTED_ITEMS, mMultiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        onViewUpdate(mLocationProvider.getLocation());
    }


    private TaskListActivity getTaskListActivity() {
        return (TaskListActivity) getActivity();
    }

    private void onViewUpdate(@Observes LocationUpdatedEvent event) {
        onViewUpdate(event.getLocation());
    }

    private void onCacheUpdated(@Observes CacheUpdatedEvent event) {
        mListAdapter.notifyDataSetChanged();
    }

    private void onViewUpdate(Location location) {
        if (!ObjectUtils.equals(location, mLocation)) {
            mLocation = location;

            checkCursors();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onViewLoaded(@Observes LocationUpdatedEvent event) {
        checkCursors();
    }

    private void onCursorUpdated(@Observes CursorUpdatedEvent event) {
        checkCursors();
    }

    private void checkCursors() {
        Cursor contextListCursor = mCursorProvider.getContextListCursor();
        Cursor projectListCursor = mCursorProvider.getProjectListCursor();
        Cursor taskListCursor = mCursorProvider.getTaskListCursor();
        if (contextListCursor != null && projectListCursor != null &&
                taskListCursor != null) {
            updateCursors(contextListCursor, projectListCursor, taskListCursor);
        }
    }

    private void updateCursors(Cursor contextListCursor,
                               Cursor projectListCursor, Cursor taskListCursor) {
        if (getActivity() == null) {
            Log.w(TAG, "Activity not set on " + this);
            return;
        }

        Log.d(TAG, "Swapping adapter cursor");
        mListAdapter.changeCursors(contextListCursor, projectListCursor, taskListCursor);
        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    protected RoboAppCompatActivity getRoboAppCompatActivity() {
        return (RoboAppCompatActivity) getActivity();
    }

    public class CombinedHolder extends SelectableHolderImpl implements
            View.OnClickListener, View.OnLongClickListener,
            SelectorClickListener {

        View mListItem;
        Object mEntity;

        public CombinedHolder(View listItem) {
            super(listItem, mMultiSelector);

            mListItem = listItem;
            mListItem.setOnClickListener(this);
            mListItem.setLongClickable(true);
            mListItem.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickPanel();
        }

        private void clickPanel() {
            if (mEntity instanceof Entity) {
                Location location = Location.editEntity((Entity)mEntity);
                if (location != null) {
                    mEventManager.fire(new NavigationRequestEvent(location));
                }
            }
        }

        @Override
        public void onClickSelector() {
            if (mActionMode == null) {
                mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                mMultiSelector.setSelected(this, true);
                mActionMode.invalidate();
            } else {
                if (mMultiSelector.tapSelection(this)) {
                    if (mMultiSelector.getSelectedPositions().isEmpty()) {
                        mActionMode.finish();
                    } else {
                        mActionMode.invalidate();
                    }
                } else {
                    clickPanel();
                }
            }
        }

        public void bindEntity(Object entity) {
            mEntity = entity;

            if (mEntity instanceof Task) {
                boolean projectVisible = ListFeatures.isProjectNameVisible(mLocation);
                boolean isSelected = mLocation.getSelectedIndex() == getAdapterPosition();
                ((TaskListItem)mListItem).updateItem((Task)mEntity, projectVisible, false, false, false, isSelected);
            } else if (mEntity instanceof Project) {
                ((EntityListItem)mListItem).updateView((Project)mEntity, null, false, false, false);
            } else if (mEntity instanceof Context) {
                ((EntityListItem)mListItem).updateView((Context)mEntity, null);
            } else if (mEntity instanceof EntityLabel){
                EntityLabel entityLabel = (EntityLabel) mEntity;
                ((EntityListItem) mListItem).updateView(entityLabel.name, entityLabel.iconResId);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            onClickSelector();
            return true;
        }

    }

    public class CombinedListAdapter extends RecyclerView.Adapter<CombinedHolder> {

        private static final int TASK = 0;
        private static final int PROJECT = 1;
        private static final int CONTEXT = 2;
        private static final int LABEL = 3;

        protected List<Object> mItems = new ArrayList<>();

        @Override
        public int getItemCount() {
            return (mItems == null) ? 0 : mItems.size();
        }

        @Override
        public long getItemId(int position) {
            if (this.mItems != null && this.mItems.size() > position) {
                Object item = mItems.get(position);
                if (item instanceof Entity) {
                    return ((Entity) item).getLocalId().getId();
                } else if (item instanceof EntityLabel) {
                    return item.hashCode();
                }
            }
            return super.getItemId(position);
        }

        public int getItemViewType(int position) {
            Object item = mItems.get(position);
            return (item instanceof Task) ? TASK :
                (item instanceof Context) ? CONTEXT :
                (item instanceof Project) ? PROJECT : LABEL;
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public CombinedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CombinedHolder combinedHolder;
            switch (viewType) {
                case TASK: {
                    TaskListItem listItem = mTaskListItemProvider.get(getActivity());
                    combinedHolder = new CombinedHolder(listItem);
                    listItem.setSelectorClickListener(combinedHolder);
                    break;
                }
                default: {
                    EntityListItem listItem = mEntityListItemProvider.get(getActivity());
                    combinedHolder = new CombinedHolder(listItem);
                    listItem.setSelectorClickListener(combinedHolder);
                    break;
                }
            }
            return combinedHolder;
        }

        @Override
        public void onBindViewHolder(CombinedHolder holder, int position) {
            Object item = mItems.get(position);
            if (item != null && mLocation != null) {
                holder.bindEntity(item);
            }
        }

        public void changeCursors(Cursor contextListCursor,
                                  Cursor projectListCursor, Cursor taskListCursor) {
            mItems.clear();
            List<Context> contexts = Arrays.asList(mContextPersister.readAll(contextListCursor));
            if (!contexts.isEmpty()) {
                mItems.add(createLabel(R.string.title_context, R.drawable.ic_label_black_24dp));
                mItems.addAll(contexts);
            }
            List<Project> projects = Arrays.asList(mProjectPersister.readAll(projectListCursor));
            if (!projects.isEmpty()) {
                mItems.add(createLabel(R.string.title_project, R.drawable.ic_folder_black_24dp));
                mItems.addAll(projects);
            }
            List<Task> tasks = new ArrayList<>(Arrays.asList(mTaskPersister.readAll(taskListCursor)));
            if (!tasks.isEmpty()) {
                mItems.add(createLabel(R.string.title_task, R.drawable.ic_assignment_black_24dp));
                Collections.sort(tasks, mComparator);
                mItems.addAll(tasks);
            }
            this.notifyDataSetChanged();
        }
    }

    private EntityLabel createLabel(int stringResId, int iconResId) {
        EntityLabel entityLabel = new EntityLabel();
        entityLabel.name = getResources().getString(stringResId);
        entityLabel.iconResId = iconResId;
        return entityLabel;
    }

    private class CombinedModalMultiSelectorCallback extends ModalMultiSelectorCallback {
        private MenuItem mMarkUndelete;

        public CombinedModalMultiSelectorCallback(MultiSelector multiSelector) {
            super(multiSelector);
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.deleted_list_context_menu, menu);

            mMarkUndelete = menu.findItem(R.id.action_undelete);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {

                case R.id.action_undelete:
                    restoreSelectedEntities();
                    mActionMode.finish();
                    return true;

            }
            return false;
        }

        private void restoreSelectedEntities() {
            List<Integer> selectedPositions = mMultiSelector.getSelectedPositions();
            Set<Long> taskIds = new HashSet<>();
            Set<Long> projectIds = new HashSet<>();
            Set<Long> contextIds = new HashSet<>();
            for (Integer position : selectedPositions) {
                Object item = mListAdapter.getItem(position);
                if (item instanceof Entity) {
                    long id = ((Entity)item).getLocalId().getId();
                    if (item instanceof Task) {
                        taskIds.add(id);
                    } else if (item instanceof Project) {
                        projectIds.add(id);
                    } else if (item instanceof Context) {
                        contextIds.add(id);
                    }
                }
            }
            if (!taskIds.isEmpty()) {
                mEventManager.fire(new UpdateTasksDeletedEvent(taskIds, false));
            }
            if (!projectIds.isEmpty()) {
                mEventManager.fire();
            }
            if (!contextIds.isEmpty()) {
                mEventManager.fire();
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            mMultiSelector.clearSelections();
            mActionMode = null;
        }

    }

    class EntityLabel {
        String name;
        int iconResId;
    }

}
