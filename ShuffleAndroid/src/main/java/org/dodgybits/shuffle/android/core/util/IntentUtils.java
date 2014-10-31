package org.dodgybits.shuffle.android.core.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.controller.AbstractActivityController;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.view.activity.TaskPagerActivity;

public class IntentUtils {
    private static final String TAG = "IntentUtils";
    
    public static Intent createNewTaskIntent(Context context, TaskListContext listContext) {
        TaskSelector selector = listContext.createSelectorWithPreferences(context);
        return createNewTaskIntent(null, selector.getContextId(), selector.getProjectId());
    }

    public static Intent createNewTaskIntent(String description, Id contextId, Id projectId) {
        Intent intent = new Intent(Intent.ACTION_INSERT, TaskProvider.Tasks.CONTENT_URI);
        if (contextId.isInitialised()) {
            intent.putExtra(TaskProvider.TaskContexts.CONTEXT_ID, contextId.getId());
        }
        if (projectId.isInitialised()) {
            intent.putExtra(TaskProvider.Tasks.PROJECT_ID, projectId.getId());
        }
        if (description != null) {
            intent.putExtra(TaskProvider.Tasks.DESCRIPTION, description);
        }
        return intent;
    }
    
    public static Intent createTaskListIntent(Context context, TaskListContext listContext) {
        TaskSelector selector;
        Id id;
        switch (listContext.getListQuery()) {
            case project:
                selector = listContext.createSelectorWithPreferences(context);
                id = selector.getProjectId();
                return createProjectViewIntent(id);
            case context:
                selector = listContext.createSelectorWithPreferences(context);
                id = selector.getContextId();
                return createContextViewIntent(id);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainView.QUERY_NAME, listContext.getListQuery().name());

        Uri.Builder builder = TaskProvider.Tasks.LIST_CONTENT_URI.buildUpon();
        builder.appendPath(listContext.getListQuery().name());
        Uri uri = builder.build();
        intent.setData(uri);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }
    
    public static Intent createContextViewIntent(Id contextId) {
        Uri url = ContentUris.withAppendedId(ContextProvider.Contexts.CONTENT_URI, contextId.getId());
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        return intent;
    }

    public static Intent createProjectViewIntent(Id projectId) {
        Uri url = ContentUris.withAppendedId(ProjectProvider.Projects.CONTENT_URI, projectId.getId());
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        return intent;
    }

    public static Intent createTaskViewIntent(Context context, TaskListContext listContext, int position) {
        Intent intent = new Intent(context, TaskPagerActivity.class);
        intent.putExtra(TaskPagerActivity.INITIAL_POSITION, position);
        intent.putExtra(TaskPagerActivity.TASK_LIST_CONTEXT, listContext);
        return intent;
    }

}
