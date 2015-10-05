package org.dodgybits.shuffle.android.preference.model;

import android.content.Context;

import com.google.common.collect.Sets;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;

import java.util.Set;

public class ListFeatures {

    public static boolean showEditActions(Location location) {
        return location.getListQuery() == ListQuery.project || location.getListQuery() == ListQuery.context;
    }

    public static boolean showMoveActions(Location location) {
        return location.getListQuery() == ListQuery.inbox ||
                location.getListQuery() == ListQuery.nextTasks ||
                location.getListQuery() == ListQuery.project;
    }


    public static String getEditEntityName(Context context, Location location) {
        String name;
        switch (location.getListQuery()) {
            case context:
                name = context.getString(R.string.context_name);
                break;

            case project:
                name = context.getString(R.string.project_name);
                break;

            default:
                throw new UnsupportedOperationException("Cannot create edit event for location " + location);
        }

        return name;
    }

    public static boolean isProjectNameVisible(Location location) {
        return !location.getProjectId().isInitialised();
    }

    private static Set<ListQuery> sSupportedLists = Sets.newHashSet(
            ListQuery.inbox,
            ListQuery.dueTasks,
            ListQuery.nextTasks,
            ListQuery.context,
            ListQuery.project);

    public static boolean isSwipeSupported(Location location) {
        return sSupportedLists.contains(location.getListQuery());
    }

    public static boolean showAddFab(Location location) {
        return sSupportedLists.contains(location.getListQuery());
    }

}
