package org.dodgybits.shuffle.android.editor.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.editor.activity.EditProjectActivity;
import org.dodgybits.shuffle.android.editor.activity.EditTaskActivity;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

public class EditProjectFragment extends AbstractEditFragment<Project> {
    private static final String TAG = "EditProjectFragment";

    private EditText mNameWidget;
    private Button mDefaultContextButton;
    private ViewGroup mParallelEntry;
    private TextView mParallelLabel;
    private TextView mParallelSubtitle;
    private ImageView mParallelButton;

    private View mActiveEntry;
    private TextView mActiveLabel;
    private CompoundButton mActiveCheckBox;
    private ImageView mActiveIcon;

    private Button mDeleteButton;
    private boolean isParallel;
    private Id mDefaultContextId;

    @Inject
    private EntityCache<Context> mContextCache;

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

            case R.id.active_row: {
                mActiveCheckBox.toggle();
                updateActiveState();
                break;
            }

            case R.id.active_entry_checkbox: {
                super.onClick(v);
                updateActiveState();
                break;
            }

            case R.id.delete_button: {
                mEventManager.fire(new UpdateProjectDeletedEvent(
                        mOriginalItem.getLocalId(), !mOriginalItem.isDeleted()));
                getActivity().finish();
                break;
            }

            case R.id.default_context: {
                showContextPicker();
                break;
            }

            default:
                super.onClick(v);
                break;
        }
    }

    private void showContextPicker() {
        ((EditProjectActivity)getActivity()).showContextPicker();
    }


    private void updateActiveState() {
        mActiveIcon.setImageResource(
                mActiveCheckBox.isChecked() ? R.drawable.ic_visibility_black_24dp : R.drawable.ic_visibility_off_black_24dp);
        mActiveLabel.setText(mActiveCheckBox.isChecked() ? R.string.active : R.string.inactive);
    }

    @Override
    protected void findViewsAndAddListeners() {
        mNameWidget = (EditText) getView().findViewById(R.id.name);
        mDefaultContextButton = (Button) getView().findViewById(R.id.default_context);
        mDefaultContextButton.setOnClickListener(this);
        mParallelEntry = (ViewGroup) getView().findViewById(R.id.parallel_entry);
        mParallelLabel = (TextView) getView().findViewById(R.id.parallel_label);
        mParallelSubtitle = (TextView) getView().findViewById(R.id.parallel_subtitle);
        mParallelButton = (ImageView) getView().findViewById(R.id.parallel_icon);

        mDeleteButton = (Button) getView().findViewById(R.id.delete_button);
        mDeleteButton.setOnClickListener(this);

        mActiveEntry = getView().findViewById(R.id.active_row);
        mActiveEntry.setOnClickListener(this);
        mActiveEntry.setOnFocusChangeListener(this);
        mActiveLabel = (TextView) mActiveEntry.findViewById(R.id.active_label);
        mActiveCheckBox = (SwitchCompat) mActiveEntry.findViewById(R.id.active_entry_checkbox);
        mActiveCheckBox.setOnClickListener(this);
        mActiveIcon = (ImageView) mActiveEntry.findViewById(R.id.active_icon);

        mParallelEntry.setOnClickListener(this);
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

        ProjectChangeSet changeSet = builder.getChangeSet();

        if (!ObjectUtils.equals(name, builder.getName())) {
            builder.setName(name);
            changeSet.nameChanged();
            changed = true;
        }

        if (!ObjectUtils.equals(mDefaultContextId, builder.getDefaultContextId())) {
            builder.setDefaultContextId(mDefaultContextId);
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
        mActiveCheckBox.setChecked(true);
        mDeleteButton.setVisibility(View.GONE);

        updateParallelSection();
        updateDefaultContext();
    }

    @Override
    protected void updateUIFromItem(Project project) {
        mNameWidget.setTextKeepState(project.getName());
        mDefaultContextId = project.getDefaultContextId();
        updateDefaultContext();

        isParallel = project.isParallel();
        updateParallelSection();

        mActiveCheckBox.setChecked(project.isActive());
        mDeleteButton.setText(project.isDeleted() ? R.string.restore_button_title : R.string.delete_completed_button_title);

        if (mOriginalItem == null) {
            mOriginalItem = project;
        }
    }

    @Override
    protected CharSequence getItemName() {
        return getString(R.string.project_name);
    }

    public void setSelectedContext(Id id) {
        mDefaultContextId = id;
        updateDefaultContext();
    }

    private void updateDefaultContext() {
        Context context = mContextCache.findById(mDefaultContextId);
        if (context == null) {
            mDefaultContextButton.setText(R.string.default_context_button);
        } else {
            mDefaultContextButton.setText(getString(R.string.default_context_title, context.getName()));
        }
    }

    private void updateParallelSection() {
        if (isParallel) {
            mParallelLabel.setText(R.string.parallel_title);
            mParallelSubtitle.setText(R.string.parallel_subtitle);
            mParallelButton.setImageResource(R.drawable.parallel);
        } else {
            mParallelLabel.setText(R.string.sequence_title);
            mParallelSubtitle.setText(R.string.sequence_subtitle);
            mParallelButton.setImageResource(R.drawable.sequence);
        }
    }


}
