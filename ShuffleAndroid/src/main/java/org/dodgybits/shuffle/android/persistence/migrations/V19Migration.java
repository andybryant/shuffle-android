package org.dodgybits.shuffle.android.persistence.migrations;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;

public class V19Migration implements Migration {
    private static final String TAG = "V19Migration";

	@Override
	public void migrate(SQLiteDatabase db) {
        Log.d(TAG, "Adding bit masks");

        db.execSQL("ALTER TABLE " + TaskProvider.TASK_TABLE_NAME
                + " ADD COLUMN changeSet INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE " + ContextProvider.CONTEXT_TABLE_NAME
                + " ADD COLUMN changeSet INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE " + ProjectProvider.PROJECT_TABLE_NAME
                + " ADD COLUMN changeSet INTEGER NOT NULL DEFAULT 0;");
    }

}
