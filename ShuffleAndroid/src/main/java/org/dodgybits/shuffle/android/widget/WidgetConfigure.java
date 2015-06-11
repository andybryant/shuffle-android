package org.dodgybits.shuffle.android.widget;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.activity.RoboFragmentActivity;

/**
 * The configuration screen for the DarkWidgetProvider widget.
 */
public class WidgetConfigure extends RoboFragmentActivity {
    private static final String TAG = "WidgetConfigure";

    static final int CONTEXT_PICKER_DIALOG = 1;
    static final int PROJECT_PICKER_DIALOG = 2;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Inject
    private WidgetConfigureListFragment mFragment;

    public WidgetConfigure() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.fragment_container);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, mFragment);
        ft.commit();


        // Find the widget id from the intent.
        final Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        setTitle(R.string.title_widget_picker);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        EntityPickerDialogHelper.OnEntitySelected listener;
        switch(id) {
            case CONTEXT_PICKER_DIALOG:
                listener = new EntityPickerDialogHelper.OnEntitySelected() {
                    public void onSelected(long id) {
                        String queryKey = Preferences.getWidgetQueryKey(getAppWidgetId());
                        String contextKey = Preferences.getWidgetContextIdKey(getAppWidgetId());
                        SharedPreferences.Editor editor = Preferences.getEditor(WidgetConfigure.this);
                        editor.putString(queryKey, ListQuery.context.name());
                        editor.putLong(contextKey, id);
                        editor.commit();
                        confirmSelection();
                    }
                };
                dialog = EntityPickerDialogHelper.createSingleSelectContentPickerDialog(WidgetConfigure.this, listener);
                break;
            case PROJECT_PICKER_DIALOG:
                listener = new EntityPickerDialogHelper.OnEntitySelected() {
                    public void onSelected(long id) {
                        String queryKey = Preferences.getWidgetQueryKey(getAppWidgetId());
                        String projectKey = Preferences.getWidgetProjectIdKey(getAppWidgetId());
                        SharedPreferences.Editor editor = Preferences.getEditor(WidgetConfigure.this);
                        editor.putString(queryKey, ListQuery.project.name());
                        editor.putLong(projectKey, id);
                        editor.commit();
                        confirmSelection();
                    }
                };
                dialog = EntityPickerDialogHelper.createSingleSelectProjectPickerDialog(WidgetConfigure.this, listener, false, false);
                break;
        }
        return dialog;
    }

    public int getAppWidgetId() {
        return mAppWidgetId;
    }

    void confirmSelection() {
        // let widget update itself (suggested approach of calling updateAppWidget did nothing)
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {getAppWidgetId()});
        intent.setPackage(getPackageName());
        sendBroadcast(intent);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getAppWidgetId());
        setResult(WidgetConfigure.RESULT_OK, resultValue);
        finish();
    }
}



