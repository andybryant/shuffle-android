package org.dodgybits.shuffle.android.editor.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.editor.fragment.AbstractEditFragment;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;

public abstract class AbstractEditActivity extends RoboAppCompatActivity {
    private static final String TAG = "AbstractEditActivity";

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "onCreate+");

        setContentView(R.layout.fragment_container);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME|
                        ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);

        AbstractEditFragment currentFragment = (AbstractEditFragment) getSupportFragmentManager().
                findFragmentById(R.id.fragment_container);
        if (currentFragment == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, getFragment());
            ft.show(getFragment());
            ft.commit();
        } else {
            setFragment(currentFragment);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragment() != null) {
            getFragment().save();
        }

        super.onBackPressed();
    }

    @Override
    public void finish() {
        if (getFragment() != null && getFragment().getNextIntent() != null) {
            startActivity(getFragment().getNextIntent());
        }
        super.finish();
    }


    protected abstract AbstractEditFragment getFragment();
    protected abstract void setFragment(AbstractEditFragment fragment);


}
