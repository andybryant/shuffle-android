package org.dodgybits.shuffle.android.core.listener;

import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

import org.dodgybits.shuffle.android.core.activity.HelpActivity;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.activity.TaskSearchResultsActivity;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.activity.PreferencesActivity;
import org.dodgybits.shuffle.android.view.activity.TaskViewActivity;

public class LocationParser {
    private static final String TAG = "LocationParser";

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static int CONTEXT = 1;
    private static int PROJECT = 2;
    static {
        URI_MATCHER.addURI(ContextProvider.AUTHORITY, "contexts/#", CONTEXT);
        URI_MATCHER.addURI(ProjectProvider.AUTHORITY, "projects/#", PROJECT);
    }

    private Location.LocationActivity mLocationActivity;

    public void setLocationActivity(Location.LocationActivity locationActivity) {
        mLocationActivity = locationActivity;
    }

    public Location parseIntent(Intent intent) {
        Log.d(TAG, "IN parseIntent. action=" + intent.getAction() + " data=" + intent.getData());

        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(mLocationActivity);

        // based on what activity this is, parse appropriate intents
        switch (mLocationActivity) {
            case TaskList:
                parseTaskListIntent(intent, builder);
                break;

            case TaskSearch:
                final String query = intent.getStringExtra(SearchManager.QUERY);
                builder.setSearchQuery(query)
                        .setListQuery(ListQuery.search);
                break;

            case EditProject:
                break;

            case EditContext:
                break;

            case EditTask:
                break;
        }

        Location location = builder.build();
        Log.i(TAG, "Parsed location " + location);
        return location;
    }

    private void parseTaskListIntent(Intent intent, Location.Builder builder) {
        Uri uri = intent.getData();
        String queryName = intent.getStringExtra(Location.QUERY_NAME);
        if (queryName != null) {
            ListQuery query = ListQuery.valueOf(queryName);
            builder.setListQuery(query);
        } else if (uri != null) {
            int match = URI_MATCHER.match(uri);
            if (match == CONTEXT) {
                long contextId = ContentUris.parseId(uri);
                builder.setListQuery(ListQuery.context)
                        .setEntityId(Id.create(contextId));
            } else if (match == PROJECT) {
                long projectId = ContentUris.parseId(uri);
                builder.setListQuery(ListQuery.project)
                        .setEntityId(Id.create(projectId));
            } else {
                Log.e(TAG, "Unexpected intent uri" + uri);
            }
        } else {
            Log.e(TAG, "Unexpected intent " + intent);
        }
    }

    public static Intent createIntent(Context context, Location location) {
        Intent intent = null;
        Id entityId = location.getEntityId();
        ListQuery listQuery = location.getListQuery();

        switch (location.getLocationActivity()) {
            case Help:
                intent = new Intent(context, HelpActivity.class);
                if (listQuery != null) {
                    intent.putExtra(HelpActivity.QUERY_NAME, listQuery.name());
                }
                break;

            case Preferences:
                intent = new Intent(context, PreferencesActivity.class);
                break;

            case ContextList:
                intent = new Intent(Intent.ACTION_VIEW, ContextProvider.Contexts.CONTENT_URI);
                break;

            case ProjectList:
                intent = new Intent(Intent.ACTION_VIEW, ProjectProvider.Projects.CONTENT_URI);
                break;

            case TaskList:
                intent = createTaskListIntent(context, location);
                break;

            case TaskSearch:
                intent = new Intent(context, TaskSearchResultsActivity.class);
                intent.setAction(Intent.ACTION_SEARCH);
                intent.getExtras().putString(SearchManager.QUERY, location.getSearchQuery());
                break;

            case EditTask:
                createEditTaskIntent(entityId, listQuery);
                break;

            case EditProject:
                intent = createEditIntent(ProjectProvider.Projects.CONTENT_URI, entityId);
                break;

            case EditContext:
                intent = createEditIntent(ContextProvider.Contexts.CONTENT_URI, entityId);
                break;

            default:
                Log.e(TAG, "Unknown location activity " + location.getLocationActivity());
        }

        return intent;
    }

    private static Intent createTaskListIntent(Context context, Location location) {
        Intent intent;
        Id entityId = location.getEntityId();
        ListQuery listQuery = location.getListQuery();

        if (listQuery == ListQuery.project) {
            Uri url = ContentUris.withAppendedId(ProjectProvider.Projects.CONTENT_URI, entityId.getId());
            intent = new Intent(Intent.ACTION_VIEW, url);
        } else if (listQuery == ListQuery.context) {
            Uri url = ContentUris.withAppendedId(ContextProvider.Contexts.CONTENT_URI, entityId.getId());
            intent = new Intent(Intent.ACTION_VIEW, url);
        } else {
            intent = new Intent(context, TaskViewActivity.class);
        }
        intent.putExtra(Location.QUERY_NAME, listQuery.name());
        if (location.getSelectedIndex() > -1) {
            intent.putExtra(Location.SELECTED_INDEX, location.getSelectedIndex());
        }

        return intent;
    }

    private static Intent createEditTaskIntent(Id entityId, ListQuery listQuery) {
        Intent intent;
        if (entityId.isInitialised()) {
            if (listQuery == ListQuery.project) {
                intent = createEditIntent(TaskProvider.Tasks.CONTENT_URI, Id.NONE);
                intent.putExtra(TaskProvider.Tasks.PROJECT_ID, entityId.getId());
            } else if (listQuery == ListQuery.context) {
                intent = createEditIntent(TaskProvider.Tasks.CONTENT_URI, Id.NONE);
                intent.putExtra(TaskProvider.TaskContexts.CONTEXT_ID, entityId.getId());
            } else {
                intent = createEditIntent(TaskProvider.Tasks.CONTENT_URI, entityId);
            }
        } else {
            intent = createEditIntent(TaskProvider.Tasks.CONTENT_URI, entityId);
        }
        return intent;
    }

    private static Intent createEditIntent(Uri contentUri, Id entityId) {
        Intent intent;
        if (entityId.isInitialised()) {
            Uri uri = ContentUris.appendId(contentUri.buildUpon(), entityId.getId()).build();
            intent = new Intent(Intent.ACTION_EDIT, uri);
        } else {
            intent = new Intent(Intent.ACTION_INSERT, contentUri);
        }
        return intent;
    }

}
