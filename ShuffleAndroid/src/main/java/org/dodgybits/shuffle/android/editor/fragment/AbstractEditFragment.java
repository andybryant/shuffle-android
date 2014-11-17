package org.dodgybits.shuffle.android.editor.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.encoding.EntityEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;

import roboguice.activity.RoboActionBarActivity;
import roboguice.fragment.RoboFragment;

public abstract class AbstractEditFragment<E extends Entity> extends RoboFragment
        implements View.OnClickListener, View.OnFocusChangeListener {
    private static final String TAG = "AbstractEditFragment";

    @Inject
    protected EntityPersister<E> mPersister;
    @Inject
    protected EntityEncoder<E> mEncoder;

    protected Uri mUri;
    protected boolean mIsNewEntity;
    protected Cursor mCursor;
    protected E mOriginalItem;
    protected Intent mNextIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView+");

        View view = inflater.inflate(getContentViewResId(), null);

        View actionBarButtons = inflater.inflate(R.layout.edit_custom_actionbar,
                new LinearLayout(getActivity()), false);
        View cancelActionView = actionBarButtons.findViewById(R.id.action_cancel);
        cancelActionView.setOnClickListener(this);
        View doneActionView = actionBarButtons.findViewById(R.id.action_done);
        doneActionView.setOnClickListener(this);

        getRoboActionBarActivity().getSupportActionBar().setCustomView(actionBarButtons);

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated+");

        Intent intent = getActivity().getIntent();
        mUri = intent.getData();
        mIsNewEntity = Intent.ACTION_INSERT.equals(intent.getAction());
        loadCursor();
        findViewsAndAddListeners();
        if (mIsNewEntity) {
            updateUIFromExtras(intent.getExtras());
        } else {
            mOriginalItem = mPersister.read(mCursor);
            updateUIFromItem(mOriginalItem);
        }

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getRoboActionBarActivity().getSupportActionBar().setCustomView(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_done:
                save();
                break;

            case R.id.action_cancel:
                revert();
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        // Because we're emulating a ListView, we need to setSelected() for
        // views as they are focused.
        v.setSelected(hasFocus);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        E item = createItemFromUI(false);
        mEncoder.save(outState, item);
    }

    public Intent getNextIntent() {
        return mNextIntent;
    }


    private void restoreInstanceState(Bundle savedInstanceState) {
        E item = mEncoder.restore(savedInstanceState);
        updateUIFromItem(item);
    }

    public void save() {
        // Save or create the contact if needed
        Uri result = null;
        if (isValid()) {
            E item = createItemFromUI(true);
            if (mIsNewEntity) {
                result = mPersister.insert(item);
            } else {
                mPersister.update(item);
                result = mUri;
            }
            showSaveToast();
        }

        if (result == null) {
            getActivity().setResult(Activity.RESULT_CANCELED);
        } else {
            getActivity().setResult(Activity.RESULT_OK, new Intent().setData(result));
        }
        getActivity().finish();

    }

    public void revert() {
        if (mCursor != null) {
            if (!mIsNewEntity) {
                // Put the original item back into the database
                mCursor.close();
                mCursor = null;
                mPersister.update(mOriginalItem);
            }
        }
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    protected RoboActionBarActivity getRoboActionBarActivity() {
        return (RoboActionBarActivity) getActivity();
    }

    protected final void showSaveToast() {
        String text;
        if (mIsNewEntity) {
            text = getResources().getString(R.string.itemCreatedToast,
                    getItemName());
        } else {
            text = getResources().getString(R.string.itemSavedToast,
                    getItemName());
        }
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    protected abstract boolean isValid();

    protected abstract E createItemFromUI(boolean commitValues);

    protected abstract CharSequence getItemName();

    protected abstract int getContentViewResId();

    protected abstract void updateUIFromItem(E item);

    protected abstract void updateUIFromExtras(Bundle extras);

    protected abstract void loadCursor();

    protected abstract void findViewsAndAddListeners();


}
