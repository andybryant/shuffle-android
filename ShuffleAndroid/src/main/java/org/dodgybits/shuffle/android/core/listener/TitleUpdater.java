package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class TitleUpdater {

    private AppCompatActivity mActivity;

    @Inject
    EntityCache<Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    public TitleUpdater(Activity activity) {
        mActivity = (AppCompatActivity) activity;
    }

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        Location location = event.getLocation();
        if (location == null || location.getViewMode() == null) {
            return;
        }
        String title = "";
        ListQuery listQuery = location.getListQuery();
        switch (location.getViewMode()) {
            case TASK:
                // blank for task view unless in landscape mode on tablet
                // also don't show in project task list view as project already shown in task view
                if (UiUtilities.showListOnViewTask(mActivity.getResources()) && listQuery != ListQuery.project) {
                    title = getTaskListTitle(location);
                }
                break;
            case TASK_LIST:
                title = getTaskListTitle(location);
                break;
            case CONTEXT_LIST:
            case PROJECT_LIST:
            case SEARCH_RESULTS_LIST:
            case SEARCH_RESULTS_TASK:
                title = getListQueryTitle(listQuery);
                break;
        }
        mActivity.setTitle(title);
    }

    public String getTaskListTitle(Location location) {
        String title = "";
        ListQuery listQuery = location.getListQuery();
        if (listQuery == ListQuery.context) {
            Context context = mContextCache.findById(location.getContextId());
            if (context != null) {
                title = context.getName();
            }
        } else if (listQuery == ListQuery.project) {
            Project project = mProjectCache.findById(location.getProjectId());
            if (project != null) {
                title = project.getName();
            }
        }
        if (title.isEmpty()) {
            title = getListQueryTitle(listQuery);
        }
        return title;
    }

    private String getListQueryTitle(ListQuery listQuery) {
        return UiUtilities.getTitle(mActivity.getResources(), listQuery);
    }
}
