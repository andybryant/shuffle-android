package org.dodgybits.shuffle.android.list.listener;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
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
import roboguice.event.Observes;

import java.util.Set;

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

    public void onToggleProjectDeleted(@Observes UpdateProjectDeletedEvent event) {
        mProjectPersister.updateDeletedFlag(event.getProjectId(), event.isDeleted());
        String entityName = mActivity.getString(R.string.project_name);
        if (event.isDeleted()) {
            showDeletedToast(entityName);
        } else {
            showSavedToast(entityName);
        }
    }

    public void onToggleContextDeleted(@Observes UpdateContextDeletedEvent event) {
        mContextPersister.updateDeletedFlag(event.getContextId(), event.isDeleted());
        String entityName = mActivity.getString(R.string.context_name);
        if (event.isDeleted()) {
            showDeletedToast(entityName);
        } else {
            showSavedToast(entityName);
        }
    }

    public void onMoveTasks(@Observes MoveTasksEvent event) {
        // TODO
    }

    public void onToggleTasksDeleted(@Observes UpdateTasksDeletedEvent event) {
        Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateDeletedFlag(id, event.isDeleted());
        }

        String entityName = mActivity.getString(R.string.task_name);
        if (event.isDeleted()) {
            showDeletedToast(entityName);
        } else {
            showSavedToast(entityName);
        }
    }

    public void onToggleTaskCompleted(@Observes UpdateTasksCompletedEvent event) {
        Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateCompleteFlag(id, event.isCompleted());
        }

        String entityName = mActivity.getString(R.string.task_name);
        showSavedToast(entityName);
    }
    
    public void onNewTask(@Observes NewTaskEvent event) {
        if (TextUtils.isEmpty(event.getDescription())) {
            Log.d(TAG, "Ignoring new task event with no description");
            return;
        }

        Task.Builder builder = Task.newBuilder();
        builder.setDescription(event.getDescription()).
                setContextId(event.getContextId()).
                setProjectId(event.getProjectId()).
                setCreatedDate(System.currentTimeMillis()).
                setModifiedDate(System.currentTimeMillis());

        mTaskPersister.insert(builder.build());
        String entityName = mActivity.getString(R.string.task_name);
        showSavedToast(entityName);
    }

    public void onNewProject(@Observes NewProjectEvent event) {
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
    }

    public void onNewContext(@Observes NewContextEvent event) {
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
    }

    private void showDeletedToast(String entityName) {
        String text = mActivity.getResources().getString(
                R.string.itemDeletedToast, entityName);
        Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
    }
    
    private void showSavedToast(String entityName) {
        String text = mActivity.getString(R.string.itemSavedToast, entityName);
        Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
    }
    
}