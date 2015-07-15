package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

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
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.list.event.MoveTasksEvent;
import org.dodgybits.shuffle.android.list.event.NewContextEvent;
import org.dodgybits.shuffle.android.list.event.NewProjectEvent;
import org.dodgybits.shuffle.android.list.event.NewTaskEvent;
import org.dodgybits.shuffle.android.list.event.UpdateContextActiveEvent;
import org.dodgybits.shuffle.android.list.event.UpdateContextDeletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateProjectActiveEvent;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;

import java.util.List;
import java.util.Set;

import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

@ContextSingleton
public class EntityUpdateListener {
    private static final String TAG = "EntityUpdateListener";

    private final Activity mActivity;
    private final ProjectPersister mProjectPersister;
    private final ContextPersister mContextPersister;
    private final TaskPersister mTaskPersister;

    @Inject
    public EntityUpdateListener(
            Activity activity, ProjectPersister projectPersister,
            ContextPersister contextPersister, TaskPersister taskPersister) {
        mActivity = activity;
        mProjectPersister = projectPersister;
        mContextPersister = contextPersister;
        mTaskPersister = taskPersister;
    }

    private void onToggleProjectDeleted(@Observes UpdateProjectDeletedEvent event) {
        final Id id = event.getProjectId();
        Boolean isDeleted = event.isDeleted();
        if (isDeleted == null) {
            // need to look up current value and toggle
            Project project = mProjectPersister.findById(id);
            isDeleted = !project.isDeleted();
        }
        final boolean undoState = !isDeleted;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProjectPersister.updateDeletedFlag(id, undoState);
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };
        mProjectPersister.updateDeletedFlag(event.getProjectId(), isDeleted);
        String entityName = mActivity.getString(R.string.project_name);
        showDeletedToast(entityName, isDeleted, listener);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onToggleProjectActive(@Observes UpdateProjectActiveEvent event) {
        final Id id = event.getProjectId();
        Boolean isActive = event.getActive();
        if (isActive == null) {
            // need to look up current value and toggle
            Project project = mProjectPersister.findById(id);
            isActive = !project.isActive();
        }
        final boolean undoState = !isActive;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProjectPersister.updateActiveFlag(id, undoState);
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };
        mProjectPersister.updateActiveFlag(event.getProjectId(), isActive);
        String entityName = mActivity.getString(R.string.project_name);
        showActiveToast(entityName, isActive, listener);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onToggleContextDeleted(@Observes UpdateContextDeletedEvent event) {
        final Id id = event.getContextId();
        Boolean isDeleted = event.isDeleted();
        if (isDeleted == null) {
            // need to look up current value and toggle
            Context context = mContextPersister.findById(id);
            isDeleted = !context.isDeleted();
        }
        final boolean undoState = !isDeleted;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContextPersister.updateDeletedFlag(id, undoState);
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };

        mContextPersister.updateDeletedFlag(event.getContextId(), event.isDeleted());
        String entityName = mActivity.getString(R.string.context_name);
        showDeletedToast(entityName, isDeleted, listener);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onToggleContextActive(@Observes UpdateContextActiveEvent event) {
        final Id id = event.getContextId();
        Boolean isActive = event.getActive();
        if (isActive == null) {
            // need to look up current value and toggle
            Context context = mContextPersister.findById(id);
            isActive = !context.isActive();
        }
        final boolean undoState = !isActive;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContextPersister.updateActiveFlag(id, undoState);
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };
        mContextPersister.updateActiveFlag(event.getContextId(), isActive);
        String entityName = mActivity.getString(R.string.context_name);
        showActiveToast(entityName, isActive, listener);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onMoveTasks(@Observes MoveTasksEvent event) {
        mTaskPersister.moveTasksWithinProject(event.getTaskIds(), event.getCursor(), event.isMoveUp());
    }

    private void onUpdateTasksDeleted(@Observes UpdateTasksDeletedEvent event) {
        final Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateDeletedFlag(id, event.isDeleted());
        }
        final boolean undoState = !event.isDeleted();
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Long taskId : taskIds) {
                    Id id = Id.create(taskId);
                    mTaskPersister.updateDeletedFlag(id, undoState);
                }
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };

        if (event.getTaskIds().size() == 1) {
            String entityName = mActivity.getString(R.string.task_name);
            showDeletedToast(entityName, event.isDeleted(), listener);
        } else {
            String text = mActivity.getString(event.isDeleted() ?
                    R.string.tasks_deleted_toast : R.string.tasks_restored_toast);
            showToast(text, listener);
        }
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void onUpdateTaskCompleted(@Observes UpdateTasksCompletedEvent event) {
        final Set<Long> taskIds = event.getTaskIds();
        for (Long taskId : taskIds) {
            Id id = Id.create(taskId);
            mTaskPersister.updateCompleteFlag(id, event.isCompleted());
        }
        final boolean undoState = !event.isCompleted();
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Long taskId : taskIds) {
                    Id id = Id.create(taskId);
                    mTaskPersister.updateCompleteFlag(id, undoState);
                }
                SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
            }
        };

        String text;
        if (event.getTaskIds().size() == 1) {
            text = mActivity.getString(event.isCompleted() ?
                    R.string.task_complete_toast : R.string.task_incomplete_toast);
        } else {
            text = mActivity.getString(event.isCompleted() ?
                    R.string.tasks_complete_toast : R.string.tasks_incomplete_toast);
        }
        showToast(text, listener);
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
        showSavedToast(entityName, null);
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
        showSavedToast(entityName, null);
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
        showSavedToast(entityName, null);
        SyncUtils.scheduleSync(mActivity, LOCAL_CHANGE_SOURCE);
    }

    private void showDeletedToast(
            String entityName, boolean isDeleted, View.OnClickListener undoListener) {
        String text = mActivity.getResources().getString(
                isDeleted ? R.string.itemDeletedToast : R.string.itemUndeletedToast,
                entityName);
        showToast(text, undoListener);
    }
    
    private void showActiveToast(
            String entityName, boolean isActive, View.OnClickListener undoListener) {
        String text = mActivity.getResources().getString(
                isActive ? R.string.itemActiveToast : R.string.itemDeactivatedToast,
                entityName);
        showToast(text, undoListener);
    }

    private void showSavedToast(String entityName, View.OnClickListener undoListener) {
        String text = mActivity.getString(R.string.itemSavedToast, entityName);
        showToast(text, undoListener);
    }

    private void showToast(String text, View.OnClickListener undoListener) {
        View parentView = UiUtilities.getSnackBarParentView(mActivity);
        Snackbar snackbar = Snackbar.make(parentView, text, Snackbar.LENGTH_LONG);
        if (undoListener != null) {
            snackbar.setAction(R.string.undo_button_title, undoListener);
        }
        snackbar.show();
    }

}
