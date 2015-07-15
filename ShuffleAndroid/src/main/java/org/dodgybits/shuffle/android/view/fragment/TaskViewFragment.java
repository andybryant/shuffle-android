package org.dodgybits.shuffle.android.view.fragment;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.CalendarUtils;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.list.view.LabelView;
import org.dodgybits.shuffle.android.list.view.StatusView;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;

import java.util.Collections;
import java.util.List;

public class TaskViewFragment extends RoboFragment implements View.OnClickListener {
    private static final String TAG = "TaskViewFragment";

    public static final String ID = "id";


    private ViewGroup mContextContainer;
    private ViewGroup mContextRow;

    private TextView mProjectView;
    private TextView mDescriptionView;

    private ViewGroup mDetailsRow;
    private TextView mDetailsView;

    private ImageView mCompleteFabIcon;

    private ViewGroup mTemporalRow;
    private TextView mTemporalView;

    private ViewGroup mCalendarRow;
    private Button mViewCalendarButton;

    private StatusView mStatusView;

    @Inject private EntityCache<Project> mProjectCache;
    @Inject private EntityCache<Context> mContextCache;
    @Inject private TaskPersister mPersister;
    @Inject private LocationProvider mLocationProvider;
    @Inject private CursorProvider mCursorProvider;

    @Inject
    private EventManager mEventManager;

