package org.dodgybits.shuffle.android.editor.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.CalendarUtils;
import org.dodgybits.shuffle.android.core.util.EntityUtils;
import org.dodgybits.shuffle.android.core.util.OSUtils;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.editor.activity.EditTaskActivity;
import org.dodgybits.shuffle.android.list.view.LabelView;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;

import java.util.List;
import java.util.TimeZone;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

public class EditTaskFragment extends AbstractEditFragment<Task>
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "EditTaskFragment";


    private static final String[] PROJECT_PROJECTION = new String[] {
            ProjectProvider.Projects._ID,
            ProjectProvider.Projects.NAME
    };

    private static final int NEW_CONTEXT_CODE = 100;
    private static final int NEW_PROJECT_CODE = 101;

    private EditText mDescriptionWidget;
    private EditText mDetailsWidget;

    private ViewGroup mContextContainer;
    private List<Id> mSelectedContextIds = Lists.newArrayList();
    private TextView mNoContexts;
    
    private Spinner mProjectSpinner;
    private String[] mProjectNames;
    private long[] mProjectIds;

    private boolean mSchedulingExpanded;
    private TextView mShowFromDateButton;
    private TextView mShowFromTimeButton;
    private TextView mDueDateButton;
    private TextView mDueTimeButton;
    private CheckBox mAllDayCheckBox;

    private boolean mShowFromDateVisible;
    private Time mShowFromTime;
    private boolean mDueDateVisible;
    private Time mDueTime;

    private View mSchedulingExtra;
    private TextView mSchedulingDetail;
    private View mExpandButton;
    private View mCollapseButton;

    private View mCompleteEntry;
    private CheckBox mCompletedCheckBox;

    private View mDeletedEntry;
    private View mDeletedDivider;
    private CheckBox mDeletedCheckBox;

    private View mUpdateCalendarEntry;
    private CheckBox mUpdateCalendarCheckBox;
    private TextView mCalendarLabel;
    private TextView mCalendarDetail;

    @Inject
    private EntityCache<Project> mProjectCache;

    @Inject
    private EntityCache<Context> mContextCache;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mShowFromTime = new Time();
        mDueTime = new Time();

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case NEW_CONTEXT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long newContextId = ContentUris.parseId(data.getData());
                        addNewContext(Id.create(newContextId));
                    }
                }
                break;
            case NEW_PROJECT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long newProjectId = ContentUris.parseId(data.getData());
                        setupProjectSpinner();
                        setSpinnerSelection(mProjectSpinner, mProjectIds, newProjectId);
                    }
                }
                break;
            
            default:
                Log.e(TAG, "Unknown requestCode: " + requestCode);
        }
    }

    public List<Id> getSelectedContextIds() {
        return mSelectedContextIds;
    }

    public void setSelectedContextIds(List<Id> selectedContextIds) {
        mSelectedContextIds = selectedContextIds;
        updateContextPanel();
    }

    @Override
    protected boolean isValid() {
        String description = mDescriptionWidget.getText().toString();
        return !TextUtils.isEmpty(description);
    }

    @Override
    protected void updateUIFromExtras(Bundle extras) {
        if (extras != null) {
            long contextId = extras.getLong(TaskProvider.TaskContexts.CONTEXT_ID, 0L);
            if (contextId != 0L) {
                replaceContexts(new long[] {contextId});
            }

            long projectId = extras.getLong(TaskProvider.Tasks.PROJECT_ID, 0L);
            setSpinnerSelection(mProjectSpinner, mProjectIds, projectId);
            
            applyDefaultContext();
        }

        mCompleteEntry.setVisibility(View.GONE);
        mDeletedEntry.setVisibility(View.GONE);
        mDeletedDivider.setVisibility(View.GONE);
        mDeletedCheckBox.setChecked(false);

        populateWhen();

        setSchedulingVisibility(false);

        mShowFromTimeButton.setVisibility(View.VISIBLE);
        mDueTimeButton.setVisibility(View.VISIBLE);
        updateCalendarPanel();
    }


    @Override
    protected void updateUIFromItem(Task task) {
        // If we hadn't previously retrieved the original task, do so
        // now.  This allows the user to revert their changes.
        if (mOriginalItem == null) {
            mOriginalItem = task;
        }

        mCompleteEntry.setVisibility(View.VISIBLE);

        final String details = task.getDetails();
        mDetailsWidget.setTextKeepState(details == null ? "" : details);

        mDescriptionWidget.setTextKeepState(task.getDescription());

        mSelectedContextIds = task.getContextIds();
        updateContextPanel();

        final Id projectId = task.getProjectId();
        if (projectId.isInitialised()) {
            setSpinnerSelection(mProjectSpinner, mProjectIds, projectId.getId());
        }

        boolean allDay = task.isAllDay();
        if (allDay && task.getStartDate() != 0L) {
            String tz = mShowFromTime.timezone;
            mShowFromTime.timezone = Time.TIMEZONE_UTC;
            mShowFromTime.set(task.getStartDate());
            mShowFromTime.timezone = tz;

            // Calling normalize to calculate isDst
            mShowFromTime.normalize(true);
        } else {
            mShowFromTime.set(task.getStartDate());
        }

        if (allDay && task.getDueDate() != 0L) {
            String tz = mDueTime.timezone;
            mDueTime.timezone = Time.TIMEZONE_UTC;
            mDueTime.set(task.getDueDate());
            mDueTime.timezone = tz;

            // Calling normalize to calculate isDst
            mDueTime.normalize(true);
        } else {
            mDueTime.set(task.getDueDate());
        }

        populateWhen();

        // show scheduling section if either start or due date are set
        mSchedulingExpanded = mShowFromDateVisible || mDueDateVisible;
        setSchedulingVisibility(mSchedulingExpanded);

        mAllDayCheckBox.setChecked(allDay);
        updateTimeVisibility(!allDay);

        mCompletedCheckBox.setChecked(task.isComplete());

        mDeletedEntry.setVisibility(task.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedDivider.setVisibility(task.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedCheckBox.setChecked(task.isDeleted());

        updateCalendarPanel();
    }

    @Override
    protected Task createItemFromUI(boolean commitValues) {
        boolean changed = false;
        Task.Builder builder = Task.newBuilder();
        if (mOriginalItem != null) {
            builder.mergeFrom(mOriginalItem);
        }

        final String description = mDescriptionWidget.getText().toString();
        final long modified = System.currentTimeMillis();
        final String details = mDetailsWidget.getText().toString();
        final Id projectId = getSpinnerSelectedId(mProjectSpinner, mProjectIds);
        final boolean allDay = mAllDayCheckBox.isChecked();
        final boolean complete = mCompletedCheckBox.isChecked();
        final boolean deleted = mDeletedCheckBox.isChecked();
        final boolean active = true;

        TaskChangeSet changeSet = builder.getChangeSet();

        if (!ObjectUtils.equals(description, builder.getDescription())) {
            builder.setDescription(description);
            changeSet.descriptionChanged();
        }
        if (!ObjectUtils.equals(details, builder.getDetails())) {
            builder.setDetails(details);
            changeSet.detailsChanged();
            changed = true;
        }
        if (!ObjectUtils.equals(projectId, builder.getProjectId())) {
            builder.setProjectId(projectId);
            changeSet.projectChanged();
            changed = true;
        }
        if (allDay != builder.isAllDay()) {
            builder.setAllDay(allDay);
            changeSet.allDayChanged();
            changed = true;
        }
        if (complete != builder.isComplete()) {
            builder.setComplete(complete);
            changeSet.completeChanged();
            changed = true;
        }
        if (deleted != builder.isDeleted()) {
            builder.setDeleted(deleted);
            changeSet.deleteChanged();
            changed = true;
        }
        if (active != builder.isActive()) {
            builder.setActive(active);
            changeSet.activeChanged();
            changed = true;
        }
        if (!EntityUtils.idsMatch(builder.getContextIds(), mSelectedContextIds)) {
            builder.setContextIds(mSelectedContextIds);
            changeSet.contextsChanged();
            changed = true;
        }

        builder.setModifiedDate(modified);
        // If we are creating a new task, set the creation date
        if (mIsNewEntity) {
            builder.setCreatedDate(modified);
        }

        String timezone;
        long showFromMillis = 0L;
        long dueMillis = 0L;

        if (mIsNewEntity) {
            // The timezone for a new task is the currently displayed timezone
            timezone = TimeZone.getDefault().getID();
        }
        else
        {
            timezone = mOriginalItem.getTimezone();

            // The timezone might be null if we are changing an existing
            // all-day task to a non-all-day event.  We need to assign
            // a timezone to the non-all-day task.
            if (TextUtils.isEmpty(timezone)) {
                timezone = TimeZone.getDefault().getID();
            }
        }

        if (allDay) {
            // Reset start and end time, increment the monthDay by 1, and set
            // the timezone to UTC, as required for all-day events.
            if (mShowFromDateVisible) {
                timezone = Time.TIMEZONE_UTC;
                mShowFromTime.timezone = timezone;
                mShowFromTime.hour = 0;
                mShowFromTime.minute = 0;
                mShowFromTime.second = 0;
                showFromMillis = mShowFromTime.toMillis(true);
            }

            if (mDueDateVisible) {
                timezone = Time.TIMEZONE_UTC;
                mDueTime.timezone = timezone;
                mDueTime.hour = 0;
                mDueTime.minute = 0;
                mDueTime.second = 0;
                dueMillis = mDueTime.toMillis(true);
            }
        } else {
            if (mShowFromDateVisible && !Time.isEpoch(mShowFromTime)) {
                mShowFromTime.timezone = timezone;
                showFromMillis = mShowFromTime.toMillis(true);
            }

            if (mDueDateVisible && !Time.isEpoch(mDueTime)) {
                mDueTime.timezone = timezone;
                dueMillis = mDueTime.toMillis(true);
            }
        }

        final int order;
        if (commitValues) {
            order = ((TaskPersister)mPersister).calculateTaskOrder(mOriginalItem, projectId);
        } else if (mOriginalItem == null) {
            order = 0;
        } else {
            order = mOriginalItem.getOrder();
        }

        builder.setTimezone(timezone);

        if (showFromMillis != builder.getStartDate()) {
            builder.setStartDate(showFromMillis);
            changeSet.showFromChanged();
            changed = true;
        }
        if (dueMillis != builder.getDueDate()) {
            builder.setDueDate(dueMillis);
            changeSet.dueChanged();
            changed = true;
        }
        if (order != builder.getOrder()) {
            builder.setOrder(order);
            changeSet.orderChanged();
            changed = true;
        }

        Id eventId = mOriginalItem == null ? Id.NONE : mOriginalItem.getCalendarEventId();
        final boolean updateCalendar = mUpdateCalendarCheckBox.isChecked();

        if (commitValues && updateCalendar) {
            long startMillis = showFromMillis > 0L ? showFromMillis : dueMillis;
            long endMillis = dueMillis > 0L ? dueMillis : showFromMillis;

            if (allDay) {
                if (endMillis < startMillis) {
                    endMillis = startMillis;
                }
                endMillis += DateUtils.DAY_IN_MILLIS;
            } else {
                if (endMillis < startMillis + DateUtils.HOUR_IN_MILLIS) {
                    endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
                }
            }

            Uri calEntryUri = addOrUpdateCalendarEvent(
                    eventId, description, details,
                    projectId, timezone, startMillis,
                    endMillis, allDay);
            if (calEntryUri != null) {
                eventId = Id.create(ContentUris.parseId(calEntryUri));
                mNextIntent = new Intent(Intent.ACTION_EDIT, calEntryUri);
                mNextIntent.putExtra("beginTime", startMillis);
                mNextIntent.putExtra("endTime", endMillis);
            }
            Log.i(TAG, "Updated calendar event " + eventId);
        }
        builder.setCalendarEventId(eventId);
        builder.setChangeSet(changeSet);

        if (commitValues && changed) {
            Log.d(TAG, "Task updated - schedule sync");
            SyncUtils.scheduleSync(getActivity(), LOCAL_CHANGE_SOURCE);
        }
        return builder.build();
    }

    /**
     * When a project is selected and the context is empty, set it
     * to the project default.
     */
    private void applyDefaultContext() {
        Id projectId = getSpinnerSelectedId(mProjectSpinner, mProjectIds);
        if (projectId.isInitialised() && mSelectedContextIds.isEmpty()) {
            Project project = mProjectCache.findById(projectId);
            if (project != null) {
                Id contextId = project.getDefaultContextId();
                if (contextId.isInitialised()) {
                    addNewContext(contextId);
                }
            }
        }
    }


    private Uri addOrUpdateCalendarEvent(
            Id calEventId, String title, String description,
            Id projectId, String timezone, 
            long startMillis, long endMillis, boolean allDay) {
        if (projectId.isInitialised()) {
            String projectName = getProjectName(projectId);
            title = projectName + " - " + title;
        }
        if (description == null) {
            description = "";
        }

        ContentValues values = new ContentValues();
        values.put("eventTimezone", timezone);
        values.put("calendar_id", Preferences.getCalendarId(getActivity()));
        values.put("title", title);
        values.put("allDay", allDay ? 1 : 0);


        values.put("dtstart", startMillis); // long (start date in ms)
        values.put("dtend", endMillis);     // long (end date in ms)
        values.put("duration", (String) null);

        values.put("description", description);


        List<Context> contexts = mContextCache.findById(mSelectedContextIds);
        if (!contexts.isEmpty()) {
            List<String> names = Lists.transform(contexts, new Function<Context, String>() {
                @Override
                public String apply(Context input) {
                    return input.getName();
                }
            });
            String location = TextUtils.join(", ", names);
            values.put("eventLocation", location);
        }

        Uri eventUri = null;
        try {
            eventUri = addCalendarEntry(values, calEventId, CalendarUtils.getEventContentUri());
        } catch (Exception e) {
            Log.e(TAG, "Attempt failed to create calendar entry", e);
        }

        return eventUri;
    }

    private Uri addCalendarEntry(ContentValues values, Id oldId, Uri baseUri) {
        ContentResolver cr = getActivity().getContentResolver();
        int updateCount = 0;
        Uri eventUri = null;
        if (oldId.isInitialised()) {
            eventUri = ContentUris.appendId(baseUri.buildUpon(), oldId.getId()).build();
            // it's possible the old event was deleted, check number of records updated
            updateCount = cr.update(eventUri, values, null, null);
        }
        if (updateCount == 0) {
            eventUri = cr.insert(baseUri, values);

            addReminder(eventUri);
        }
        return eventUri;
    }

    private Uri addReminder(Uri eventUri) {
        Uri uri = null;
        try {
            ContentResolver cr = getActivity().getContentResolver();
            ContentValues values = new ContentValues();
            long id = ContentUris.parseId(eventUri);
            values.put("minutes", 15);
            values.put("event_id", id);
            values.put("method", 1 /* alert */);
            uri = cr.insert(Uri.parse("content://com.android.calendar/reminders"), values);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add reminder " + e);
        }
        return uri;
    }

    /**
     * @return id of layout for this view
     */
    @Override
    protected int getContentViewResId() {
        return R.layout.task_editor;
    }

    @Override
    protected CharSequence getItemName() {
        return getString(R.string.task_name);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.context_add: {
                Intent addContextIntent = new Intent(Intent.ACTION_INSERT, ContextProvider.Contexts.CONTENT_URI);
                startActivityForResult(addContextIntent, NEW_CONTEXT_CODE);
                break;
            }

            case R.id.context_items_container: {
                getActivity().showDialog(EditTaskActivity.CONTEXT_PICKER_DIALOG);
                break;
            }

            case R.id.project_add: {
                Intent addProjectIntent = new Intent(Intent.ACTION_INSERT, ProjectProvider.Projects.CONTENT_URI);
                startActivityForResult(addProjectIntent, NEW_PROJECT_CODE);
                break;
            }

            case R.id.scheduling_entry: {
                toggleSchedulingSection();
                break;
            }

            case R.id.completed_entry: {
                mCompletedCheckBox.toggle();
                break;
            }

            case R.id.deleted_entry: {
                mDeletedCheckBox.toggle();
                break;
            }

            case R.id.gcal_entry: {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.update_calendar_checkbox);
                checkBox.toggle();
                break;
            }

            case R.id.clear_from_date: {
                mAllDayCheckBox.setChecked(false);
                mShowFromTime.set(0L);
                populateWhen();
                updateCalendarPanel();
                break;
            }

            case R.id.clear_due_date: {
                mAllDayCheckBox.setChecked(false);
                mDueTime.set(0L);
                populateWhen();
                updateCalendarPanel();
                break;
            }

            default:
                super.onClick(v);
                break;
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateTimeVisibility(!isChecked);
    }

    private void updateTimeVisibility(boolean showTime) {
        if (showTime) {
            mShowFromTimeButton.setVisibility(View.VISIBLE);
            mDueTimeButton.setVisibility(View.VISIBLE);
        } else {
            mShowFromTimeButton.setVisibility(View.GONE);
            mDueTimeButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void loadCursor() {
        // Get the task if we're editing
        if (mUri != null && !mIsNewEntity)
        {
            mCursor = getActivity().managedQuery(mUri, TaskProvider.Tasks.FULL_PROJECTION, null, null, null);
            if (mCursor == null || mCursor.getCount() == 0) {
                // The cursor is empty. This can happen if the event was deleted.
                getActivity().finish();
            }
            mCursor.moveToFirst();
        }
    }

    @Override
    protected void findViewsAndAddListeners() {
        mDescriptionWidget = (EditText) getView().findViewById(R.id.description);
        mProjectSpinner = (Spinner) getView().findViewById(R.id.project);
        mDetailsWidget = (EditText) getView().findViewById(R.id.details);

        mShowFromDateButton = (TextView) getView().findViewById(R.id.show_from_date);
        mShowFromTimeButton = (TextView) getView().findViewById(R.id.show_from_time);
        mDueDateButton = (TextView) getView().findViewById(R.id.due_date);
        mDueTimeButton = (TextView) getView().findViewById(R.id.due_time);
        mAllDayCheckBox = (CheckBox) getView().findViewById(R.id.is_all_day);

        mCompleteEntry = getView().findViewById(R.id.completed_entry);
        mDeletedEntry = getView().findViewById(R.id.deleted_entry);
        mDeletedDivider = getView().findViewById(R.id.deleted_divider);
        mDeletedCheckBox = (CheckBox) getView().findViewById(R.id.deleted_entry_checkbox);
        mUpdateCalendarEntry = getView().findViewById(R.id.gcal_entry);

        mContextContainer = (ViewGroup) getView().findViewById(R.id.context_items_container);
        mContextContainer.setOnClickListener(this);
        mNoContexts = (TextView) getView().findViewById(R.id.no_contexts);

        View addContextButton = getView().findViewById(R.id.context_add);
        addContextButton.setOnClickListener(this);
        addContextButton.setOnFocusChangeListener(this);

        setupProjectSpinner();
        View addProjectButton = getView().findViewById(R.id.project_add);
        addProjectButton.setOnClickListener(this);
        addProjectButton.setOnFocusChangeListener(this);

        mProjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyDefaultContext();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mCompleteEntry.setOnClickListener(this);
        mCompleteEntry.setOnFocusChangeListener(this);
        mCompletedCheckBox = (CheckBox) mCompleteEntry.findViewById(R.id.completed_entry_checkbox);

        mDeletedEntry.setOnClickListener(this);
        mDeletedEntry.setOnFocusChangeListener(this);

        mUpdateCalendarEntry.setOnClickListener(this);
        mUpdateCalendarEntry.setOnFocusChangeListener(this);
        mUpdateCalendarCheckBox = (CheckBox) mUpdateCalendarEntry.findViewById(R.id.update_calendar_checkbox);
        mCalendarLabel = (TextView) mUpdateCalendarEntry.findViewById(R.id.gcal_label);
        mCalendarDetail = (TextView) mUpdateCalendarEntry.findViewById(R.id.gcal_detail);

        mShowFromDateButton.setOnClickListener(new DateClickListener(mShowFromTime));
        mShowFromTimeButton.setOnClickListener(new TimeClickListener(mShowFromTime));
        View clearShowFromButton = getView().findViewById(R.id.clear_from_date);
        clearShowFromButton.setOnClickListener(this);

        mDueDateButton.setOnClickListener(new DateClickListener(mDueTime));
        mDueTimeButton.setOnClickListener(new TimeClickListener(mDueTime));
        View clearDueButton = getView().findViewById(R.id.clear_due_date);
        clearDueButton.setOnClickListener(this);

        mAllDayCheckBox.setOnCheckedChangeListener(this);

        ViewGroup schedulingSection = (ViewGroup) getView().findViewById(R.id.scheduling_section);
        View schedulingEntry = getView().findViewById(R.id.scheduling_entry);
        schedulingEntry.setOnClickListener(this);
        schedulingEntry.setOnFocusChangeListener(this);

        mSchedulingExtra = schedulingSection.findViewById(R.id.scheduling_extra);
        mExpandButton = schedulingEntry.findViewById(R.id.expand);
        mCollapseButton = schedulingEntry.findViewById(R.id.collapse);
        mSchedulingDetail = (TextView) schedulingEntry.findViewById(R.id.scheduling_detail);
        mSchedulingExpanded = mSchedulingExtra.getVisibility() == View.VISIBLE;

    }

    private void addNewContext(Id contextId) {
        if (!mSelectedContextIds.contains(contextId)) {
            mSelectedContextIds.add(contextId);
            updateContextPanel();
        }
    }

    private void replaceContexts(long[] contextIds) {
        mSelectedContextIds.clear();
        for (long contextId : contextIds) {
            mSelectedContextIds.add(Id.create(contextId));
        }
        updateContextPanel();
    }

    private void updateContextPanel() {
        List<Context> contexts = Lists.transform(mSelectedContextIds, new Function<Id, Context>() {
            @Override
            public Context apply(Id id) {
                return mContextCache.findById(id);
            }
        });

        // don't show deleted contexts
        contexts = Lists.newArrayList(Iterables.filter(contexts, new Predicate<Context>() {
            @Override
            public boolean apply(Context context) {
                return !context.isDeleted();
            }
        }));



        int viewCount = mContextContainer.getChildCount();
        if (contexts.isEmpty()) {
            mNoContexts.setVisibility(View.VISIBLE);
            if (viewCount > 1) {
                mContextContainer.removeViews(1, viewCount - 1);

            }
        } else {
            mNoContexts.setVisibility(View.GONE);
            viewCount--; // ignore no contexts view
            // reuse existing views if present
            int contextCount = contexts.size();
            while (viewCount < contextCount) {
                LabelView contextView = new LabelView(getActivity());
                contextView.setDuplicateParentStateEnabled(true);
                mContextContainer.addView(contextView);
                viewCount++;
            }
            if (viewCount > contextCount) {
                mContextContainer.removeViews(contextCount + 1, viewCount - contextCount);
            }

            for (int i = 0; i < contextCount; i++) {
                LabelView contextView = (LabelView) mContextContainer.getChildAt(i + 1); // skip no contexts view
                Context context = contexts.get(i);
                contextView.setText(context.getName());
                contextView.setColourIndex(context.getColourIndex());
                ContextIcon contextIcon = ContextIcon.createIcon(context.getIconName(), getResources(), true);
                Drawable icon = contextIcon == null ? null : getResources().getDrawable(contextIcon.smallIconId);
                contextView.setIcon(icon);
            }
        }
    }   
    
    private void setupProjectSpinner() {
        Cursor projectCursor = getActivity().getContentResolver().query(
                ProjectProvider.Projects.CONTENT_URI, PROJECT_PROJECTION,
                ProjectProvider.Projects.DELETED + " = 0", null, ProjectProvider.Projects.NAME + " ASC");
        int arraySize = projectCursor.getCount() + 1;
        mProjectIds = new long[arraySize];
        mProjectIds[0] = 0;
        mProjectNames = new String[arraySize];
        mProjectNames[0] = getText(R.string.none_empty).toString();
        for (int i = 1; i < arraySize; i++) {
            projectCursor.moveToNext();
            mProjectIds[i] = projectCursor.getLong(0);
            mProjectNames[i] = projectCursor.getString(1);
        }
        projectCursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, mProjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProjectSpinner.setAdapter(adapter);
    }

    private Id getSpinnerSelectedId(Spinner spinner, long[] ids) {
        Id id = Id.NONE;
        int selectedItemPosition = spinner.getSelectedItemPosition();
        if (selectedItemPosition > 0) {
            id = Id.create(ids[selectedItemPosition]);
        }
        return id;
    }

    private void setSpinnerSelection(Spinner spinner, long[] ids, long id) {
        if (id == 0L) {
            spinner.setSelection(0);
        } else {
            for (int i = 1; i < ids.length; i++) {
                if (ids[i] == id) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private String getProjectName(Id projectId) {
        String name = "";
        final long id = projectId.getId();
        for(int i = 0; i < mProjectIds.length; i++) {
            long currentId = mProjectIds[i];
            if (currentId == id) {
                name = mProjectNames[i];
                break;
            }
        }
        return name;
    }

    private void toggleSchedulingSection() {
        mSchedulingExpanded = !mSchedulingExpanded;
        setSchedulingVisibility(mSchedulingExpanded);
    }

    private void setSchedulingVisibility(boolean visible) {
        if (visible) {
            mSchedulingExtra.setVisibility(View.VISIBLE);
            mExpandButton.setVisibility(View.GONE);
            mCollapseButton.setVisibility(View.VISIBLE);
            mSchedulingDetail.setText(R.string.scheduling_expanded);
        } else {
            mSchedulingExtra.setVisibility(View.GONE);
            mExpandButton.setVisibility(View.VISIBLE);
            mCollapseButton.setVisibility(View.GONE);
            mSchedulingDetail.setText(R.string.scheduling_collapsed);
        }
    }

    private void populateWhen() {
        mShowFromDateVisible = !Time.isEpoch(mShowFromTime);
        mDueDateVisible = !Time.isEpoch(mDueTime);

        long showFromMillis = mShowFromTime.toMillis(false /* use isDst */);
        long dueMillis = mDueTime.toMillis(false /* use isDst */);
        setDate(mShowFromDateButton, showFromMillis, mShowFromDateVisible);
        setDate(mDueDateButton, dueMillis, mDueDateVisible);

        setTime(mShowFromTimeButton, showFromMillis, mShowFromDateVisible);
        setTime(mDueTimeButton, dueMillis, mDueDateVisible);
    }

    private void setDate(TextView view, long millis, boolean showValue) {
        CharSequence value;
        if (showValue) {
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH |
                    DateUtils.FORMAT_ABBREV_WEEKDAY;
            value = DateUtils.formatDateTime(getActivity(), millis, flags);
        } else {
            value = "";
        }
        view.setText(value);
    }

    private void setTime(TextView view, long millis, boolean showValue) {
        CharSequence value;
        if (showValue) {
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(getActivity())) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            value = DateUtils.formatDateTime(getActivity(), millis, flags);
        } else {
            value = "";
        }
        view.setText(value);
    }


    private void updateCalendarPanel() {
        boolean enabled = true;
        if (mOriginalItem != null &&
                mOriginalItem.getCalendarEventId().isInitialised()) {
            mCalendarLabel.setText(getString(R.string.update_gcal_title));
            mCalendarDetail.setText(getString(R.string.update_gcal_detail));
        } else if (mDueDateVisible || mShowFromDateVisible) {
            mCalendarLabel.setText(getString(R.string.add_to_gcal_title));
            mCalendarDetail.setText(getString(R.string.add_to_gcal_detail));
        } else {
            mCalendarLabel.setText(getString(R.string.add_to_gcal_title));
            mCalendarDetail.setText(getString(R.string.add_to_gcal_detail_disabled));
            enabled = false;
        }
        mUpdateCalendarEntry.setEnabled(enabled);
        mUpdateCalendarCheckBox.setEnabled(enabled);
    }

    private void updateToDefault(Time displayTime) {
        displayTime.setToNow();
        displayTime.second = 0;
        int minute = displayTime.minute;
        if (minute > 0 && minute <= 30) {
            displayTime.minute = 30;
        } else {
            displayTime.minute = 0;
            displayTime.hour += 1;
        }
    }
    
    
    /* This class is used to update the time buttons. */
    private class TimeListener implements TimePickerDialog.OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            mView = view;
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Cache the member variables locally to avoid inner class overhead.
            Time showFromTime = mShowFromTime;
            Time dueTime = mDueTime;

            // Cache the start and due millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long showFromMillis;
            long dueMillis;
            if (mView == mShowFromTimeButton) {
                // The show from time was changed.

                if (Time.isEpoch(showFromTime)) {
                    // time wasn't set - set to default to pick up default date values
                    updateToDefault(showFromTime);
                }

                int hourDuration = dueTime.hour - showFromTime.hour;
                int minuteDuration = dueTime.minute - showFromTime.minute;

                showFromTime.hour = hourOfDay;
                showFromTime.minute = minute;
                showFromMillis = showFromTime.normalize(true);
                mShowFromDateVisible = true;

                if (mDueDateVisible) {
                    // Also update the due time to keep the duration constant.
                    dueTime.hour = hourOfDay + hourDuration;
                    dueTime.minute = minute + minuteDuration;
                }
                dueMillis = dueTime.normalize(true);
            } else {
                // The due time was changed.

                if (Time.isEpoch(dueTime)) {
                    // time wasn't set - set to default to pick up default date values
                    updateToDefault(dueTime);
                }

                showFromMillis = showFromTime.toMillis(true);
                dueTime.hour = hourOfDay;
                dueTime.minute = minute;
                dueMillis = dueTime.normalize(true);
                mDueDateVisible = true;

                if (mShowFromDateVisible) {
                    // Do not allow an event to have a due time before the show from time.
                    if (dueTime.before(showFromTime)) {
                        // set show from to a day before the due date
                        showFromMillis = dueMillis - DateUtils.DAY_IN_MILLIS;
                        showFromTime.set(showFromMillis);
                    }
                }
            }

            // update all 4 buttons in case visibility has changed
            setDate(mShowFromDateButton, showFromMillis, mShowFromDateVisible);
            setTime(mShowFromTimeButton, showFromMillis, mShowFromDateVisible);
            setDate(mDueDateButton, dueMillis, mDueDateVisible);
            setTime(mDueTimeButton, dueMillis, mDueDateVisible);
            updateCalendarPanel();
        }

    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        public TimeClickListener(Time time) {
            mTime = time;
        }

        public void onClick(View v) {
            Time displayTime = mTime;

            if (Time.isEpoch(displayTime)) {
                // date isn't set - default to closest half hour
                displayTime = new Time();
                updateToDefault(displayTime);
            }

            new TimePickerDialog(getActivity(), new TimeListener(v),
                    displayTime.hour, displayTime.minute,
                    DateFormat.is24HourFormat(getActivity())).show();
        }
    }

    private class DateListener implements DatePickerDialog.OnDateSetListener {
        View mView;

        public DateListener(View view) {
            mView = view;
        }

        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            // Cache the member variables locally to avoid inner class overhead.
            Time showFromTime = mShowFromTime;
            Time dueTime = mDueTime;

            // Cache the show from and due millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long showFromMillis;
            long dueMillis;
            if (mView == mShowFromDateButton) {
                // The show from date was changed.

                if (Time.isEpoch(showFromTime)) {
                    // time wasn't set - set to default to pick up default time values
                    updateToDefault(showFromTime);
                }

                int yearDuration = dueTime.year - showFromTime.year;
                int monthDuration = dueTime.month - showFromTime.month;
                int monthDayDuration = dueTime.monthDay - showFromTime.monthDay;

                showFromTime.year = year;
                showFromTime.month = month;
                showFromTime.monthDay = monthDay;
                showFromMillis = showFromTime.normalize(true);
                mShowFromDateVisible = true;

                if (mDueDateVisible) {
                    // Also update the end date to keep the duration constant.
                    dueTime.year = year + yearDuration;
                    dueTime.month = month + monthDuration;
                    dueTime.monthDay = monthDay + monthDayDuration;
                }
                dueMillis = dueTime.normalize(true);
            } else {
                // The due date was changed.

                if (Time.isEpoch(dueTime)) {
                    // time wasn't set - set to default to pick up default time values
                    updateToDefault(dueTime);
                }

                showFromMillis = showFromTime.toMillis(true);
                dueTime.year = year;
                dueTime.month = month;
                dueTime.monthDay = monthDay;
                dueMillis = dueTime.normalize(true);
                mDueDateVisible = true;

                if (mShowFromDateVisible) {
                    // Do not allow an event to have a due time before the show from time.
                    if (dueTime.before(showFromTime)) {
                        // set show from to a day before the due date
                        showFromMillis = dueMillis - DateUtils.DAY_IN_MILLIS;
                        showFromTime.set(showFromMillis);
                    }
                }
            }

            // update all 4 buttons in case visibility has changed
            setDate(mShowFromDateButton, showFromMillis, mShowFromDateVisible);
            setTime(mShowFromTimeButton, showFromMillis, mShowFromDateVisible);
            setDate(mDueDateButton, dueMillis, mDueDateVisible);
            setTime(mDueTimeButton, dueMillis, mDueDateVisible);
            updateCalendarPanel();
        }

    }

    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        public DateClickListener(Time time) {
            mTime = time;
        }

        public void onClick(View v) {
            Time displayTime = mTime;
            if (Time.isEpoch(displayTime)) {
                displayTime = new Time();
                updateToDefault(displayTime);
            }
            new DatePickerDialog(getActivity(), new DateListener(v), displayTime.year,
                    displayTime.month, displayTime.monthDay).show();
        }
    }
    

}
