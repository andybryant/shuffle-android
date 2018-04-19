package org.dodgybits.shuffle.android.preference.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.IntegrationSettings;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.android.server.sync.event.ResetSyncSettingsEvent;
import org.dodgybits.shuffle.android.server.sync.listener.SyncListener;

import roboguice.event.EventManager;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

import static android.app.Activity.RESULT_OK;
import static java.util.Collections.singletonList;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.MANUAL_SOURCE;

public class PreferencesAppEngineSynchronizationFragment extends RoboFragment {
    private static final String TAG = "PrefAppEngSyncFrg";

    private static final int RC_SIGN_IN = 123;

    @InjectView(R.id.intro_message)
    private TextView introTextView;

    @InjectView(R.id.link_account)
    private Button linkAccountButton;

    @InjectView(R.id.logged_in_message)
    private TextView loggedInTextView;

    @InjectView(R.id.logout)
    private Button logoutButton;

    @InjectView(R.id.sync_now)
    private Button syncNowButton;

    @InjectView(R.id.last_sync_message)
    private TextView lastSyncTextView;

    @Inject
    private EventManager eventManager;

    @Inject
    private SyncListener syncListener;

    @Inject
    private IntegrationSettings integrationSettings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView+");
        return inflater.inflate(R.layout.preferences_appengine_sync, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateViewsOnSyncAccountSet();
    }

    public void onSelectAccountClicked(View view) {
        triggerLogin();
    }

    public void onLogoutClicked(View view) {
        Preferences.getEditor(getActivity())
                .putBoolean(Preferences.SYNC_ENABLED, false)
                .commit();
        eventManager.fire(new ResetSyncSettingsEvent());
        updateViewsOnSyncAccountSet();
    }

    public void onSyncNowClicked(View view) {
        SyncUtils.scheduleSync(getActivity(), MANUAL_SOURCE);
    }

    public void triggerLogin() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(singletonList(
                                new AuthUI.IdpConfig.GoogleBuilder().build()))
                        .build(),
                RC_SIGN_IN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = Preferences.getEditor(getActivity());
                editor.putBoolean(Preferences.SYNC_ENABLED, true);
                editor.putString(Preferences.SYNC_ACCOUNT, response.getEmail());
                editor.putString(Preferences.SYNC_AUTH_TOKEN, response.getIdpToken());
                editor.commit();

                updateViewsOnSyncAccountSet();
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    // no internet connection
                    return;
                }

                Log.e(TAG, "Sign-in error: " + response.getErrorCode());
            }
        }
    }

    private void updateViewsOnSyncAccountSet() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        final boolean syncEnabled = currentUser != null;
        introTextView.setVisibility(syncEnabled ? View.GONE : View.VISIBLE);
        linkAccountButton.setVisibility(syncEnabled ? View.GONE : View.VISIBLE);
        loggedInTextView.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        loggedInTextView.setText(getString(R.string.sync_selected_account, currentUser.getDisplayName()));
        logoutButton.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        syncNowButton.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        lastSyncTextView.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        long lastSyncDate = Preferences.getLastSyncLocalDate(getActivity());
        if (lastSyncDate == 0L) {
            lastSyncTextView.setText(R.string.no_previous_sync);
        } else {
            CharSequence syncDate = DateUtils.getRelativeTimeSpanString(getActivity(), lastSyncDate, false);
            lastSyncTextView.setText(getString(R.string.last_sync_title, syncDate));
        }
    }

}
