package org.dodgybits.shuffle.android.persistence.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class ProjectProvider extends AbstractCollectionProvider {
	   
	public static final String PROJECT_TABLE_NAME = "project";

    public static final String UPDATE_INTENT = "org.dodgybits.shuffle.android.PROJECT_UPDATE";

	public static final String AUTHORITY = Shuffle.PACKAGE+".projectprovider";

	static final int PROJECT_TASKS = 203;

	private static final String URL_COLLECTION_NAME = "projects";
	

	public ProjectProvider() {
        super(
                AUTHORITY,           // authority
                URL_COLLECTION_NAME, // collectionNamePlural
                PROJECT_TABLE_NAME,  // tableName
                UPDATE_INTENT,       // update intent action
                Projects.NAME,       // primary key
                BaseColumns._ID,     // id field
                Projects.CONTENT_URI,// content URI
                BaseColumns._ID,     // fields...
                Projects.NAME, 
                Projects.DEFAULT_CONTEXT_ID,
                Projects.PARALLEL,
                Projects.ARCHIVED,
                ShuffleTable.MODIFIED_DATE,
                ShuffleTable.DELETED,
                ShuffleTable.ACTIVE,
                ShuffleTable.GAE_ID,
                ShuffleTable.CHANGE_SET,
                Projects.DISPLAY_ORDER
                );
	    
		makeSearchable(Projects._ID, Projects.NAME, Projects.NAME, Projects.NAME);
		uriMatcher.addURI(AUTHORITY, "projectTasks", PROJECT_TASKS);
		String idField = "project._id";
		String tables = "project, task";
		String restrictions = "task.projectId = project._id";

		restrictionBuilders.put(PROJECT_TASKS, 
		        new CustomElementFilterRestrictionBuilder(
		                tables, restrictions, idField));
		groupByBuilders.put(PROJECT_TASKS, 
		        new StandardGroupByBuilder("project._id"));
        elementInserters.put(COLLECTION_MATCH_ID, new ProjectInserter());
		setDefaultSortOrder(Projects.DEFAULT_SORT_ORDER);
	}
	
    /**
     * Projects table
     */
    public static final class Projects implements ShuffleTable {
        public static final String ARCHIVED = "archived";
        
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + URL_COLLECTION_NAME);
        public static final Uri PROJECT_TASKS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/projectTasks");
        public static final String DEFAULT_CONTEXT_ID = "defaultContextId";
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "displayOrder ASC";
        public static final String NAME = "name";
        
        public static final String PARALLEL = "parallel";
        public static final String TASK_COUNT = "count";
        public static final String DISPLAY_ORDER = "displayOrder";

        /**
         * Projection for all the columns of a project.
         */
        public static final String[] FULL_PROJECTION = new String[] {
                _ID,
                NAME,
                DEFAULT_CONTEXT_ID,
                MODIFIED_DATE,
                PARALLEL,
                ARCHIVED,
                DELETED,
                ACTIVE,
                GAE_ID,
                CHANGE_SET,
                DISPLAY_ORDER
        };
        /**
         * Projection for fetching the task count for each project.
         */
        public static final String[] FULL_TASK_PROJECTION = new String[] {
            _ID,
            TASK_COUNT,
        };
    }
    
    private class ProjectInserter extends ElementInserterImpl {

        public ProjectInserter() {
            super(Projects.NAME);
        }
        
    }
    
    
}
