package org.dodgybits.shuffle.android.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.view.ViewParent;
import android.widget.ListView;
import com.google.common.collect.Sets;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.model.ListQuery;

import java.util.Set;

public class UiUtilities {
    private UiUtilities() {
    }

    /**
     * Returns a boolean indicating whether the table UI should be shown.
     */
    public static boolean useTabletUI(Resources res) {
        return res.getBoolean(R.bool.use_tablet_ui);
    }

    public static boolean showListOnViewTask(Resources res) {
        return useTabletUI(res) && !res.getBoolean(R.bool.list_collapsible);
    }

    public static View getSnackBarParentView(Activity activity) {
        View parent = activity.findViewById(R.id.coordinator_layout);
        if (parent == null) {
            parent = activity.findViewById(android.R.id.content);
        }
        return parent;
    }
    public static String getTitle(Resources res, ListQuery listQuery) {
        String title = "";
        switch (listQuery) {
            case inbox:
                title = res.getString(R.string.title_inbox);
                break;
            case dueTasks:
                title = res.getString(R.string.title_due_tasks);
                break;
            case nextTasks:
                title = res.getString(R.string.title_next_tasks);
                break;
            case project:
                title = res.getString(R.string.title_project);
                break;
            case context:
                title = res.getString(R.string.title_context);
                break;
            case deferred:
                title = res.getString(R.string.title_deferred);
                break;
            case deleted:
                title = res.getString(R.string.title_deleted);
                break;
            case search:
                title = res.getString(R.string.title_search);
                break;

        }
        return title;
    }

    /** Generics version of {@link android.app.Activity#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(Activity parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /** Generics version of {@link View#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(View parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /**
     * Same as {@link Activity#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(Activity parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }


    private static Set<ListQuery> sEntityListQueries = Sets.newHashSet(ListQuery.project, ListQuery.context);

    public static boolean showHomeAsUp(Resources res, Location location) {
        return (location.getViewMode() == ViewMode.TASK && !showListOnViewTask(res)) ||
                (location.getViewMode() == ViewMode.TASK_LIST && (sEntityListQueries.contains(location.getListQuery())));
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(Activity parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Returns the x coordinates of a view by tracing up its hierarchy.
     */
    public static int getX(View view) {
        int x = 0;
        while (view != null) {
            x += (int) view.getX();
            ViewParent parent = view.getParent();
            view = parent != null ? (View) parent : null;
        }
        return x;
    }

    /**
     * Returns the y coordinates of a view by tracing up its hierarchy.
     */
    public static int getY(View view) {
        int y = 0;
        while (view != null) {
            y += (int) view.getY();
            ViewParent parent = view.getParent();
            view = parent != null ? (View) parent : null;
        }
        return y;
    }

    /**
     * Workaround for the {@link android.widget.ListView#smoothScrollToPosition} randomly scroll the view bug
     * if it's called right after {@link android.widget.ListView#setAdapter}.
     */
    public static void listViewSmoothScrollToPosition(final Activity activity,
                                                      final ListView listView, final int position) {
        // Workarond: delay-call smoothScrollToPosition()
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) {
                    return; // Activity being destroyed
                }
                listView.smoothScrollToPosition(position);
            }
        });
    }

    public static String getCountForUi(Context context, int count,
                                              boolean replaceZeroWithBlank) {
        if (replaceZeroWithBlank && (count == 0)) {
            return "";
        } else if (count > 999) {
            return context.getString(R.string.more_than_999);
        } else {
            return Integer.toString(count);
        }
    }

}
