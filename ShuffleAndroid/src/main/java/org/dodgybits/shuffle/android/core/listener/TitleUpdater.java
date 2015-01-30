package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.model.ListQuery;

import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class TitleUpdater {

    private ActionBarActivity mActivity;

    @Inject
    EntityCache<Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    public TitleUpdater(Activity activity) {
        mActivity = (ActionBarActivity) activity;
    }

    private void onViewChanged(@Observes MainViewUpdateEvent event) {
        MainView mainView = event.getMainView();
        if (mainView == null || mainView.getViewMode() == null) {
            return;
        }
        String title = "";
        ListQuery listQuery = mainView.getListQuery();
        switch (mainView.getViewMode()) {
            case TASK:
                // blank for task view unless in landscape mode on tablet
                if (!UiUtilities.isListCollapsible(mActivity.getResources())) {
                    title = getTaskListTitle(mainView);
                }
                break;
            case TASK_LIST:
                title = getTaskListTitle(mainView);
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

    private String getTaskListTitle(MainView mainView) {
        String title = "";
        ListQuery listQuery = mainView.getListQuery();
        if (listQuery == ListQuery.context) {
            Context context = mContextCache.findById(mainView.getEntityId());
            if (context != null) {
                title = context.getName();
            }
        } else if (listQuery == ListQuery.project) {
            Project project = mProjectCache.findById(mainView.getEntityId());
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
