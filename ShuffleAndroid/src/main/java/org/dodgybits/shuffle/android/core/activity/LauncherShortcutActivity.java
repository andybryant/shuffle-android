package org.dodgybits.shuffle.android.core.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.fragment.LaunchListFragment;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.IntentUtils;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import roboguice.activity.RoboFragmentActivity;

public class LauncherShortcutActivity extends RoboFragmentActivity {
    public static final int CONTEXT_PICKER_DIALOG = 1;
    public static final int PROJECT_PICKER_DIALOG = 2;

    @Inject
    EntityCache<Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    private LaunchListFragment mFragment;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final String action = getIntent().getAction();

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        if (!Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            // handle old school shortcuts by just going to home screen
            startActivity(new Intent(this, BootstrapActivity.class));
        	finish();
        	return;
        }

        setContentView(R.layout.fragment_container);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, mFragment);
        ft.commit();

        setTitle(R.string.title_shortcut_picker);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        EntityPickerDialogHelper.OnEntitySelected listener;
        final Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.shuffle_icon);

        switch(id) {
            case CONTEXT_PICKER_DIALOG:
                listener = new EntityPickerDialogHelper.OnEntitySelected() {
                    public void onSelected(long id) {
                        Id contextId = Id.create(id);
                        Intent shortcutIntent = IntentUtils.createContextViewIntent(contextId);
                        String name = mContextCache.findById(contextId).getName();
                        returnShortcut(shortcutIntent, name, iconResource);
                    }
                };
                dialog = EntityPickerDialogHelper.createSingleSelectContentPickerDialog(LauncherShortcutActivity.this, listener);
                break;
            case PROJECT_PICKER_DIALOG:
                listener = new EntityPickerDialogHelper.OnEntitySelected() {
                    public void onSelected(long id) {
                        Id projectId = Id.create(id);
                        Intent shortcutIntent = IntentUtils.createProjectViewIntent(projectId);
                        String name = mProjectCache.findById(projectId).getName();
                        returnShortcut(shortcutIntent, name, iconResource);
                    }
                };
                dialog = EntityPickerDialogHelper.createSingleSelectProjectPickerDialog(LauncherShortcutActivity.this, listener);
                break;
        }
        return dialog;
    }    
    
    public void returnShortcut(Intent shortcutIntent,
                                       String name, Intent.ShortcutIconResource iconResource) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(Activity.RESULT_OK, intent);
        finish();
    }


}