    private Task mTask;
    Cursor mCursor;

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
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.task_view, container, false);
        FontUtils.setCustomFont(view, getActivity().getAssets());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "+onActivityCreated");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "+onResume");

        findViews();

        mViewCalendarButton.setOnClickListener(this);

        getView().findViewById(R.id.complete_fab).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleComplete();
                    }
                }
        );

        updateCursor();
    }

    private void onCursorUpdated(@Observes CursorUpdatedEvent event) {
        updateCursor();
    }

    private void updateCursor() {
        updateCursor(mCursorProvider.getTaskListCursor());
    }

    private void updateCursor(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        mCursor = cursor;

        getTask();
        updateUIFromItem();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "+onPause");
        mTask = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateMenuVisibility(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                editTask();
                return true;
            case R.id.action_mark_complete:
            case R.id.action_mark_incomplete:
                toggleComplete();
                return true;
            case R.id.action_delete:
            case R.id.action_undelete:
                Log.d(TAG, "Toggling delete on task");
                mEventManager.fire(new UpdateTasksDeletedEvent(mTask.getLocalId().getId(), !mTask.isDeleted()));
                mEventManager.fire(new NavigationRequestEvent(mLocationProvider.getLocation().parent()));
                return true;
        }
        return false;
    }

    public long getTaskId() {
        return mTask.getLocalId().getId();
    }

    private void editTask() {
        Log.d(TAG, "Editing the action");
        Location location = Location.editTask(mTask.getLocalId());
        mEventManager.fire(new NavigationRequestEvent(location));
    }

    private void toggleComplete() {
        long taskId = mTask.getLocalId().getId();
        Log.d(TAG, "Toggling complete on task " + mTask.getDescription() + " id=" + taskId + " tag=" + getTag());
        mEventManager.fire(new UpdateTasksCompletedEvent(taskId, !mTask.isComplete()));
        mEventManager.fire(new NavigationRequestEvent(mLocationProvider.getLocation().parent()));
    }

    private void initializeArgCache() {
        if (mTask != null) return;
        Bundle args = getArguments();
        long id = args.getLong(ID);
        int position = findPosition(id);
        mCursor.moveToPosition(position);
        mTask = mPersister.read(mCursor);
        Log.d(TAG, "Read task from cursor at position " + position);
    }

    private int findPosition(long id) {
        int position = 0;
        int length = mCursor.getCount();
        for (int i = 0; i < length; i++) {
            mCursor.moveToPosition(i);
            if (mCursor.getLong(0) == id) {
                position = i;
                break;
            }
        }
        return position;
    }

    private Task getTask() {
        if (mTask == null) {
            initializeArgCache();
        }
        return mTask;
    }

    private void updateMenuVisibility(Menu menu) {
        if (mTask != null) {
            final boolean isComplete = mTask.isComplete();
            setVisible(menu, R.id.action_mark_complete, !isComplete);
            setVisible(menu, R.id.action_mark_incomplete, isComplete);
            final boolean isDeleted = mTask.isDeleted();
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
        mContextRow = (ViewGroup) getView().findViewById(R.id.context_row);
        mContextContainer = (ViewGroup) getView().findViewById(R.id.context_container);
        mDetailsRow = (ViewGroup) getView().findViewById(R.id.details_row);
        mDetailsView = (TextView) getView().findViewById(R.id.details);
        mTemporalRow = (ViewGroup) getView().findViewById(R.id.temporal_row);
        mTemporalView = (TextView) getView().findViewById(R.id.temporal);
        mCalendarRow = (ViewGroup) getView().findViewById(R.id.calendar_row);
        mViewCalendarButton = (Button) getView().findViewById(R.id.view_calendar_button);
        mStatusView = (StatusView) getView().findViewById(R.id.status);
        mCompleteFabIcon = (ImageView) getView().findViewById(R.id.complete_fab);
    }

    private void updateUIFromItem() {
        if (mTask == null) return;
        List<Context> contexts = mContextCache.findById(mTask.getContextIds());
        Project project = mProjectCache.findById(mTask.getProjectId());

        updateProject(project);
        updateDescription(mTask.getDescription());
        updateContexts(contexts);
        updateDetails(mTask.getDetails());
        updateTemporal(mTask.getStartDate(), mTask.getDueDate(), mTask.isAllDay());
        updateCalendar(mTask.getCalendarEventId());
        updateExtras(mTask, contexts, project);
        updateCompleteFab(mTask.isComplete());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.view_calendar_button: {
                Uri eventUri = ContentUris.appendId(
                        CalendarUtils.getEventContentUri().buildUpon(),
                        mTask.getCalendarEventId().getId()).build();
                Intent viewCalendarEntry = new Intent(Intent.ACTION_VIEW, eventUri);
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
            mContextRow.setVisibility(View.GONE);
        } else {
            mContextRow.setVisibility(View.VISIBLE);
            Collections.sort(contexts);
            // reuse existing views if present
            int viewCount = mContextContainer.getChildCount();
            int contextCount = contexts.size();
            while (viewCount < contextCount) {
                LabelView contextView = new LabelView(getActivity());
                contextView.setTag(FontUtils.ALL_CAPS);
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
            FontUtils.setCustomFont(mContextContainer, getActivity().getAssets());
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

    private void updateTemporal(long deferUntilMillis, long dueMillis, boolean allDay) {
        boolean deferInPast = deferUntilMillis < System.currentTimeMillis();
        boolean dueIsSet = dueMillis > 0L;
        boolean dueInPast = dueMillis < System.currentTimeMillis();
        if (deferInPast && !dueIsSet) {
            mTemporalRow.setVisibility(View.GONE);
        } else {
            mTemporalRow.setVisibility(View.VISIBLE);
            StringBuilder text = new StringBuilder();
            if (!deferInPast) {
                text.append(getResources().getString(R.string.deferred_until_phrase, formatDateTime(deferUntilMillis)));
                text.append(".\n");
            }
            if (dueIsSet) {
                text.append(getResources().getString(R.string.due_phrase, formatDateTime(dueMillis)));
            }
            mTemporalView.setTextColor(getResources().getColor(
                    dueIsSet && dueInPast ? R.color.theme_primary_dark : R.color.label_color));
            mTemporalView.setText(text);
        }
    }

    private void updateCalendar(Id calendarEntry) {
        if (calendarEntry.isInitialised()) {
            mCalendarRow.setVisibility(View.VISIBLE);
        } else {
            mCalendarRow.setVisibility(View.GONE);
        }
    }

    private CharSequence formatDateTime(long millis) {
        int fullFlags = DateUtils.FORMAT_SHOW_TIME |
                DateUtils.FORMAT_SHOW_WEEKDAY  |
                DateUtils.FORMAT_ABBREV_WEEKDAY |
                DateUtils.FORMAT_SHOW_DATE;
        CharSequence value;
        if (millis > 0L) {
            value = DateUtils.formatDateTime(getActivity(), millis, fullFlags);
        } else {
            value = "";
        }

        return value;
    }

    private void updateExtras(Task task, List<Context> contexts, Project project) {
        mStatusView.updateStatus(task, contexts, project, true);
        mStatusView.setVisibility(task.isComplete() ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateCompleteFab(boolean isComplete) {
        mCompleteFabIcon.setImageResource(isComplete ? R.drawable.ic_done_green_24dp : R.drawable.ic_done_white_24dp);
    }


}
