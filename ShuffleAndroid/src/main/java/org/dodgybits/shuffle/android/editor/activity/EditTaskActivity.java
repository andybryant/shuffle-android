package org.dodgybits.shuffle.android.editor.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.editor.fragment.AbstractEditFragment;
import org.dodgybits.shuffle.android.editor.fragment.EditTaskFragment;

import java.util.List;

public class EditTaskActivity extends AbstractEditActivity
        implements EntityPickerDialogHelper.OnEntitiesSelected, EntityPickerDialogHelper.OnEntitySelected {
    private static final String TAG = "EditTaskActivity";

    @Inject
    private EditTaskFragment mEditFragment;

    @Override
    protected AbstractEditFragment getFragment() {
        return mEditFragment;
    }

    @Override
    protected void setFragment(AbstractEditFragment fragment) {
        mEditFragment = (EditTaskFragment) fragment;
    }

    public void showContextPicker() {
        new ContextPickerDialog().show(getSupportFragmentManager(), "contexts");
    }

    public void showProjectPicker() {
        new ProjectPickerDialog().show(getSupportFragmentManager(), "project");
    }

    @Override
    public List<Id> getInitialSelection() {
        return mEditFragment.getSelectedContextIds();
    }

    @Override
    public void onSelected(List<Id> ids) {
        mEditFragment.setSelectedContextIds(ids);
    }

    @Override
    public void onCancel() {
        // nothing to do
    }

    @Override
    public void onSelected(long id) {
        if (id == EntityPickerDialogHelper.ADD_NEW_ID) {
            mEditFragment.triggerAddProject();
        } else {
            mEditFragment.setSelectedProject(Id.create(id));
        }
    }


    public static class ContextPickerDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return EntityPickerDialogHelper.createMultiSelectContextPickerDialog(getActivity());
        }
    }

    public static class ProjectPickerDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return EntityPickerDialogHelper.createSingleSelectProjectPickerDialog(getActivity(), true, true);
        }

    }
}
