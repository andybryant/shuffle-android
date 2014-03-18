package org.dodgybits.shuffle.android.editor.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

public class EditProjectFragment extends AbstractEditFragment<Project> {
    private static final String TAG = "EditProjectFragment";

    private EditText mNameWidget;
    private Spinner mDefaultContextSpinner;
    private RelativeLayout mParallelEntry;
    private TextView mParallelLabel;
    private ImageView mParallelButton;
    private View mDeletedDivider;
    private View mDeletedEntry;
    private CheckBox mDeletedCheckBox;
    private View mActiveEntry;
    private CheckBox mActiveCheckBox;

    private String[] mContextNames;
    private long[] mContextIds;
    private boolean isParallel;

    @Override
    protected int getContentViewResId() {
        return R.layout.project_editor;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.parallel_entry: {
                isParallel = !isParallel;
                updateParallelSection();
                break;
            }

            case R.id.active_entry: {
                mActiveCheckBox.toggle();
                break;
            }

            case R.id.deleted_entry: {
                mDeletedCheckBox.toggle();
                break;
            }

            default:
                super.onClick(v);
                break;
        }
    }

    @Override
    protected void findViewsAndAddListeners() {
        mNameWidget = (EditText) getView().findViewById(R.id.name);
        mDefaultContextSpinner = (Spinner) getView().findViewById(R.id.default_context);
        mParallelEntry = (RelativeLayout) getView().findViewById(R.id.parallel_entry);
        mParallelLabel = (TextView) getView().findViewById(R.id.parallel_label);
        mParallelButton = (ImageView) getView().findViewById(R.id.parallel_icon);
        mDeletedEntry = getView().findViewById(R.id.deleted_entry);
        mDeletedDivider = getView().findViewById(R.id.deleted_divider);
        mActiveEntry = getView().findViewById(R.id.active_entry);
        mActiveCheckBox = (CheckBox) getView().findViewById(R.id.active_entry_checkbox);

        Cursor contactCursor = getActivity().getContentResolver().query(
                ContextProvider.Contexts.CONTENT_URI,
                new String[] {ContextProvider.Contexts._ID, ContextProvider.Contexts.NAME},
                null, null,
                ContextProvider.Contexts.NAME + " ASC");
        int size = contactCursor.getCount() + 1;
        mContextIds = new long[size];
        mContextIds[0] = 0;
        mContextNames = new String[size];
        mContextNames[0] = getText(R.string.none_empty).toString();
        for (int i = 1; i < size; i++) {
            contactCursor.moveToNext();
            mContextIds[i] = contactCursor.getLong(0);
            mContextNames[i] = contactCursor.getString(1);
        }
        contactCursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, mContextNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDefaultContextSpinner.setAdapter(adapter);

        mParallelEntry.setOnClickListener(this);

        mActiveEntry.setOnClickListener(this);
        mActiveEntry.setOnFocusChangeListener(this);

        mDeletedEntry.setOnClickListener(this);
        mDeletedEntry.setOnFocusChangeListener(this);
        mDeletedCheckBox = (CheckBox) mDeletedEntry.findViewById(R.id.deleted_entry_checkbox);
    }

    @Override
    protected void loadCursor() {
        if (mUri != null && !mIsNewEntity) {
            mCursor = getActivity().managedQuery(mUri, ProjectProvider.Projects.FULL_PROJECTION, null, null, null);
            if (mCursor == null || mCursor.getCount() == 0) {
                // The cursor is empty. This can happen if the event was deleted.
                getActivity().finish();
            }
            mCursor.moveToFirst();
        }
    }

    @Override
    protected boolean isValid() {
        String name = mNameWidget.getText().toString();
        return !TextUtils.isEmpty(name);
    }

    @Override
    protected Project createItemFromUI(boolean commitValues) {
        boolean changed = false;
        Project.Builder builder = Project.newBuilder();
        if (mOriginalItem != null) {
            builder.mergeFrom(mOriginalItem);
        }

        String name = mNameWidget.getText().toString();
        boolean active = mActiveCheckBox.isChecked();
        boolean deleted = mDeletedCheckBox.isChecked();

        ProjectChangeSet changeSet = builder.getChangeSet();

        if (!ObjectUtils.equals(name, builder.getName())) {
            builder.setName(name);
            changeSet.nameChanged();
            changed = true;
        }

        Id defaultContextId = Id.NONE;
        int selectedItemPosition = mDefaultContextSpinner.getSelectedItemPosition();
        if (selectedItemPosition > 0) {
            defaultContextId = Id.create(mContextIds[selectedItemPosition]);
        }
        if (!ObjectUtils.equals(defaultContextId, builder.getDefaultContextId())) {
            builder.setDefaultContextId(defaultContextId);
            changeSet.defaultContextChanged();
            changed = true;
        }
        if (isParallel != builder.isParallel()) {
            builder.setParallel(isParallel);
            changeSet.parallelChanged();
            changed = true;
        }
        if (active != builder.isActive()) {
            builder.setActive(active);
            changeSet.activeChanged();
            changed = true;
        }
        if (deleted != builder.isDeleted()) {
            builder.setDeleted(deleted);
            changeSet.deleteChanged();
            changed = true;
        }

        builder.setModifiedDate(System.currentTimeMillis());
        builder.setChangeSet(changeSet);

        if (commitValues && changed) {
            Log.d(TAG, "Project updated - schedule sync");
            SyncUtils.scheduleSync(getActivity(), LOCAL_CHANGE_SOURCE);
        }

        return builder.build();
    }

    @Override
    protected void updateUIFromExtras(Bundle savedState) {
        mDeletedEntry.setVisibility(View.GONE);
        mDeletedDivider.setVisibility(View.GONE);
        mDeletedCheckBox.setChecked(false);
        mActiveCheckBox.setChecked(true);

        updateParallelSection();
    }

    @Override
    protected void updateUIFromItem(Project project) {
        mNameWidget.setTextKeepState(project.getName());
        Id defaultContextId = project.getDefaultContextId();
        if (defaultContextId.isInitialised()) {
            for (int i = 1; i < mContextIds.length; i++) {
                if (mContextIds[i] == defaultContextId.getId()) {
                    mDefaultContextSpinner.setSelection(i);
                    break;
                }
            }
        } else {
            mDefaultContextSpinner.setSelection(0);
        }

        isParallel = project.isParallel();
        updateParallelSection();

        mActiveCheckBox.setChecked(project.isActive());

        mDeletedEntry.setVisibility(project.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedDivider.setVisibility(project.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedCheckBox.setChecked(project.isDeleted());

        if (mOriginalItem == null) {
            mOriginalItem = project;
        }
    }

    @Override
    protected CharSequence getItemName() {
        return getString(R.string.project_name);
    }

    private void updateParallelSection() {
        if (isParallel) {
            mParallelLabel.setText(R.string.parallel_title);
            mParallelButton.setImageResource(R.drawable.parallel);
        } else {
            mParallelLabel.setText(R.string.sequence_title);
            mParallelButton.setImageResource(R.drawable.sequence);
        }
    }


}
