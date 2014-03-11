package org.dodgybits.shuffle.android.persistence.provider;

import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCollectionProvider extends ContentProvider {
	public static final String DATABASE_NAME = "shuffle.db";
	static final int DATABASE_VERSION = 20;
	public static final String TAG = "AbstractColProvider";
	
	public static interface ShuffleTable extends BaseColumns {
		static final String CONTENT_TYPE_PATH = "vnd.dodgybits";
		static final String CONTENT_TYPE_PRE_PREFIX = "vnd.android.cursor.dir/";
		static final String CONTENT_ITEM_TYPE_PRE_PREFIX = "vnd.android.cursor.item/";
		static final String CONTENT_TYPE_PREFIX = CONTENT_TYPE_PRE_PREFIX+CONTENT_TYPE_PATH;
		static final String CONTENT_ITEM_TYPE_PREFIX = CONTENT_ITEM_TYPE_PRE_PREFIX+CONTENT_TYPE_PATH;

        public static final String MODIFIED_DATE = "modified";
        public static final String DELETED = "deleted";
        public static final String ACTIVE = "active";
        public static final String GAE_ID = "gaeId";
        public static final String CHANGE_SET = "changeSet";
	}

	protected static final int SEARCH = 3;
	protected static final int COLLECTION_MATCH_ID = 1;
	protected static final int ELEMENT_MATCH_ID = 2;
	protected static Map<String, String> createSuggestionsMap(String idField,
			String column1Field, String column2Field) {
		HashMap<String, String> sSuggestionProjectionMap = new HashMap<String, String>();
		sSuggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
	            column1Field + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		sSuggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_2,
	    		column2Field + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
		sSuggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
	    		idField + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
	    sSuggestionProjectionMap.put(idField, idField);
		return sSuggestionProjectionMap;
	}

	protected static Map<String, String> createTableMap(String tableName,
			String... fieldNames) {
		HashMap<String, String> fieldNameMap = new HashMap<String, String>();
		
		for (String fieldName : fieldNames) {
			
			fieldNameMap.put(fieldName, tableName + "."+fieldName);
		}
		return fieldNameMap;
	}
	private final String authority;
	protected DatabaseHelper mOpenHelper;
	protected Map<String, String> suggestionProjectionMap;
	protected final Map<Integer, RestrictionBuilder> restrictionBuilders;
	protected final Map<Integer, GroupByBuilder> groupByBuilders;
	protected final Map<Integer, CollectionUpdater> collectionUpdaters;
	protected final Map<Integer, ElementInserter> elementInserters;
	protected final Map<Integer, ElementDeleter> elementDeleters;
	private final String tableName;
	private final String updateIntentAction;
	private final Map<String, String> elementsMap;
	protected final Map<Integer,String> mimeTypes;
	protected final Uri contentUri;
	private String defaultSortOrder = null;

	protected void setDefaultSortOrder(String defaultSortOrder) {
		this.defaultSortOrder = defaultSortOrder;
	}

	protected String getTableName() {
		return tableName;
	}
	
	protected Map<String,String> getElementsMap() {
		return elementsMap;
	}
	
    private void notifyOnChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
        getContext().sendBroadcast(new Intent(updateIntentAction));
    }

	public static interface RestrictionBuilder {
		void addRestrictions(Uri uri, SQLiteQueryBuilder qb);
	}
	
	private class EntireCollectionRestrictionBuilder implements RestrictionBuilder {
		@Override
		public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
			qb.setTables(getTableName());
			qb.setProjectionMap(getElementsMap());			
		}
	}
	
	private class ElementByIdRestrictionBuilder implements RestrictionBuilder {
		@Override
		public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
			qb.setTables(getTableName());
			qb.appendWhere("_id=" + ContentUris.parseId(uri));
		}
	}
	
	private class SearchRestrictionBuilder implements RestrictionBuilder {

		private final String[] searchFields;
		public SearchRestrictionBuilder(String[] searchFields) {
			super();
			this.searchFields = searchFields;
		}
		@Override
		public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
			qb.setTables(getTableName());
			String query = uri.getLastPathSegment();
			if (!TextUtils.isEmpty(query)) {
				for (int i = 0; i < searchFields.length; i++) {
					String field = searchFields[i];
					qb.appendWhere(field + " LIKE ");
					qb.appendWhereEscapeString('%' + query + '%');
					if (i < searchFields.length - 1)
						qb.appendWhere(" OR ");
				}
			}
			qb.setProjectionMap(suggestionProjectionMap);			
		}
		
	}
	
	protected class CustomElementFilterRestrictionBuilder implements RestrictionBuilder {

		private final String tables;
		private final String restrictions;
		private final String idField;
		
		public CustomElementFilterRestrictionBuilder(String tables,
				String restrictions, String idField) {
			super();
			this.tables = tables;
			this.restrictions = restrictions;
			this.idField = idField;
		}

		@Override
		public void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
			Map<String, String> projectionMap = new HashMap<String, String>();
			projectionMap.put("_id", idField);
			projectionMap.put("count", "count(*)");
			qb.setProjectionMap(projectionMap);
			qb.setTables(tables);
			qb.appendWhere(restrictions);
		}
		
	}
	
    public static interface GroupByBuilder {
        String getGroupBy(Uri uri);
    }
	
	protected class StandardGroupByBuilder implements GroupByBuilder
	{
	    private String mGroupBy;
	    
	    public StandardGroupByBuilder(String groupBy) {
	        mGroupBy = groupBy;
	    }
	    
	    @Override
	    public String getGroupBy(Uri uri) {
	        return mGroupBy;
	    }
	}

	protected final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	public AbstractCollectionProvider(String authority, String collectionNamePlural, 
	        String tableName,
	        String updateIntentAction,
	        String primaryKey, String idField,Uri contentUri, 
	        String... fields) {
		this.authority = authority;
		this.contentUri = contentUri;
		registerCollectionUrls(collectionNamePlural);
		this.restrictionBuilders = new HashMap<Integer, RestrictionBuilder>();
		this.restrictionBuilders.put(COLLECTION_MATCH_ID, new EntireCollectionRestrictionBuilder());
		this.restrictionBuilders.put(ELEMENT_MATCH_ID, new ElementByIdRestrictionBuilder());
		this.tableName = tableName;
		this.updateIntentAction = updateIntentAction;
		this.elementsMap = createTableMap(tableName, fields);
		this.mimeTypes = new HashMap<Integer, String>();
		this.mimeTypes.put(COLLECTION_MATCH_ID, getContentType());
		this.mimeTypes.put(ELEMENT_MATCH_ID, getContentItemType());
		this.collectionUpdaters = new HashMap<Integer, CollectionUpdater>();
		this.collectionUpdaters.put(COLLECTION_MATCH_ID, new EntireCollectionUpdater());
		this.collectionUpdaters.put(ELEMENT_MATCH_ID, new SingleElementUpdater());
		this.elementInserters = new HashMap<Integer, ElementInserter>();
		this.elementInserters.put(COLLECTION_MATCH_ID, new ElementInserterImpl(primaryKey));
		this.elementDeleters = new HashMap<Integer, ElementDeleter>();
		this.elementDeleters.put(COLLECTION_MATCH_ID, new EntireCollectionDeleter());
		this.elementDeleters.put(ELEMENT_MATCH_ID, new ElementDeleterImpl(idField));
        this.groupByBuilders = new HashMap<Integer, GroupByBuilder>();
	}
	
	@Override
	public String getType(Uri uri) {
		String mimeType = mimeTypes.get(match(uri));
		if (mimeType == null) throw new IllegalArgumentException("Unknown Uri " + uri);
		return mimeType;
	}


	SQLiteQueryBuilder createQueryBuilder() {
		return new SQLiteQueryBuilder();
	}
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = getWriteableDatabase();
	
		int count = doDelete(uri, where, whereArgs, db);
		notifyOnChange(uri);
		return count;
	}
	


	SQLiteDatabase getReadableDatabase() {
		return mOpenHelper.getReadableDatabase();
	}
	protected String getSortOrder(Uri uri, String sort) {
		if (defaultSortOrder != null && TextUtils.isEmpty(sort)) {
			return defaultSortOrder;
		}
		return sort;
	}
	protected SQLiteDatabase getWriteableDatabase() {
		return mOpenHelper.getWritableDatabase();
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
	
		SQLiteDatabase db = getWriteableDatabase();
	
		return doInsert(url, values, db);
	
	}
	protected void makeSearchable(String idField, String descriptionField,
			String detailsField, String...searchFields) {
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
		suggestionProjectionMap = createSuggestionsMap(idField,descriptionField,detailsField);
		restrictionBuilders.put(SEARCH, new SearchRestrictionBuilder(searchFields));
	}

	public int match(Uri uri) {
		return uriMatcher.match(uri);
	}
	
	@Override
	public boolean onCreate() {
		Log.i(TAG, "+onCreate");
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	protected int doUpdate(Uri uri, ContentValues values, String where,
			String[] whereArgs, SQLiteDatabase db) {
		CollectionUpdater updater = collectionUpdaters.get(match(uri));
		if (updater == null) throw new IllegalArgumentException("Unknown URL " + uri);
		return updater.update(uri, values, where, whereArgs, db);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = createQueryBuilder();

		SQLiteDatabase db = getReadableDatabase();

		addRestrictions(uri, qb);

		String orderBy = getSortOrder(uri, sort);
		String groupBy = getGroupBy(uri);
		
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "Executing " + selection + " with args "
					+ Arrays.toString(selectionArgs) + " ORDER BY " + orderBy + " (sort=" + sort + ")");
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
				null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	protected String getGroupBy(Uri uri) {
	    String groupBy = null;
	    GroupByBuilder builder = groupByBuilders.get(match(uri));
	    if (builder != null) {
	        groupBy = builder.getGroupBy(uri);
	    }
        return groupBy;
    }

    protected void registerCollectionUrls(String collectionName) {
		uriMatcher.addURI(authority, collectionName, COLLECTION_MATCH_ID);
		uriMatcher.addURI(authority, collectionName+"/#", ELEMENT_MATCH_ID);
	}
	protected String getContentType() {
		return ShuffleTable.CONTENT_TYPE_PREFIX+"."+getTableName();
	}
	
	public String getContentItemType() {
		return ShuffleTable.CONTENT_ITEM_TYPE_PREFIX+"."+getTableName();
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		int count = 0;
		SQLiteDatabase db = getWriteableDatabase();
		count = doUpdate(uri, values, where, whereArgs, db);
		notifyOnChange(uri);
		return count;
	}
	protected void addRestrictions(Uri uri, SQLiteQueryBuilder qb) {
		RestrictionBuilder restrictionBuilder = restrictionBuilders.get(match(uri));
		if (restrictionBuilder == null) throw new IllegalArgumentException("Unknown URL " + uri);
		restrictionBuilder.addRestrictions(uri, qb);
	}
	
	public interface CollectionUpdater {
		int update(Uri uri, ContentValues values, String where,
			String[] whereArgs, SQLiteDatabase db);
	}
	
	private class EntireCollectionUpdater implements CollectionUpdater {
		@Override
		public int update(Uri uri, ContentValues values, String where,
				String[] whereArgs, SQLiteDatabase db) {
			return db.update(getTableName(), values, where, whereArgs);
		}
	}
	private class SingleElementUpdater implements CollectionUpdater {
		@Override
		public int update(Uri uri, ContentValues values, String where,
				String[] whereArgs, SQLiteDatabase db) {
			String segment = uri.getPathSegments().get(1);
			return db.update(getTableName(), values,
					"_id="
							+ segment
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
		}
	}
	
	public interface ElementInserter {
		Uri insert(Uri url, ContentValues values, SQLiteDatabase db);
	}
	
	protected class ElementInserterImpl implements ElementInserter {
		private final String primaryKey;
		
		
		public ElementInserterImpl(String primaryKey) {
			super();
			this.primaryKey = primaryKey;
		}


		@Override
		public Uri insert(Uri url, ContentValues values, SQLiteDatabase db) {
			addDefaultValues(values);

			long rowID = db.insert(getTableName(), getElementsMap()
					.get(primaryKey), values);
			if (rowID > 0) {
				Uri uri = ContentUris.withAppendedId(contentUri,
						rowID);
				notifyOnChange(uri);
				return uri;
			}
			throw new SQLException("Failed to insert row into " + url);
		}


		protected void addDefaultValues(ContentValues values) {
            Long now = System.currentTimeMillis();
            if (!values.containsKey(ShuffleTable.MODIFIED_DATE)) {
                values.put(ShuffleTable.MODIFIED_DATE, now);
            }
            if (!values.containsKey(ShuffleTable.DELETED)) {
                values.put(ShuffleTable.DELETED, 0);
            }
            if (!values.containsKey(ShuffleTable.ACTIVE)) {
                values.put(ShuffleTable.ACTIVE, 1);
            }
            
            if (!values.containsKey(primaryKey)) {
				values.put(primaryKey, "");
			}
		}
		
	}
	protected Uri doInsert(Uri url, ContentValues values, SQLiteDatabase db) {
		ElementInserter elementInserter = elementInserters.get(match(url));
		if (elementInserter == null) throw new IllegalArgumentException("Unknown URL " + url);
		return elementInserter.insert(url, values, db);
	}
	
	public static interface ElementDeleter {
		int delete(Uri uri, String where, String[] whereArgs,
				SQLiteDatabase db);
	}
	
	private class ElementDeleterImpl implements ElementDeleter {
		private final String idField;
		
		public ElementDeleterImpl(String idField) {
			super();
			this.idField = idField;
		}

		@Override
		public int delete(Uri uri, String where, String[] whereArgs,
				SQLiteDatabase db) {
			String id = uri.getPathSegments().get(1);
			int rowsUpdated = db.delete(getTableName(),
                    idField + "=" + id +
                    (!TextUtils.isEmpty(where) ? " AND (" + where +
                            ')' : ""), whereArgs);
			notifyOnChange(uri);
			return rowsUpdated;
		}
	}

    private class EntireCollectionDeleter implements ElementDeleter {
        @Override
        public int delete(Uri uri, String where, String[] whereArgs,
                SQLiteDatabase db) {
            int rowsUpdated = db.delete(getTableName(), where, whereArgs);
            notifyOnChange(uri);
            return rowsUpdated;
        }
    }
	
	
	
	protected int doDelete(Uri uri, String where, String[] whereArgs,
			SQLiteDatabase db) {
		ElementDeleter elementDeleter = elementDeleters.get(match(uri));
		if (elementDeleter == null)	throw new IllegalArgumentException("Unknown uri " + uri);
		return elementDeleter.delete(uri, where, whereArgs, db);
	}
}