package org.dodgybits.shuffle.android.persistence.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class  ContextProvider extends AbstractCollectionProvider {
	public static final String CONTEXT_TABLE_NAME = "context";

    public static final String UPDATE_INTENT = "org.dodgybits.shuffle.android.CONTEXT_UPDATE";

	private static final String AUTHORITY = Shuffle.PACKAGE + ".contextprovider";

	static final int CONTEXT_TASKS = 103;

    private static final String URL_COLLECTION_NAME = "contexts";

	public ContextProvider() {
		super(
		        AUTHORITY,           // authority
		        URL_COLLECTION_NAME, // collectionNamePlural
		        CONTEXT_TABLE_NAME,  // tableName
                UPDATE_INTENT,       // update intent action
		        Contexts.NAME,       // primary key
		        BaseColumns._ID,     // id field
		        Contexts.CONTENT_URI,// content URI
		        BaseColumns._ID,     // fields...
		        Contexts.NAME, 
		        Contexts.COLOUR,
				Contexts.ICON,
				ShuffleTable.MODIFIED_DATE,
				ShuffleTable.DELETED,
				ShuffleTable.ACTIVE,
                ShuffleTable.GAE_ID,
                ShuffleTable.CHANGE_SET
				);

        makeSearchable(Contexts._ID, Contexts.NAME, Contexts.NAME, Contexts.NAME);
		uriMatcher.addURI(AUTHORITY, "contextTasks", CONTEXT_TASKS);
		restrictionBuilders.put(CONTEXT_TASKS, 
		        new CustomElementFilterRestrictionBuilder(
		                "context, task, taskContext",
                        "task._id = taskContext.taskId AND context._id = taskContext.contextId",
                        "context._id"));
        groupByBuilders.put(CONTEXT_TASKS, 
                new StandardGroupByBuilder("context._id"));
        elementInserters.put(COLLECTION_MATCH_ID, new ContextInserter());
		setDefaultSortOrder(Contexts.DEFAULT_SORT_ORDER);
	}


	/**
	 * Contexts table
	 */
	public static final class Contexts implements ShuffleTable {
		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
				+ "/contexts");
		public static final Uri CONTEXT_TASKS_CONTENT_URI = Uri
				.parse("content://" + AUTHORITY + "/contextTasks");

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String NAME = "name";
		public static final String COLOUR = "colour";
		public static final String ICON = "iconName";
		/**
		 * Projection for all the columns of a context.
		 */
		public static final String[] FULL_PROJECTION = new String[] { 
		    _ID,
		    NAME, 
		    COLOUR, 
		    ICON, 
		    MODIFIED_DATE,
		    DELETED, 
		    ACTIVE,
            GAE_ID,
            CHANGE_SET
		    };

		public static final String TASK_COUNT = "count";
		/**
		 * Projection for fetching the task count for each context.
		 */
		public static final String[] FULL_TASK_PROJECTION = new String[] { _ID,
				TASK_COUNT, };
	}
	
    private class ContextInserter extends ElementInserterImpl {

        public ContextInserter() {
            super(Contexts.NAME);
        }

    }
	

}
