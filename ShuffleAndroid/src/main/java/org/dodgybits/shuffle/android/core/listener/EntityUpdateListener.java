package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.list.event.*;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

import java.util.List;
import java.util.Set;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

@ContextSingleton
public class EntityUpdateListener {
    private static final String TAG = "EntityUpdateListener";

    private Activity mActivity;
    private ProjectPersister mProjectPersister;
    private ContextPersister mContextPersister;
    private TaskPersister mTaskPersister;
    
    @Inject
    public EntityUpdateListener(Activity activity, ProjectPersister projectPersister,
                                ContextPersister contextPersister, TaskPersister taskPersister) {
        mActivity = activity;
        mProjectPersister = projectPersister;
        mContextPersister = contextPersister;
        mTaskPersister = taskPersister;
    }

    private void onToggleProjectDeleted(@Observes UpdateProjectDeletedEvent event) {
        Id id = event.getProjectId();
        boolean isDeleted = event.isDeleted();
        if (event.isDeleted() == null) {
            // need to look up current value and toggle
            Project project = mProjectPersister.findById(id);
            isDeleted = !project.isDeleted();
        }
        
        mProjectPersister.updateDeletedFlag(event.getProjectId(), isDeleted);
        String entityName = mActivity.getString(R.string.project_name);
        showDeletedToast(entityName, isDeleted);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onToggleContextDeleted(@Observes UpdateContextDeletedEvent event) {
        Id id = event.getContextId();
        boolean isDeleted = event.isDeleted();
        if (event.isDeleted() == null) {
            // need to look up current value and toggle
            Context context = mContextPersister.findById(id);
            isDeleted = !context.isDeleted();
        }
        
        mContextPersister.updateDeletedFlag(event.getContextId(), event.isDeleted());
        String entityName = mActivity.getString(R.string.context_name);
        showDeletedToast(entityName, isDeleted);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onMoveTasks(@Observes MoveTasksEvent event) {
        mTaskPersister.moveTasksWithinProject(event.getTaskIds(), event.getCursor(), event.isMoveUp());
    }

    private void onUpdateTasksDeleted(@Observes UpdateTasksDeletedEvent event) {
        Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateDeletedFlag(id, event.isDeleted());
        }

        String entityName = mActivity.getString(R.string.task_name);
        showDeletedToast(entityName, event.isDeleted());
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onUpdateTaskCompleted(@Observes UpdateTasksCompletedEvent event) {
        Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateCompleteFlag(id, event.isCompleted());
        }

        String entityName = mActivity.getString(R.string.task_name);
        showSavedToast(entityName);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onNewTask(@Observes NewTaskEvent event) {
        if (TextUtils.isEmpty(event.getDescription())) {
            Log.d(TAG, "Ignoring new task event with no description");
            return;
        }
        
        Id projectId = event.getProjectId();
        Id contextId = event.getContextId();
        // apply default context if project set but not context
        if (projectId.isInitialised() && !contextId.isInitialised()) {
            Project project = mProjectPersister.findById(projectId);
            contextId = project.getDefaultContextId();
        }

        Task.Builder builder = Task.newBuilder();
        builder.setDescription(event.getDescription()).
                setOrder(mTaskPersister.calculateTaskOrder(null, event.getProjectId())).
                setProjectId(projectId).
                setCreatedDate(System.currentTimeMillis()).
                setModifiedDate(System.currentTimeMillis());

        if (contextId.isInitialised()) {
            List<Id> contextIds = Lists.newArrayList(contextId);
            builder.setContextIds(contextIds);
        }

        mTaskPersister.insert(builder.build());
        String entityName = mActivity.getString(R.string.task_name);
        showSavedToast(entityName);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onNewProject(@Observes NewProjectEvent event) {
        if (TextUtils.isEmpty(event.getName())) {
            Log.d(TAG, "Ignoring new project event with no name");
            return;
        }

        Project.Builder builder = Project.newBuilder();
        builder.setName(event.getName()).
                setModifiedDate(System.currentTimeMillis());

        mProjectPersister.insert(builder.build());
        String entityName = mActivity.getString(R.string.project_name);
        showSavedToast(entityName);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onNewContext(@Observes NewContextEvent event) {
        if (TextUtils.isEmpty(event.getName())) {
            Log.d(TAG, "Ignoring new context event with no name");
            return;
        }

        Context.Builder builder = Context.newBuilder();
        builder.setName(event.getName()).
                setModifiedDate(System.currentTimeMillis());

        mContextPersister.insert(builder.build());
        String entityName = mActivity.getString(R.string.context_name);
        showSavedToast(entityName);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void showDeletedToast(String entityName, boolean isDeleted) {
        String text = mActivity.getResources().getString(
                isDeleted ? R.string.itemDeletedToast : R.string.itemUndeletedToast,
                entityName);
        View topView = mActivity.findViewById(android.R.id.content);
        Snackbar.make(topView, text, Snackbar.LENGTH_SHORT).show();
    }
    
    private void showSavedToast(String entityName) {
        String text = mActivity.getString(R.string.itemSavedToast, entityName);
        View topView = mActivity.findViewById(android.R.id.content);
        Snackbar.make(topView, text, Snackbar.LENGTH_SHORT).show();
    }
    
}
