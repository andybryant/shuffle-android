package org.dodgybits.shuffle.android.persistence.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Map;

public class TaskProvider extends AbstractCollectionProvider {
	
	public static final String TASK_TABLE_NAME = "task";
	public static final String TASK_CONTEXT_JUNCTION_TABLE_NAME = "taskContext";

	public static final String UPDATE_INTENT = "org.dodgybits.shuffle.android.TASK_UPDATE";

    static final int TASK_CONTEXTS_COLLECTION_ID = 401;

    private static final String URL_COLLECTION_NAME = "tasks";
    private static final String URL_TASK_CONTEXT_COLLECTION_NAME = "taskContexts";

	public TaskProvider() {
        super(
                AUTHORITY,           // authority
                URL_COLLECTION_NAME, // collectionNamePlural
                TASK_TABLE_NAME,     // tableName
                UPDATE_INTENT,       // update intent action
                Tasks.DESCRIPTION,   // primary key
                BaseColumns._ID,     // id field
                Tasks.CONTENT_URI,   // content URI
                BaseColumns._ID,     // fields...
                Tasks.DESCRIPTION,
                Tasks.DETAILS,
                Tasks.PROJECT_ID,
                Tasks.CREATED_DATE,
                Tasks.START_DATE,
                Tasks.DUE_DATE,
                Tasks.TIMEZONE,
                Tasks.CAL_EVENT_ID,
                Tasks.DISPLAY_ORDER,
                Tasks.COMPLETE,
                Tasks.ALL_DAY,
                Tasks.HAS_ALARM,
                ShuffleTable.MODIFIED_DATE,
                ShuffleTable.DELETED,
                ShuffleTable.ACTIVE,
                ShuffleTable.GAE_ID,
                ShuffleTable.CHANGE_SET
                );
		
		makeSearchable(Tasks._ID, 
		        Tasks.DESCRIPTION, Tasks.DETAILS,
		        Tasks.DESCRIPTION, Tasks.DETAILS);

        // taskContext specific config
        uriMatcher.addURI(AUTHORITY, URL_TASK_CONTEXT_COLLECTION_NAME, TASK_CONTEXTS_COLLECTION_ID);
        getElementsMap().put(TaskContexts.CONTEXT_ID,
                TASK_CONTEXT_JUNCTION_TABLE_NAME + "." + TaskContexts.CONTEXT_ID);
        restrictionBuilders.put(TASK_CONTEXTS_COLLECTION_ID, new TaskContextsCollectionRestrictionBuilder());
        elementInserters.put(TASK_CONTEXTS_COLLECTION_ID, new TaskContextsInserter());
        elementDeleters.put(TASK_CONTEXTS_COLLECTION_ID, new TaskContextsCollectionDeleter());

        //restrictionBuilders.put(COLLECTION_MATCH_ID, new TaskCollectionRestrictionBuilder());
		elementInserters.put(COLLECTION_MATCH_ID, new TaskInserter());

		setDefaultSortOrder(Tasks.DEFAULT_SORT_ORDER);
	}

	public static final String AUTHORITY = Shuffle.PACKAGE + ".taskprovider";


    public static final class TaskContexts implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/taskContexts");

        public static final String TASK_ID = "taskId";
        public static final String CONTEXT_ID = "contextId";

        public static final String[] FULL_PROJECTION = new String[] {
                TASK_ID,
                CONTEXT_ID
        };

    }

    private class TaskContextsInserter implements ElementInserter {
        @Override
        public Uri insert(Uri url, ContentValues values, SQLiteDatabase db) {
            long rowId = db.insert(TASK_CONTEXT_JUNCTION_TABLE_NAME, null, values);
            if (rowId <= 0L) {
                throw new SQLException("Failed to insert row into " + url);
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Added taskContext " + rowId + " for values " + values);
            }
            Uri uri = ContentUris.withAppendedId(TaskContexts.CONTENT_URI, rowId);
            return uri;
        }
    }

    private class TaskContextsCollectionRestrictionBuilder implements RestrictionBuilder {

        private Map<String,String> mElementsMap;

        private TaskContextsCollectionRestrictionBuilder() {
            mElementsMap = createTableMap(TASK_CONTEXT_JUNCTION_TABLE_NAME,
                    TaskContexts.TASK_ID, TaskContexts.CONTEXT_ID);
        }

        @Override
        public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
            qb.setTables(TASK_CONTEXT_JUNCTION_TABLE_NAME);
            qb.setProjectionMap(mElementsMap);
        }
    }

    private class TaskContextsCollectionDeleter implements ElementDeleter {
        @Override
        public int delete(Uri uri, String where, String[] whereArgs,
                          SQLiteDatabase db) {
            int rowsUpdated = db.delete(TASK_CONTEXT_JUNCTION_TABLE_NAME, where, whereArgs);
            return rowsUpdated;
        }
    }


	/**
	 * Tasks table
	 */
	public static final class Tasks implements ShuffleTable {

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY+"/tasks");
        public static final Uri LIST_CONTENT_URI = Uri.parse("content://" + AUTHORITY+"/taskLists");

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "due ASC, created DESC";

		public static final String DESCRIPTION = "description";
		public static final String DETAILS = "details";
		public static final String PROJECT_ID = "projectId";
		public static final String CREATED_DATE = "created";
		public static final String START_DATE = "start";
		public static final String DUE_DATE = "due";
		public static final String TIMEZONE = "timezone";
		public static final String CAL_EVENT_ID = "calEventId";
		public static final String DISPLAY_ORDER = "displayOrder";
		public static final String COMPLETE = "complete";
		public static final String ALL_DAY = "allDay";
		public static final String HAS_ALARM = "hasAlarm";

		/**
		 * Projection for all the columns of a task.
		 */
		public static final String[] FULL_PROJECTION = new String[] { _ID,
				DESCRIPTION, DETAILS, PROJECT_ID, CREATED_DATE,
				MODIFIED_DATE, START_DATE, DUE_DATE, TIMEZONE, CAL_EVENT_ID,
				DISPLAY_ORDER, COMPLETE, ALL_DAY, HAS_ALARM, DELETED, ACTIVE,
                GAE_ID, CHANGE_SET };


	}

    private class TaskCollectionRestrictionBuilder implements RestrictionBuilder {
        @Override
        public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
            qb.setTables("task, taskContext");
            qb.setProjectionMap(getElementsMap());
        }
    }

	private class TaskInserter extends ElementInserterImpl {

		public TaskInserter() {
			super(Tasks.DESCRIPTION);
		}
		

		@Override
		protected void addDefaultValues(ContentValues values) {
		    super.addDefaultValues(values);
		    
		    // Make sure that the fields are all set
		    
			Long now = System.currentTimeMillis();
			if (!values.containsKey(Tasks.CREATED_DATE)) {
				values.put(Tasks.CREATED_DATE, now);
			}
			if (!values.containsKey(Tasks.DETAILS)) {
				values.put(Tasks.DETAILS, "");
			}
			if (!values.containsKey(Tasks.DISPLAY_ORDER)) {
				values.put(Tasks.DISPLAY_ORDER, 0);
			}
			if (!values.containsKey(Tasks.COMPLETE)) {
				values.put(Tasks.COMPLETE, 0);
			}
		}
	}
}
