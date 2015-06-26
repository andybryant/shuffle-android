package org.dodgybits.shuffle.android.editor.activity;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.editor.fragment.AbstractEditFragment;
import org.dodgybits.shuffle.android.editor.fragment.EditProjectFragment;

public class EditProjectActivity extends AbstractEditActivity
        implements EntityPickerDialogHelper.OnEntitySelected {
    private static final String TAG = "EditProjectActivity";

    @Inject
    private EditProjectFragment mEditFragment;

    @Override
    protected AbstractEditFragment getFragment() {
        return mEditFragment;
    }

    @Override
    protected void setFragment(AbstractEditFragment fragment) {
        mEditFragment = (EditProjectFragment) fragment;
    }

    public void showContextPicker() {
        new EntityPickerDialogHelper.SingleSelectContextPickerDialog()
                .show(getSupportFragmentManager(), "contexts");
    }

    @Override
    public void onSelected(long id) {
        mEditFragment.setSelectedContext(Id.create(id));
    }
}
