package org.dodgybits.shuffle.android.view.fragment;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.CalendarUtils;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.list.view.LabelView;
import org.dodgybits.shuffle.android.list.view.StatusView;
import roboguice.event.EventManager;
import roboguice.fragment.RoboFragment;

import java.util.Collections;
import java.util.List;

public class TaskViewFragment extends RoboFragment implements View.OnClickListener {
    private static final String TAG = "TaskViewFragment";

    public static final String SELECTED_INDEX = "selectedIndex";
    public static final String ARG_LIST_CONTEXT = "taskListContext";

    public static final String INDEX = "TaskViewFragment.index";
    public static final String COUNT = "TaskViewFragment.count";

    private ViewGroup mContextContainer;

    private TextView mProjectView;
    private TextView mDescriptionView;

    private ViewGroup mDetailsRow;
    private TextView mDetailsView;

    private ViewGroup mShowFromRow;
    private TextView mShowFromView;

    private ViewGroup mDueRow;
    private TextView mDueView;

    private ViewGroup mCalendarRow;
    private Button mViewCalendarButton;

    private StatusView mStatusView;

    private Button mNoCompleteButton;
    private Button mYesCompleteButton;

    @Inject private EntityCache<Project> mProjectCache;
    @Inject private EntityCache<Context> mContextCache;
    @Inject private TaskPersister mPersister;
    @Inject private TaskEncoder mEncoder;
    @Inject private LocationProvider mLocationProvider;

    @Inject
    private EventManager mEventManager;

    private Task mTask;
    private int mPosition;
    private int mTaskCount;

    public static TaskViewFragment newInstance(Bundle args) {
        TaskViewFragment fragment = new TaskViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.task_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        findViews();
        updateUIFromItem(getTask());

        mViewCalendarButton.setOnClickListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateMenuVisibility(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                Log.d(TAG, "Editing the action");
                Location location = Location.editTask(mTask.getLocalId());
                mEventManager.fire(new NavigationRequestEvent(location));
                return true;
            case R.id.action_delete:
            case R.id.action_undelete:
                Log.d(TAG, "Toggling delete on task");
                mEventManager.fire(new UpdateTasksDeletedEvent(mTask.getLocalId().getId(), !mTask.isDeleted()));
                Location parentLocation = mLocationProvider.getLocation().builderFrom().parentView().build();
                mEventManager.fire(new NavigationRequestEvent(parentLocation));
                return true;
        }
        return false;
    }

    private void toggleComplete() {
        Log.d(TAG, "Toggling complete on task");
        mEventManager.fire(new UpdateTasksCompletedEvent(mTask.getLocalId().getId(), !mTask.isComplete()));
        Location parentLocation = mLocationProvider.getLocation().builderFrom().parentView().build();
        mEventManager.fire(new NavigationRequestEvent(parentLocation));
    }

    private void initializeArgCache() {
        if (mTask != null) return;
        Bundle args = getArguments();
        mTask = mEncoder.restore(args);
        mTaskCount = args.getInt(COUNT, -1);
        mPosition = args.getInt(INDEX, -1);
    }

    private Task getTask() {
        if (mTask == null) {
            initializeArgCache();
        }
        return mTask;
    }

    private void updateMenuVisibility(Menu menu) {
        if (getTask() != null) {
            final boolean isComplete = getTask().isComplete();
            setVisible(menu, R.id.action_mark_complete, !isComplete);
            setVisible(menu, R.id.action_mark_incomplete, isComplete);
            final boolean isDeleted = getTask().isDeleted();
            setVisible(menu, R.id.action_delete, !isDeleted);
            setVisible(menu, R.id.action_undelete, isDeleted);
        }
    }

