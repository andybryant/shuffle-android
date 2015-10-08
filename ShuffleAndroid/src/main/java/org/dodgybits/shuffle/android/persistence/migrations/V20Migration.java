package org.dodgybits.shuffle.android.persistence.migrations;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class V20Migration implements Migration {
    private static final String TAG = "V20Migration";

    @Override
    public void migrate(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + ProjectProvider.PROJECT_TABLE_NAME
                + " ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0;");

        resetTaskOrder(db);
        resetProjectOrder(db);
    }

    /**
     * Clean up task ordering. Previous schema allowed
     * two tasks without a project to share the same order id
     */
    private void resetTaskOrder(SQLiteDatabase db) {
        Cursor c = db.query(TaskProvider.TASK_TABLE_NAME,
                new String[] {"_id","projectId","displayOrder"},
                null, null,
                null, null,
                "projectId ASC, due ASC, displayOrder ASC");

        long currentProjectId = -1L;
        int newOrder = 0;
        Map<String,Integer> updatedValues = new HashMap<>();
        while (c.moveToNext()) {
            long id = c.getLong(0);
            long projectId = c.getLong(1);
            int displayOrder = c.getInt(2);

            if (projectId == currentProjectId) {
                newOrder++;
            } else {
                newOrder = 0;
                currentProjectId = projectId;
            }

            if (newOrder != displayOrder) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    String message = String.format("Updating project %4$d task %1$d displayOrder from %2$d to %3$d",
                            id, displayOrder, newOrder, currentProjectId);
                    Log.d(TAG, message);
                }
                updatedValues.put(String.valueOf(id), newOrder);
            }

        }
        c.close();
        applyUpdates(db, TaskProvider.TASK_TABLE_NAME, updatedValues);
    }

    private void resetProjectOrder(SQLiteDatabase db) {
        Cursor c = db.query(ProjectProvider.PROJECT_TABLE_NAME,
                new String[] {"_id","displayOrder"},
                null, null,
                null, null,
                "name ASC");

        int newOrder = 0;
        Map<String,Integer> updatedValues = new HashMap<>();
        while (c.moveToNext()) {
            long id = c.getLong(0);
            int displayOrder = c.getInt(1);
            newOrder++;

            if (newOrder != displayOrder) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    String message = String.format("Updating project %1$d displayOrder from %2$d to %3$d",
                            id, displayOrder, newOrder);
                    Log.d(TAG, message);
                }
                updatedValues.put(String.valueOf(id), newOrder);
            }

        }
        c.close();
        applyUpdates(db, ProjectProvider.PROJECT_TABLE_NAME, updatedValues);
    }

    private void applyUpdates(SQLiteDatabase db, String table, Map<String,Integer> updatedValues) {
        ContentValues values = new ContentValues();
        Set<String> ids = updatedValues.keySet();
        for (String id : ids) {
            values.clear();
            values.put("displayOrder", updatedValues.get(id));
            db.update(table, values, BaseColumns._ID + " = ?", new String[] {id});
        }
    }

}
