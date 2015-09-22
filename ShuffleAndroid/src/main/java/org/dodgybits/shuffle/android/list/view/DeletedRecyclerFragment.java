package org.dodgybits.shuffle.android.list.view;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.list.view.task.TaskListItem;

import roboguice.event.EventManager;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

public class DeletedRecyclerFragment extends RoboFragment {

    @Inject
    private TaskPersister mTaskPersister;

    @Inject
    private ProjectPersister mProjectPersister;

    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private ContextScopedProvider<EntityListItem> mProjectListItemProvider;

    @Inject
    private ContextScopedProvider<TaskListItem> mTaskListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;



}
