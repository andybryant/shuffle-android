/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dodgybits.shuffle.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.listener.LocationParser;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;

import java.util.List;

/**
 * The task widget.
 * <p><em>NOTE</em>: All methods must be called on the UI thread so synchronization is NOT required
 * in this class)
 */
public class TaskWidget implements RemoteViewsService.RemoteViewsFactory,
        OnLoadCompleteListener<Cursor> {
    public static final String TAG = "TaskWidget";

    /**
     * When handling clicks in a widget ListView, a single PendingIntent template is provided to
     * RemoteViews, and the individual "on click" actions are distinguished via a "fillInIntent"
     * on each list element; when a click is received, this "fillInIntent" is merged with the
     * PendingIntent using Intent.fillIn().  Since this mechanism does NOT preserve the Extras
     * Bundle, we instead encode information about the action (e.g. view, reply, etc.) and its
     * arguments (e.g. messageId, mailboxId, etc.) in an Uri which is added to the Intent via
     * Intent.setDataAndType()
     *
     * The mime type MUST be set in the Intent, even though we do not use it; therefore, it's value
     * is entirely arbitrary.
     *
     * Our "command" Uri is NOT used by the system in any manner, and is therefore constrained only
     * in the requirement that it be syntactically valid.
     *
     * We use the following convention for our commands:
     *     widget://command/<command>/<arg1>[/<arg2>]
     */
    private static final String WIDGET_DATA_MIME_TYPE = "org.dodgybits.shuffle/widget_data";

    private static final Uri COMMAND_URI = Uri.parse("widget://command");

    // Command names and Uri's built upon COMMAND_URI
    private static final String COMMAND_NAME_VIEW_TASK = "view_task";
    private static final Uri COMMAND_URI_VIEW_TASK =
            COMMAND_URI.buildUpon().appendPath(COMMAND_NAME_VIEW_TASK).build();

    private static final int MAX_MESSAGE_LIST_COUNT = 25;

    private static String sContentsSnippetDivider;
    private static int sProjectFontSize;
    private static int sContentsFontSize;
    private static int sDateFontSize;
    private static int sDefaultTextColor;
    private static int sLightTextColor;

    private final Context mContext;
    private final AppWidgetManager mWidgetManager;

    // The widget identifier
    private int mWidgetId;

    // The widget's loader (derived from ThrottlingCursorLoader)
    private final TaskWidgetLoader mLoader;

    /** The display name of this task list */
    private String mTaskListName;

    private TaskListContext mListContext;

    /**
     * The cursor for the messages, with some extra info such as the number of accounts.
     *
     * Note this cursor can be closed any time by the loader.  Always use {@link #isCursorValid()}
     * before touching its contents.
     */
    private TaskWidgetLoader.WidgetCursor mCursor;

    @Inject
    EntityCache<org.dodgybits.shuffle.android.core.model.Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    TaskPersister mTaskPersister;

    @Inject
    LocationParser mLocationParser;

    ContextBitmapProvider mBitmapProvider;
    
    @Inject
    public TaskWidget(Context context) {
        super();
        mContext = context.getApplicationContext();
        mWidgetManager = AppWidgetManager.getInstance(mContext);
        mBitmapProvider = new ContextBitmapProvider(mContext);

        mLoader = new TaskWidgetLoader(mContext);
        mLoader.registerListener(0, this);
        if (sContentsSnippetDivider == null) {
            // Initialize string, color, dimension resources
            Resources res = mContext.getResources();
            sContentsSnippetDivider = " - ";
            sProjectFontSize = res.getDimensionPixelSize(R.dimen.widget_project_font_size);
            sContentsFontSize = res.getDimensionPixelSize(R.dimen.widget_contents_font_size);
            sDateFontSize = res.getDimensionPixelSize(R.dimen.widget_date_font_size);
            sDefaultTextColor = res.getColor(R.color.widget_default_text_color);
            sLightTextColor = res.getColor(R.color.widget_light_text_color);
        }
    }

    public void setWidgetId(int widgetId) {
        mWidgetId = widgetId;
    }

    /**
     * Start loading the data.  At this point nothing on the widget changes -- the current view
     * will remain valid until the loader loads the latest data.
     */
    public void start() {
        mListContext = WidgetManager.loadListContextPref(mContext, mWidgetId);
        if (mListContext != null) {
            mLoader.load(mListContext);
        }
    }

    /**
     * Resets the data in the widget and forces a reload.
     */
    public void reset() {
        mLoader.reset();
        start();
    }

    private boolean isCursorValid() {
        return mCursor != null && !mCursor.isClosed();
    }

    /**
     * Called when the loader finished loading data.  Update the widget.
     */
    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
        mCursor = (TaskWidgetLoader.WidgetCursor) cursor;   // Save away the cursor
        mTaskListName = mListContext.createTitle(mContext, mContextCache, mProjectCache);
        updateHeader();
        mWidgetManager.notifyAppWidgetViewDataChanged(mWidgetId, R.id.task_list);
    }

    /**
     * Convenience method for creating an onClickPendingIntent that launches another activity
     * directly.
     *
     * @param views The RemoteViews we're inflating
     * @param buttonId the id of the button view
     * @param intent The intent to be used when launching the activity
     */
    private void setActivityIntent(RemoteViews views, int buttonId, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // just in case intent comes without it
        PendingIntent pendingIntent =
                PendingIntent.getActivity(mContext, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(buttonId, pendingIntent);
    }

    /**
     * Convenience method for constructing a fillInIntent for a given list view element.
     * Appends the command and any arguments to a base Uri.
     *
     * @param views the RemoteViews we are inflating
     * @param viewId the id of the view
     * @param baseUri the base uri for the command
     * @param args any arguments to the command
     */
    private void setFillInIntent(RemoteViews views, int viewId, Uri baseUri, String ... args) {
        Intent intent = new Intent();
        Uri.Builder builder = baseUri.buildUpon();
        for (String arg: args) {
            builder.appendPath(arg);
        }
        intent.setDataAndType(builder.build(), WIDGET_DATA_MIME_TYPE);
        views.setOnClickFillInIntent(viewId, intent);
    }

    /**
     * Called back by {@link WidgetProvider.WidgetService} to
     * handle intents created by remote views.
     */
    public static boolean processIntent(Context context, Intent intent) {
        final Uri data = intent.getData();
        if (data == null) {
            return false;
        }

        List<String> pathSegments = data.getPathSegments();
        // Our path segments are <command>, <queryName>. <contextId>, <projectId>, <position>
        // First, a quick check of Uri validity
        if (pathSegments.size() < 5) {
            throw new IllegalArgumentException();
        }
        String command = pathSegments.get(0);
        // Ignore unknown action names
        try {
            final String queryName = pathSegments.get(1);
            final ListQuery listQuery = ListQuery.valueOf(queryName);
            final Id contextId = Id.create(Long.parseLong(pathSegments.get(2)));
            final Id projectId = Id.create(Long.parseLong(pathSegments.get(3)));
            TaskListContext listContext = TaskListContext.create(listQuery, contextId, projectId);
            final int position = Integer.parseInt(pathSegments.get(4));
            if (COMMAND_NAME_VIEW_TASK.equals(command)) {
                openTask(context, listContext, position);
            }
        } catch (NumberFormatException e) {
            // Shouldn't happen as we construct all of the Uri's
            return false;
        }
        return true;
    }

    private static void openTask(final Context context, final TaskListContext listContext,
                                 final int position) {
        Location location = listContext.toLocation().
                builderFrom().setSelectedIndex(position).build();
        Intent intent = LocationParser.createIntent(context, location);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // just in case intent comes without it
        context.startActivity(intent);
    }

    private void setTextViewTextAndDesc(RemoteViews views, final int id, String text) {
        views.setTextViewText(id, text);
    }

    private void setupTitleAndCount(RemoteViews views) {
        // Set up the title (view type + count of messages)
        setTextViewTextAndDesc(views, R.id.widget_title, mTaskListName);
        String count = "";
        if (isCursorValid()) {
            count = UiUtilities.getCountForUi(mContext, mCursor.getTaskCount(), false);
        }
        setTextViewTextAndDesc(views, R.id.widget_count, count);
    }

    /**
     * Update the "header" of the widget (i.e. everything that doesn't include the scrolling
     * task list)
     */
    private void updateHeader() {
        Log.d(TAG, "#updateHeader(); widgetId: " + mWidgetId);

        // Get the widget layout
        RemoteViews views =
                new RemoteViews(mContext.getPackageName(), R.layout.widget);

        // Set up the list with an adapter
        Intent intent = new Intent(mContext, WidgetProvider.WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.task_list, intent);

        setupTitleAndCount(views);

        if (isCursorValid()) {
            // Show compose icon & task list
            views.setViewVisibility(R.id.widget_compose, View.VISIBLE);
            views.setViewVisibility(R.id.task_list, View.VISIBLE);
            // Create click intent for "create task" target
            Location location = Location.newTaskWithContext()

                    
            intent = IntentUtils.createNewTaskIntent(mContext, mListContext);
            setActivityIntent(views, R.id.widget_compose, intent);
            // Create click intent for logo to open inbox
            intent = IntentUtils.createTaskListIntent(mContext, mListContext);
            setActivityIntent(views, R.id.widget_logo, intent);
            setActivityIntent(views, R.id.widget_title, intent);
        }

        // Use a bare intent for our template; we need to fill everything in
        intent = new Intent(mContext, WidgetProvider.WidgetService.class);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.task_list, pendingIntent);

        // And finally update the widget
        mWidgetManager.updateAppWidget(mWidgetId, views);
    }

    /**
     * Add size and color styling to text
     *
     * @param text the text to style
     * @param size the font size for this text
     * @param color the color for this text
     * @return a CharSequence quitable for use in RemoteViews.setTextViewText()
     */
    private CharSequence addStyle(CharSequence text, int size, int color) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        if (text.length() > 0) {
            builder.setSpan(
                    new AbsoluteSizeSpan(size), 0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (color != 0) {
                builder.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }

    /**
     * Create styled text for our combination subject and snippet
     *
     * @param subject the message's subject (or null)
     * @param snippet the message's snippet (or null)
     * @param read whether or not the message is read
     * @return a CharSequence suitable for use in RemoteViews.setTextViewText()
     */
    private CharSequence getStyledContents(String subject, String snippet, boolean read) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        boolean hasSubject = false;
        if (!TextUtils.isEmpty(subject)) {
            SpannableString ss = new SpannableString(subject);
            ss.setSpan(new StyleSpan(read ? Typeface.NORMAL : Typeface.BOLD), 0, ss.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(sDefaultTextColor), 0, ss.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(ss);
            hasSubject = true;
        }
        if (!TextUtils.isEmpty(snippet)) {
            if (hasSubject) {
                ssb.append(sContentsSnippetDivider);
            }
            SpannableString ss = new SpannableString(snippet);
            ss.setSpan(new ForegroundColorSpan(sLightTextColor), 0, snippet.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(ss);
        }
        return addStyle(ssb, sContentsFontSize, 0);
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // Use the cursor to set up the widget
        if (!isCursorValid() || !mCursor.moveToPosition(position)) {
            return getLoadingView();
        }
        
        Task task = mTaskPersister.read(mCursor);
        
        RemoteViews views =
            new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        boolean isIncomplete = !(task.isComplete() || task.isDeleted());
        int drawableId = R.drawable.task_complete_selector;
        if (isIncomplete) {
            drawableId = R.drawable.task_incomplete_selector;
        }
        views.setInt(R.id.widget_task, "setBackgroundResource", drawableId);

        // Add style to project
        Project project = mProjectCache.findById(task.getProjectId());
        String projectName = project == null ? "" : project.getName();
        SpannableStringBuilder projectBuilder = new SpannableStringBuilder(projectName);
        if (projectBuilder.length() > 0) {
            projectBuilder.setSpan(
                    isIncomplete ? new StyleSpan(Typeface.BOLD) : new StyleSpan(Typeface.NORMAL), 0,
                    projectBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        CharSequence styledProject = addStyle(projectBuilder, sProjectFontSize, sDefaultTextColor);
        views.setTextViewText(R.id.widget_project, styledProject);

        long timestamp = task.getDueDate();
        // Get a nicely formatted date string (relative to today)
        String date = timestamp == 0L ? "" : DateUtils.getRelativeTimeSpanString(mContext, timestamp).toString();

        // Add style to date
        CharSequence styledDate = addStyle(date, sDateFontSize, sDefaultTextColor);
        views.setTextViewText(R.id.widget_date, styledDate);

        // Add style to contents
        CharSequence contents = getStyledContents(task.getDescription(), task.getDetails(), !isIncomplete);
        views.setTextViewText(R.id.widget_contents, contents);

        // Add badge icons
        views.setViewVisibility(R.id.widget_deleted, task.isDeleted() ? View.VISIBLE : View.GONE);

        // Add context graphic
        List<org.dodgybits.shuffle.android.core.model.Context> contexts = mContextCache.findById(task.getContextIds());
        Bitmap bitmap = mBitmapProvider.getBitmapForContexts(contexts);
        views.setImageViewBitmap(R.id.widget_context, bitmap);

        TaskSelector selector = mListContext.createSelectorWithPreferences(mContext);
        String queryName = selector.getListQuery().name();
        String contextId = String.valueOf(selector.getContextId().getId());
        String projectId = String.valueOf(selector.getProjectId().getId());
        
        setFillInIntent(views, R.id.widget_task, COMMAND_URI_VIEW_TASK,
                queryName, contextId, projectId, String.valueOf(position));

        return views;
    }

    @Override
    public int getCount() {
        if (!isCursorValid()) return 0;
        return Math.min(mCursor.getCount(), MAX_MESSAGE_LIST_COUNT);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.widget_loading);
        view.setTextViewText(R.id.loading_text, mContext.getString(R.string.widget_loading));
        return view;
    }

    @Override
    public int getViewTypeCount() {
        // Regular list view and the "loading" view
        return 2;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
    }

    public void onDeleted() {
        Log.d(TAG, "#onDeleted(); widgetId: " + mWidgetId);

        if (mLoader != null) {
            mLoader.reset();
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "#onCreate(); widgetId: " + mWidgetId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "#onDestroy(); widgetId: " + mWidgetId);

        if (mLoader != null) {
            mLoader.reset();
        }
    }

    @Override
    public String toString() {
        return "View=" + mListContext.toString();
    }
}
