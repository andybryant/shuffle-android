package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.util.Log;
import com.google.common.collect.Lists;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;

import java.util.List;

public class EntityPickerDialogHelper {
    private static final String TAG = "EntityPkrDlgHelper";

    public static final long NONE_ID = 0L;
    public static final long ADD_NEW_ID = -1L;


    public static Dialog createSingleSelectProjectPickerDialog(Activity activity,
        boolean noneItem, boolean addNewItem) {
        final OnEntitySelected listener = (OnEntitySelected) activity;
        return createSingleSelectProjectPickerDialog(activity, listener, noneItem, addNewItem);
    }

    public static Dialog createSingleSelectProjectPickerDialog(
            Activity activity, OnEntitySelected listener,
            boolean noneItem, boolean addNewItem) {
        String title = activity.getString(R.string.title_project_picker);
        String addNewTitle = activity.getString(R.string.new_title);
        return createSingleSelectDialog(activity, listener,
                createProjectCursor(activity), title,
                addNewTitle, noneItem, addNewItem);
    }

    public static Dialog createSingleSelectContextPickerDialog(Activity activity) {
        final OnEntitySelected listener = (OnEntitySelected) activity;
        return createSingleSelectContextPickerDialog(activity, listener);
    }

    public static Dialog createSingleSelectContextPickerDialog(
            Activity activity, OnEntitySelected listener) {
        String title = activity.getString(R.string.title_context_picker);
        return createSingleSelectDialog(activity, listener, createContextCursor(activity), title,
                null, false, false);
    }

    public static Dialog createMultiSelectContextPickerDialog(
            Activity activity) {
        String title = activity.getString(R.string.title_contexts_picker);
        return createMultiSelectDialog(activity, createContextCursor(activity), title);
    }

    private static Cursor createProjectCursor(Activity activity) {
        return activity.getContentResolver().query(
                ProjectProvider.Projects.CONTENT_URI,
                new String[]{ProjectProvider.Projects._ID, ProjectProvider.Projects.NAME},
                null, null, null);
    }

    private static Cursor createContextCursor(Activity activity) {
        return activity.getContentResolver().query(
                ContextProvider.Contexts.CONTENT_URI,
                new String[]{ContextProvider.Contexts._ID, ContextProvider.Contexts.NAME},
                ContextProvider.Contexts.DELETED + "=0", null, null);
    }

    private static Dialog createSingleSelectDialog(
            Activity activity, final OnEntitySelected listener,
            Cursor cursor, String title,
            String addNewTitle, boolean noneItem, boolean addNewItem) {
        Dialog dialog;
        int count = cursor.getCount();
        if (addNewItem) count++;
        if (noneItem) count++;

        final String[] names = new String[count];
        final long[] ids = new long[count];
        cursor.moveToPosition(-1);
        int index = 0;
        if (noneItem) {
            ids[index] = NONE_ID;
            names[index] = activity.getString(R.string.none_empty);
            index++;
        }
        while (cursor.moveToNext()) {
            ids[index] = cursor.getLong(0);
            names[index] = cursor.getString(1);
            index++;
        }
        if (addNewItem) {
            ids[index] = ADD_NEW_ID;
            names[index] = addNewTitle;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);

        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                listener.onSelected(ids[item]);
            }
        });
        dialog = builder.create();
        cursor.close();
        return dialog;
    }

    private static Dialog createMultiSelectDialog(
            Activity activity, Cursor cursor, String title) {
        Dialog dialog;
        final OnEntitiesSelected multiListener = (OnEntitiesSelected) activity;
        int count = cursor.getCount();

        final String[] names = new String[count];
        final long[] ids = new long[count];
        cursor.moveToPosition(-1);
        int index = 0;
        while (cursor.moveToNext()) {
            ids[index] = cursor.getLong(0);
            names[index] = cursor.getString(1);
            index++;
        }
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);

        List<Id> selectedIds = multiListener.getInitialSelection();
        final boolean[] selectedItems = new boolean[count];
        updateSelectedFlags(selectedItems, selectedIds, ids);
        final List<Id> newSelectedItems = Lists.newArrayList(selectedIds);
        builder.setMultiChoiceItems(names, selectedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                Log.d(TAG, "Checkbox changed");
                Id id = Id.create(ids[which]);
                if (isChecked) {
                    newSelectedItems.add(id);
                } else {
                    newSelectedItems.remove(id);
                }
            }
        });
        builder.setPositiveButton(R.string.ok_button_title, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "Ok button clicked");
                multiListener.onSelected(newSelectedItems);
            }
        });
        builder.setNegativeButton(R.string.cancel_button_title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Cancel button clicked");
                multiListener.onCancel();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "Dialog cancelled");
                multiListener.onCancel();
            }
        });
        dialog = builder.create();
        return dialog;
    }

    private static void updateSelectedFlags(boolean[] selectedItems, List<Id> selectedIds, long[] allIds) {
        int count = selectedItems.length;
        for (int i = 0; i < count; i++) {
            selectedItems[i] = selectedIds.contains(Id.create(allIds[i]));
        }
    }

    public interface OnEntitySelected {
        void onSelected(long id);
    }

    public interface OnEntitiesSelected {
        List<Id> getInitialSelection();
        void onSelected(List<Id> ids);
        void onCancel();
    }

}