    private void setVisible(Menu menu, int id, boolean visible) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    private void findViews() {
        mProjectView = (TextView) getView().findViewById(R.id.project);
        mDescriptionView = (TextView) getView().findViewById(R.id.description);
        mContextContainer = (ViewGroup) getView().findViewById(R.id.context_container);
        mDetailsRow = (ViewGroup) getView().findViewById(R.id.details_row);
        mDetailsView = (TextView) getView().findViewById(R.id.details);
        mShowFromRow = (ViewGroup) getView().findViewById(R.id.show_from_row);
        mShowFromView = (TextView) getView().findViewById(R.id.show_from);
        mDueRow = (ViewGroup) getView().findViewById(R.id.due_row);
        mDueView = (TextView) getView().findViewById(R.id.due);
        mCalendarRow = (ViewGroup) getView().findViewById(R.id.calendar_row);
        mViewCalendarButton = (Button) getView().findViewById(R.id.view_calendar_button);
        mStatusView = (StatusView) getView().findViewById(R.id.status);
        mNoCompleteButton = (Button) getView().findViewById(R.id.no_complete);
        mYesCompleteButton = (Button) getView().findViewById(R.id.yes_complete);

        View.OnClickListener completeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleComplete();
            }
        };

        mNoCompleteButton.setOnClickListener(completeListener);
        mYesCompleteButton.setOnClickListener(completeListener);
    }

    private void updateUIFromItem(Task task) {
        List<Context> contexts = mContextCache.findById(task.getContextIds());
        Project project = mProjectCache.findById(task.getProjectId());

        updateProject(project);
        updateDescription(task.getDescription());
        updateContexts(contexts);
        updateDetails(task.getDetails());
        updateScheduling(task.getStartDate(), task.getDueDate(), task.isAllDay());
        updateCalendar(task.getCalendarEventId());
        updateExtras(task, contexts, project);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.view_calendar_button: {
                Uri eventUri = ContentUris.appendId(
                        CalendarUtils.getEventContentUri().buildUpon(),
                        getTask().getCalendarEventId().getId()).build();
                Intent viewCalendarEntry = new Intent(Intent.ACTION_VIEW, eventUri);
                viewCalendarEntry.putExtra(CalendarUtils.EVENT_BEGIN_TIME, mTask.getStartDate());
                viewCalendarEntry.putExtra(CalendarUtils.EVENT_END_TIME, mTask.getDueDate());
                startActivity(viewCalendarEntry);
                break;
            }
        }
    }

    private void updateProject(Project project) {
        if (project == null) {
            mProjectView.setVisibility(View.GONE);
        } else {
            mProjectView.setVisibility(View.VISIBLE);
            mProjectView.setText(project.getName());
        }

    }

    private void updateDescription(String description) {
        mDescriptionView.setTextKeepState(description);
    }

    private void updateContexts(List<Context> contexts) {
        // don't show deleted contexts
        contexts = Lists.newArrayList(Iterables.filter(contexts, new Predicate<Context>() {
            @Override
            public boolean apply(Context context) {
                return !context.isDeleted();
            }
        }));

        if (contexts.isEmpty()) {
            mContextContainer.setVisibility(View.INVISIBLE);
        } else {
            Collections.sort(contexts);
            // reuse existing views if present
            int viewCount = mContextContainer.getChildCount();
            int contextCount = contexts.size();
            while (viewCount < contextCount) {
                LabelView contextView = new LabelView(getActivity());
                mContextContainer.addView(contextView);
                viewCount++;
            }
            if (viewCount > contextCount) {
                mContextContainer.removeViews(contextCount, viewCount - contextCount);
                viewCount = contextCount;
            }
            
            for (int i = 0; i < contextCount; i++) {
                LabelView contextView = (LabelView) mContextContainer.getChildAt(i);
                Context context = contexts.get(i);
                contextView.setText(context.getName());
                contextView.setColourIndex(context.getColourIndex());
                ContextIcon contextIcon = ContextIcon.createIcon(context.getIconName(), getResources(), true);
                Drawable icon = contextIcon == null ? null : getResources().getDrawable(contextIcon.smallIconId);
                contextView.setIcon(icon);
            }
        }
    }

    private void updateDetails(String details) {
        if (TextUtils.isEmpty(details)) {
            mDetailsRow.setVisibility(View.GONE);
        } else {
            mDetailsRow.setVisibility(View.VISIBLE);
            mDetailsView.setText(details);
        }
    }

    private void updateScheduling(long showFromMillis, long dueMillis, boolean allDay) {
        if (showFromMillis == 0L) {
            mShowFromRow.setVisibility(View.GONE);
        } else {
            mShowFromRow.setVisibility(View.VISIBLE);
            mShowFromView.setText(formatDateTime(showFromMillis, false));
        }

        if (dueMillis == 0L) {
            mDueRow.setVisibility(View.GONE);
        } else {
            mDueRow.setVisibility(View.VISIBLE);
            mDueView.setText(formatDateTime(dueMillis, false));
        }
    }

    private void updateCalendar(Id calendarEntry) {
        if (calendarEntry.isInitialised()) {
            mCalendarRow.setVisibility(View.VISIBLE);
        } else {
            mCalendarRow.setVisibility(View.GONE);
        }
    }

    private CharSequence formatDateTime(long millis, boolean withPreposition) {
        CharSequence value;
        if (millis > 0L) {
            value = DateUtils.getRelativeTimeSpanString(getActivity(), millis, withPreposition);
        } else {
            value = "";
        }

        return value;
    }

    private void updateExtras(Task task, List<Context> contexts, Project project) {
        mStatusView.updateStatus(task, contexts, project, true);
        mStatusView.setVisibility(task.isComplete() ? View.INVISIBLE : View.VISIBLE);

        if (task.isComplete()) {
            mYesCompleteButton.setTextColor(getResources().getColor(R.color.theme_accent));
            mNoCompleteButton.setTextColor(getResources().getColor(R.color.black));
        } else {
            mYesCompleteButton.setTextColor(getResources().getColor(R.color.black));
            mNoCompleteButton.setTextColor(getResources().getColor(R.color.theme_accent));
        }
    }


}
