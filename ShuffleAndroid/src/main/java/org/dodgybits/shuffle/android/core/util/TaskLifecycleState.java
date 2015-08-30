package org.dodgybits.shuffle.android.core.util;

import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;

import java.util.List;

public class TaskLifecycleState {

    public enum Status {
        yes, no, fromContext, fromProject
    }

    /**
     * Determine overall active status for the task.
     * A task is considered inactive if either its project is inactive or all of its contexts are.
     *
     * @param task the task to check
     * @param contexts contexts associated with this task
     * @param project project assigned to this task
     * @return yes if active, no if directly not active, fromContext if inactive via contexts, fromProject if inactive via the project
     */
    public static Status getActiveStatus(Task task, List<Context> contexts, Project project) {
        Status status;
        if (task.isActive()) {
            status = Status.yes;
            if (project != null && !project.isActive()) {
                status = Status.fromProject;
            } else if (!contexts.isEmpty()) {
                // task is inactive if all contexts are inactive
                boolean foundActive = false;
                for (Context context : contexts) {
                    if (context.isActive()) {
                        foundActive = true;
                        break;
                    }
                }
                if (!foundActive) {
                    status = Status.fromContext;
                }
            }
        } else {
            status = Status.no;
        }
        return status;
    }

    /**
     * Determine overall deleted status for the task.
     * A task is considered deleted if is directly deleted or its project is deleted
     *
     * @param task the task to check
     * @param project project assigned to this task
     * @return yes if directly deleted, no if not deleted, fromProject if deleted via the project
     */
    public static Status getDeletedStatus(Task task, Project project) {
        Status status;
        if (task.isDeleted()) {
            status = Status.yes;
        } else {
            status = Status.no;
            if (project != null && project.isDeleted()) {
                status = Status.fromProject;
            }
        }
        return status;
    }

}
