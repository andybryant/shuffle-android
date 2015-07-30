package org.dodgybits.shuffle.android.editor.activity;

import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.editor.fragment.AbstractEditFragment;
import org.dodgybits.shuffle.android.editor.fragment.EditTaskFragment;

import java.util.List;
import java.util.Set;

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
        new EntityPickerDialogHelper.ContextPickerDialog()
                .show(getSupportFragmentManager(), "contexts");
    }

    public void showProjectPicker() {
        new EntityPickerDialogHelper.ProjectPickerDialog()
                .show(getSupportFragmentManager(), "project");
    }

    @Override
    public List<Id> getInitialSelection() {
        return mEditFragment.getSelectedContextIds();
    }

    @Override
    public void onSelected(List<Id> selectedIds, Set<Id> modifiedIds) {
        mEditFragment.setSelectedContextIds(selectedIds);
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

}
