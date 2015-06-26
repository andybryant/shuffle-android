package org.dodgybits.shuffle.android.editor.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
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
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.editor.activity.DateTimePickerActivity;
import org.dodgybits.shuffle.android.editor.activity.EditTaskActivity;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
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

    private static final int NEW_CONTEXT_CODE = 100;
    private static final int NEW_PROJECT_CODE = 101;
    private static final int DEFERRED_CODE = 102;
    private static final int DUE_CODE = 103;

    private EditText mDescriptionWidget;
    private EditText mDetailsWidget;

    private ViewGroup mContextContainer;
    private List<Id> mSelectedContextIds = Lists.newArrayList();
    private TextView mNoContexts;
    private Button mAddContextButton;

    private Id mSelectedProjectId = Id.NONE;
    private Button mEditProjectButton;

    private Button mDeferredEditButton;
    private Button mDueEditButton;

    private Time mDeferredTime;
    private Time mDueTime;

    private View mCompleteEntry;
    private TextView mCompleteLabel;
    private CompoundButton mCompletedCheckBox;
    private ImageView mCompletedIcon;

    private Button mDeleteButton;

    private View mUpdateCalendarEntry;
    private CompoundButton mUpdateCalendarCheckBox;
    private TextView mCalendarLabel;
    private TextView mCalendarDetail;

    @Inject
    private EntityCache<Project> mProjectCache;

    @Inject
    private EntityCache<Context> mContextCache;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mDeferredTime = new Time();
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
                        setSelectedProject(Id.create(ContentUris.parseId(data.getData())));
                    }
                }
                break;

            case DEFERRED_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mDeferredTime.set(data.getLongExtra(DateTimePickerActivity.DATETIME_VALUE, 0L));
                        if (!Time.isEpoch(mDueTime) && !Time.isEpoch(mDeferredTime) &&
                                mDeferredTime.after(mDueTime)) {
                            mDueTime.set(mDeferredTime);
                        }
                        onDatesUpdated();
                    }
                }
                break;

            case DUE_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mDueTime.set(data.getLongExtra(DateTimePickerActivity.DATETIME_VALUE, 0L));
                        if (!Time.isEpoch(mDueTime) && !Time.isEpoch(mDeferredTime) &&
                                mDeferredTime.after(mDueTime)) {
                            mDeferredTime.set(mDueTime);
                        }
                        onDatesUpdated();
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

    public void setSelectedProject(Id projectId) {
        mSelectedProjectId = projectId;
        updateProjectButton();
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
            setSelectedProject(Id.create(projectId));
            applyDefaultContext();
        }

        mCompleteEntry.setVisibility(View.GONE);
        mDeleteButton.setVisibility(View.GONE);

        onDatesUpdated();
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

        setSelectedProject(task.getProjectId());

        boolean allDay = task.isAllDay();
        if (allDay && task.getStartDate() != 0L) {
            String tz = mDeferredTime.timezone;
            mDeferredTime.timezone = Time.TIMEZONE_UTC;
            mDeferredTime.set(task.getStartDate());
            mDeferredTime.timezone = tz;

            // Calling normalize to calculate isDst
            mDeferredTime.normalize(true);
        } else {
            mDeferredTime.set(task.getStartDate());
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
        onDatesUpdated();

        mCompletedCheckBox.setChecked(task.isComplete());
        updateCompletedState();
        mDeleteButton.setText(task.isDeleted() ? R.string.restore_button_title : R.string.delete_completed_button_title);
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
        final boolean complete = mCompletedCheckBox.isChecked();
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
        if (!ObjectUtils.equals(mSelectedProjectId, builder.getProjectId())) {
            builder.setProjectId(mSelectedProjectId);
            changeSet.projectChanged();
            changed = true;
        }
        if (complete != builder.isComplete()) {
            builder.setComplete(complete);
            changeSet.completeChanged();
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
        long deferredMillis = 0L;
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

        if (!Time.isEpoch(mDeferredTime)) {
            mDeferredTime.timezone = timezone;
            deferredMillis = mDeferredTime.toMillis(true);
        }

        if (!Time.isEpoch(mDueTime)) {
            mDueTime.timezone = timezone;
            dueMillis = mDueTime.toMillis(true);
        }

        final int order;
        if (commitValues) {
            order = ((TaskPersister)mPersister).calculateTaskOrder(mOriginalItem, mSelectedProjectId);
        } else if (mOriginalItem == null) {
            order = 0;
        } else {
            order = mOriginalItem.getOrder();
        }

        builder.setTimezone(timezone);

        if (deferredMillis != builder.getStartDate()) {
            builder.setStartDate(deferredMillis);
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
            long startMillis = deferredMillis != 0L ? deferredMillis : dueMillis - DateUtils.HOUR_IN_MILLIS;
            long endMillis = dueMillis != 0L ? dueMillis : deferredMillis + DateUtils.HOUR_IN_MILLIS;

            Uri calEntryUri = addOrUpdateCalendarEvent(
                    eventId, description, details,
                    mSelectedProjectId, timezone, startMillis,
                    endMillis, mOriginalItem != null && mOriginalItem.isAllDay());
            if (calEntryUri != null) {
                eventId = Id.create(ContentUris.parseId(calEntryUri));
                mNextIntent = new Intent(Intent.ACTION_VIEW , calEntryUri);
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
        Id projectId = mSelectedProjectId;
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO clear buttons clicked
        Log.d(TAG, "Button " + buttonView + " checked: " + isChecked);
    }

    public void triggerAddProject() {
        Intent addProjectIntent = new Intent(Intent.ACTION_INSERT,
                ProjectProvider.Projects.CONTENT_URI);
        startActivityForResult(addProjectIntent, NEW_PROJECT_CODE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.context_add: {
                Intent addContextIntent = new Intent(Intent.ACTION_INSERT,
                        ContextProvider.Contexts.CONTENT_URI);
                startActivityForResult(addContextIntent, NEW_CONTEXT_CODE);
                break;
            }

            case R.id.context_items_container: {
                showContextPicker();
                break;
            }

            case R.id.project: {
                showProjectPicker();
                break;
            }

            case R.id.completed_row: {
                mCompletedCheckBox.toggle();
                updateCompletedState();
                break;
            }

            case R.id.completed_entry_checkbox: {
                super.onClick(v);
                updateCompletedState();
                break;
            }

            case R.id.delete_button: {
                mEventManager.fire(new UpdateTasksDeletedEvent(
                        mOriginalItem.getLocalId().getId(), !mOriginalItem.isDeleted()));
                getActivity().finish();
                break;
            }

            case R.id.gcal_entry: {
                mUpdateCalendarCheckBox.toggle();
                break;
            }

            case R.id.defer: {
                showDeferredPicker();
                break;
            }

            case R.id.due: {
                showDuePicker();
                break;
            }

            default:
                super.onClick(v);
                break;
        }
    }

    private void showContextPicker() {
        ((EditTaskActivity)getActivity()).showContextPicker();
    }

    private void showProjectPicker() {
        ((EditTaskActivity)getActivity()).showProjectPicker();
    }

    private void showDeferredPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(DateTimePickerActivity.TYPE);
        long deferredMillis = mDeferredTime.toMillis(false /* use isDst */);
        intent.putExtra(DateTimePickerActivity.DATETIME_VALUE, deferredMillis);
        intent.putExtra(DateTimePickerActivity.TITLE, getString(R.string.title_deferred_picker));
        startActivityForResult(intent, DEFERRED_CODE);
    }

    private void showDuePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(DateTimePickerActivity.TYPE);
        long dueMillis = mDueTime.toMillis(false /* use isDst */);
        intent.putExtra(DateTimePickerActivity.DATETIME_VALUE, dueMillis);
        intent.putExtra(DateTimePickerActivity.TITLE, getString(R.string.title_due_picker));
        startActivityForResult(intent, DUE_CODE);
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
        mEditProjectButton = (Button) getView().findViewById(R.id.project);
        mEditProjectButton.setOnClickListener(this);
        mDescriptionWidget = (EditText) getView().findViewById(R.id.description);
        mDetailsWidget = (EditText) getView().findViewById(R.id.details);


        mAddContextButton = (Button) getView().findViewById(R.id.context_add);
        mAddContextButton.setOnClickListener(this);
        mAddContextButton.setOnFocusChangeListener(this);
        mContextContainer = (ViewGroup) getView().findViewById(R.id.context_items_container);
        mContextContainer.setOnClickListener(this);
        mNoContexts = (TextView) getView().findViewById(R.id.no_contexts);

        updateProjectButton();

        mCompleteEntry = getView().findViewById(R.id.completed_row);
        mCompleteEntry.setOnClickListener(this);
        mCompleteEntry.setOnFocusChangeListener(this);
        mCompleteLabel = (TextView) mCompleteEntry.findViewById(R.id.completed_label);
        mCompletedCheckBox = (SwitchCompat) mCompleteEntry.findViewById(R.id.completed_entry_checkbox);
        mCompletedCheckBox.setOnClickListener(this);
        mCompletedIcon = (ImageView) mCompleteEntry.findViewById(R.id.completed_icon);

        mDeferredEditButton = (Button) getView().findViewById(R.id.defer);
        mDeferredEditButton.setOnClickListener(this);
        mDueEditButton = (Button) getView().findViewById(R.id.due);
        mDueEditButton.setOnClickListener(this);

        mUpdateCalendarEntry = getView().findViewById(R.id.gcal_entry);
        mUpdateCalendarEntry.setOnClickListener(this);
        mUpdateCalendarEntry.setOnFocusChangeListener(this);
        mUpdateCalendarCheckBox = (CompoundButton) mUpdateCalendarEntry.findViewById(R.id.update_calendar_checkbox);
        mCalendarLabel = (TextView) mUpdateCalendarEntry.findViewById(R.id.gcal_label);
        mCalendarDetail = (TextView) mUpdateCalendarEntry.findViewById(R.id.gcal_detail);

        mDeleteButton = (Button) getView().findViewById(R.id.delete_button);
        mDeleteButton.setOnClickListener(this);
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
                contextView.setTag(FontUtils.ALL_CAPS);
                contextView.setText(context.getName());
                contextView.setColourIndex(context.getColourIndex());
                ContextIcon contextIcon = ContextIcon.createIcon(context.getIconName(), getResources(), true);
                Drawable icon = contextIcon == null ? null : getResources().getDrawable(contextIcon.smallIconId);
                contextView.setIcon(icon);
            }
        }

        FontUtils.setCustomFont(mContextContainer, getActivity().getAssets());
    }   
    
    private void updateProjectButton() {
        String name = getProjectName(mSelectedProjectId);
        if (name.isEmpty()) {
            mEditProjectButton.setTextColor(getResources().getColor(R.color.body_text_2));
            mEditProjectButton.setTag("regular");
            mEditProjectButton.setText(getString(R.string.title_project_picker));
        } else {
            mEditProjectButton.setTextColor(getResources().getColor(R.color.body_text_1));
            mEditProjectButton.setTag("bold");
            mEditProjectButton.setText(name);
        }
        FontUtils.setCustomFont(mEditProjectButton, getActivity().getAssets());
    }

    private String getProjectName(Id projectId) {
        String name = "";
        Project project = mProjectCache.findById(projectId);
        if (project != null) {
            name = project.getName();
        }
        return name;
    }

    private void onDatesUpdated() {
        updateDatesPanel();
        updateCalendarPanel();
    }

    private void updateDatesPanel() {
        boolean deferredSet = !Time.isEpoch(mDeferredTime);
        long deferredMillis = mDeferredTime.toMillis(false /* use isDst */);
        boolean deferredInPast = deferredMillis < System.currentTimeMillis();
        if (deferredSet && !deferredInPast) {
            mDeferredEditButton.setTextColor(getResources().getColor(R.color.deferred));
            mDeferredEditButton.setTag("bold");
            mDeferredEditButton.setText(getString(R.string.deferred_until_phrase ,
                    formatDateTime(deferredMillis)));
        } else {
            mDeferredEditButton.setTextColor(getResources().getColor(R.color.label_color));
            mDeferredEditButton.setTag("regular");
            mDeferredEditButton.setText(R.string.not_deferred);
        }
        FontUtils.setCustomFont(mDeferredEditButton, getActivity().getAssets());

        boolean dueSet = !Time.isEpoch(mDueTime);
        long dueMillis = mDueTime.toMillis(false /* use isDst */);
        boolean dueInPast = dueMillis < System.currentTimeMillis();
        if (dueSet) {
            mDueEditButton.setText(getString(R.string.due_phrase,
                    formatDateTime(dueMillis)));
        } else {
            mDueEditButton.setText(R.string.not_due);
        }
        mDueEditButton.setTextColor(getResources().getColor(
                dueSet && dueInPast ? R.color.theme_primary_dark : R.color.label_color));
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

    private void updateCalendarPanel() {
        boolean enabled = true;
        if (mOriginalItem != null &&
                mOriginalItem.getCalendarEventId().isInitialised()) {
            mCalendarLabel.setText(getString(R.string.update_gcal_title));
            mCalendarDetail.setText(getString(R.string.update_gcal_detail));
        } else if (!Time.isEpoch(mDueTime) || !Time.isEpoch(mDeferredTime)) {
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

    private void updateCompletedState() {
        mCompletedIcon.setImageResource(
                mCompletedCheckBox.isChecked() ? R.drawable.ic_menu_incomplete : R.drawable.ic_menu_complete_black);
        mCompleteLabel.setText(mCompletedCheckBox.isChecked() ? R.string.complete : R.string.incomplete);
    }

    

}
