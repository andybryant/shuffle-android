package org.dodgybits.shuffle.android.list.listener;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.activity.HelpActivity;
import org.dodgybits.shuffle.android.core.util.IntentUtils;
import org.dodgybits.shuffle.android.list.activity.ContextTaskListsActivity;
import org.dodgybits.shuffle.android.list.activity.ProjectTaskListsActivity;
import org.dodgybits.shuffle.android.list.activity.TaskSearchResultsActivity;
import org.dodgybits.shuffle.android.list.event.*;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.activity.PreferencesActivity;
import roboguice.event.Observes;

public class NavigationListener {

    private Activity mActivity;

    @Inject
    public NavigationListener(Activity activity) {
        mActivity = activity;
    }

    public void onViewPreferences(@Observes ViewPreferencesEvent event) {
        Intent intent = new Intent(mActivity, PreferencesActivity.class);
        mActivity.startActivity(intent);
    }

    public void onViewHelp(@Observes ViewHelpEvent event) {
        Intent intent = new Intent(mActivity, HelpActivity.class);
        if (event.getListQuery() != null) {
            intent.putExtra(HelpActivity.QUERY_NAME, event.getListQuery().name());
        }
        mActivity.startActivity(intent);
    }

    public void onNewTask(@Observes EditNewTaskEvent event) {
        Intent intent = IntentUtils.createNewTaskIntent(event.getDescription(), event.getContextId(), event.getProjectId());
        mActivity.startActivity(intent);
    }

    public void onNewProject(@Observes EditNewProjectEvent event) {
        Intent intent = new Intent(Intent.ACTION_INSERT, ProjectProvider.Projects.CONTENT_URI);
        mActivity.startActivity(intent);
    }

    public void onNewContext(@Observes EditNewContextEvent event) {
        Intent intent = new Intent(Intent.ACTION_INSERT, ContextProvider.Contexts.CONTENT_URI);
        mActivity.startActivity(intent);
    }

    public void onViewContext(@Observes ViewContextEvent event) {
        Intent intent = IntentUtils.createContextViewIntent(event.getContextId());
        intent.putExtra(ContextTaskListsActivity.INITIAL_POSITION, event.getPosition());
        mActivity.startActivity(intent);
    }

    public void onViewProject(@Observes ViewProjectEvent event) {
        Intent intent = IntentUtils.createProjectViewIntent(event.getProjectId());
        intent.putExtra(ProjectTaskListsActivity.INITIAL_POSITION, event.getPosition());
        mActivity.startActivity(intent);
    }

    public void onViewTaskSearchList(@Observes ViewTaskSearchResultsEvent event) {
        Intent intent = new Intent(mActivity, TaskSearchResultsActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.getExtras().putString(SearchManager.QUERY, event.getSearchQuery());
        mActivity.startActivity(intent);
    }

    public void onEditTask(@Observes EditTaskEvent event) {
        Uri uri = ContentUris.appendId(
                TaskProvider.Tasks.CONTENT_URI.buildUpon(), event.getTaskId().getId()).build();
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        mActivity.startActivity(intent);
    }

    public void onEditProject(@Observes EditProjectEvent event) {
        Uri uri = ContentUris.appendId(
                ProjectProvider.Projects.CONTENT_URI.buildUpon(), event.getProjectId().getId()).build();
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        mActivity.startActivity(intent);
    }

    public void onEditContext(@Observes EditContextEvent event) {
        Uri uri = ContentUris.appendId(
                ContextProvider.Contexts.CONTENT_URI.buildUpon(), event.getContextId().getId()).build();
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        mActivity.startActivity(intent);
    }

    public void onEditListSettings(@Observes EditListSettingsEvent event) {
        Intent intent = ListSettingsCache.createListSettingsEditorIntent(mActivity, event.getListQuery());
        event.getFragment().startActivityForResult(intent, event.getRequestCode());
    }

}
