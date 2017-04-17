package org.dodgybits.shuffle.android.preference.fragment;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.preference.activity.PreferencesAppEngineSynchronizationActivity;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.IntegrationSettings;
import org.dodgybits.shuffle.android.server.sync.AuthTokenRetriever;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.android.server.sync.event.ResetSyncSettingsEvent;
import org.dodgybits.shuffle.android.server.sync.listener.SyncListener;
import roboguice.event.EventManager;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.MANUAL_SOURCE;

public class PreferencesAppEngineSynchronizationFragment extends RoboFragment {
    private static final String TAG = "PrefAppEngSyncAct";

    public static final String GOOGLE_ACCOUNT = "com.google";
    private static final int MY_PERMISSIONS_GET_ACCOUNTS = 234;

    @InjectView(R.id.intro_message)
    private TextView mIntroTextView;

    @InjectView(R.id.select_account)
    private Button mSelectAccountButton;

    @InjectView(R.id.logged_in_message)
    private TextView mLoggedInTextView;

    @InjectView(R.id.logout)
    private Button mLogoutButton;

    @InjectView(R.id.sync_now)
    private Button mSyncNowButton;

    @InjectView(R.id.last_sync_message)
    private TextView mLastSyncTextView;

    @Inject
    private EventManager mEventManager;

    @Inject
    private SyncListener mSyncListener;

    @Inject
    private IntegrationSettings integrationSettings;

    @Inject
    private AuthTokenRetriever authTokenRetriever;

    private Account mSelectedAccount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView+");
        return inflater.inflate(R.layout.preferences_appengine_sync, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupScreen();
    }

    public void onSelectAccountClicked(View view) {
        requestAccounts();
    }

    public void onLogoutClicked(View view) {
        Preferences.getEditor(getActivity())
                .putBoolean(Preferences.SYNC_ENABLED, false)
                .commit();
        mEventManager.fire(new ResetSyncSettingsEvent());
        updateViewsOnSyncAccountSet();
    }

    public void onSyncNowClicked(View view) {
        SyncUtils.scheduleSync(getActivity(), MANUAL_SOURCE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_GET_ACCOUNTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showAccountsDialog();
                }
            }
        }
    }

    private void requestAccounts() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_GET_ACCOUNTS);
        } else {
            showAccountsDialog();
        }
    }

    private void showAccountsDialog() {
        getActivity().showDialog(PreferencesAppEngineSynchronizationActivity.ACCOUNTS_DIALOG);
    }

    public Dialog createAccountsDialog() throws SecurityException {
        AccountManager manager = AccountManager.get(getActivity());
        final Account[] accounts = manager.getAccountsByType(GOOGLE_ACCOUNT);

        final int numAccounts = accounts.length;

        if (numAccounts == 0) {
            return createNoAccountsDialog();
        }

        final CharSequence[] items = new CharSequence[numAccounts];

        String accountName = Preferences.getSyncAccount(getActivity());

        int selectedIndex = -1;
        mSelectedAccount = null;
        for (int i=0; i < numAccounts; i++)
        {
            final String name = accounts[i].name;
            if (name.equals(accountName)) {
                selectedIndex = i;
                mSelectedAccount = accounts[i];
            }
            items[i] = name;
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
            .setTitle(R.string.select_account_button_title)
            .setSingleChoiceItems(items, selectedIndex,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    mSelectedAccount = accounts[item];
                }
            })
            .setPositiveButton(R.string.ok_button_title,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mSelectedAccount != null) {
                        final Account account = mSelectedAccount;
                        final FragmentActivity activity = getActivity();
                        String oldAccountName = Preferences.getSyncAccount(activity);
                        SharedPreferences.Editor editor = Preferences.getEditor(activity);
                        editor.putBoolean(Preferences.SYNC_ENABLED, true);
                        boolean accountChanged = !oldAccountName.equals(account.name);
                        if (accountChanged) {
                            Log.i(TAG, "Switching from account " + oldAccountName +
                                    " to " + account.name);
                            editor.putString(Preferences.SYNC_ACCOUNT, account.name);
                            editor.remove(Preferences.SYNC_AUTH_TOKEN);
                        }
                        editor.commit();
                        updateViewsOnSyncAccountSet();
                        if (accountChanged) {
                            // fetch token now so if permission is required, use
                            // will be able to respond
                            authTokenRetriever.retrieveToken();
                        }
                    }
                }
            });

        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog createNoAccountsDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.no_sync_accounts)
                        .setNegativeButton(R.string.cancel_button_title,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                        .setPositiveButton(R.string.ok_button_title,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                                        getActivity().startActivity(intent);
                                    }
                                });
        return builder.create();
    }


    private void setupScreen() {
        updateViewsOnSyncAccountSet();
    }

    private void updateViewsOnSyncAccountSet() {
        String syncAccount = Preferences.getSyncAccount(getActivity());
        final boolean syncEnabled = Preferences.isSyncEnabled(getActivity());
        mIntroTextView.setVisibility(syncEnabled ? View.GONE : View.VISIBLE);
        mSelectAccountButton.setVisibility(syncEnabled ? View.GONE : View.VISIBLE);
        mLoggedInTextView.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        mLoggedInTextView.setText(getString(R.string.sync_selected_account, syncAccount));
        mLogoutButton.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        mSyncNowButton.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        mLastSyncTextView.setVisibility(syncEnabled ? View.VISIBLE : View.GONE);
        long lastSyncDate = Preferences.getLastSyncLocalDate(getActivity());
        if (lastSyncDate == 0L) {
            mLastSyncTextView.setText(R.string.no_previous_sync);
        } else {
            CharSequence syncDate = DateUtils.getRelativeTimeSpanString(getActivity(), lastSyncDate, false);
            mLastSyncTextView.setText(getString(R.string.last_sync_title, syncDate));
        }
    }

}
