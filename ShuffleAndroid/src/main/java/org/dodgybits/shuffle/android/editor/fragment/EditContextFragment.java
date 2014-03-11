package org.dodgybits.shuffle.android.editor.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.DrawableUtils;
import org.dodgybits.shuffle.android.editor.activity.ColourPickerActivity;
import org.dodgybits.shuffle.android.editor.activity.IconPickerActivity;
import org.dodgybits.shuffle.android.list.view.context.ContextListItem;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.sync.model.ContextChangeSet;

public class EditContextFragment extends AbstractEditFragment<Context>
        implements TextWatcher {
    private static final String TAG = "EditContextFragment";

    private static final int COLOUR_PICKER = 0;
    private static final int ICON_PICKER = 1;

    private int mColourIndex;
    private ContextIcon mIcon;

    private EditText mNameWidget;
    private TextView mColourWidget;
    private ImageView mIconWidget;
    private TextView mIconNoneWidget;
    private ImageButton mClearIconButton;
    private View mDeletedDivider;
    private View mDeletedEntry;
    private CheckBox mDeletedCheckBox;
    private View mActiveEntry;
    private CheckBox mActiveCheckBox;
    private ContextListItem mContextPreview;


    @Override
    protected int getContentViewResId() {
        return R.layout.context_editor;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.colour_entry: {
                // Launch activity to pick colour
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ColourPickerActivity.TYPE);
                startActivityForResult(intent, COLOUR_PICKER);
                break;
            }

            case R.id.icon_entry: {
                // Launch activity to pick icon
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(IconPickerActivity.TYPE);
                startActivityForResult(intent, ICON_PICKER);
                break;
            }

            case R.id.icon_clear_button: {
                mIcon = ContextIcon.NONE;
                displayIcon();
                updatePreview();
                break;
            }

            case R.id.active_entry: {
                mActiveCheckBox.toggle();
                updatePreview();
                break;
            }

            case R.id.deleted_entry: {
                mDeletedCheckBox.toggle();
                updatePreview();
                break;
            }

            default:
                super.onClick(v);
                break;

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case COLOUR_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mColourIndex = Integer.parseInt(data.getStringExtra("colour"));
                        displayColour();
                        updatePreview();
                    }
                }
                break;
            case ICON_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        String iconName = data.getStringExtra("iconName");
                        mIcon = ContextIcon.createIcon(iconName, getResources());
                        displayIcon();
                        updatePreview();
                    }
                }
                break;
            default:
                Log.e(TAG, "Unknown requestCode: " + requestCode);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updatePreview();
    }

    @Override
    protected void findViewsAndAddListeners() {
        mNameWidget = (EditText) getView().findViewById(R.id.name);
        mColourWidget = (TextView) getView().findViewById(R.id.colour_display);
        mIconWidget = (ImageView) getView().findViewById(R.id.icon_display);
        mIconNoneWidget = (TextView) getView().findViewById(R.id.icon_none);
        mClearIconButton = (ImageButton) getView().findViewById(R.id.icon_clear_button);
        mDeletedEntry = getView().findViewById(R.id.deleted_entry);
        mDeletedDivider = getView().findViewById(R.id.deleted_divider);
        mActiveEntry = getView().findViewById(R.id.active_entry);
        mActiveCheckBox = (CheckBox) getView().findViewById(R.id.active_entry_checkbox);
        mContextPreview = (ContextListItem) getView().findViewById(R.id.context_preview);

        mNameWidget.addTextChangedListener(this);

        mColourIndex = 0;
        mIcon = ContextIcon.NONE;

        View colourEntry = getView().findViewById(R.id.colour_entry);
        colourEntry.setOnClickListener(this);
        colourEntry.setOnFocusChangeListener(this);

        View iconEntry = getView().findViewById(R.id.icon_entry);
        iconEntry.setOnClickListener(this);
        iconEntry.setOnFocusChangeListener(this);

        mClearIconButton.setOnClickListener(this);
        mClearIconButton.setOnFocusChangeListener(this);

        mActiveEntry.setOnClickListener(this);
        mActiveEntry.setOnFocusChangeListener(this);

        mDeletedEntry.setOnClickListener(this);
        mDeletedEntry.setOnFocusChangeListener(this);
        mDeletedCheckBox = (CheckBox) mDeletedEntry.findViewById(R.id.deleted_entry_checkbox);
    }

    @Override
    protected void loadCursor() {
        if (mUri != null && !mIsNewEntity) {
            mCursor = getActivity().managedQuery(mUri, ContextProvider.Contexts.FULL_PROJECTION, null, null, null);
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
    protected Context createItemFromUI(boolean commitValues) {
        Context.Builder builder = Context.newBuilder();
        if (mOriginalItem != null) {
            builder.mergeFrom(mOriginalItem);
        }

        String name = mNameWidget.getText().toString();
        String iconName = mIcon.iconName;
        boolean active = mActiveCheckBox.isChecked();
        boolean deleted = mDeletedCheckBox.isChecked();

        ContextChangeSet changeSet = builder.getChangeSet();

        if (!ObjectUtils.equals(name, builder.getName())) {
            builder.setName(name);
            changeSet.nameChanged();
        }
        if (mColourIndex != builder.getColourIndex()) {
            builder.setColourIndex(mColourIndex);
            changeSet.colourChanged();
        }
        if (!ObjectUtils.equals(iconName, builder.getIconName())) {
            builder.setIconName(iconName);
            changeSet.iconChanged();
        }
        if (active != builder.isActive()) {
            builder.setActive(active);
            changeSet.activeChanged();
        }
        if (deleted != builder.isDeleted()) {
            builder.setDeleted(deleted);
            changeSet.deleteChanged();
        }
        builder.setModifiedDate(System.currentTimeMillis());
        builder.setChangeSet(changeSet);
        return builder.build();
    }

    @Override
    protected void updateUIFromExtras(Bundle savedState) {
        mDeletedEntry.setVisibility(View.GONE);
        mDeletedDivider.setVisibility(View.GONE);
        mDeletedCheckBox.setChecked(false);
        mActiveCheckBox.setChecked(true);

        displayIcon();
        displayColour();
        updatePreview();
    }

    @Override
    protected void updateUIFromItem(Context context) {
        mColourIndex = context.getColourIndex();
        displayColour();

        final String iconName = context.getIconName();
        mIcon = ContextIcon.createIcon(iconName, getResources());
        displayIcon();

        mActiveCheckBox.setChecked(context.isActive());

        mDeletedEntry.setVisibility(context.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedDivider.setVisibility(context.isDeleted() ? View.VISIBLE : View.GONE);
        mDeletedCheckBox.setChecked(context.isDeleted());

        mNameWidget.setTextKeepState(context.getName());

        if (mOriginalItem == null) {
            mOriginalItem = context;
        }
    }

    @Override
    protected CharSequence getItemName() {
        return getString(R.string.context_name);
    }

    private void displayColour() {
        int bgColour = TextColours.getInstance(getActivity()).getBackgroundColour(mColourIndex);
        GradientDrawable drawable = DrawableUtils.createGradient(bgColour, GradientDrawable.Orientation.TL_BR);
        int radius = getResources().getDimensionPixelSize(R.dimen.context_large_corner_radius);
        drawable.setCornerRadius(radius);
        mColourWidget.setBackgroundDrawable(drawable);
    }

    private void displayIcon() {
        if (mIcon == ContextIcon.NONE) {
            mIconNoneWidget.setVisibility(View.VISIBLE);
            mIconWidget.setVisibility(View.GONE);
            mClearIconButton.setEnabled(false);
        } else {
            mIconNoneWidget.setVisibility(View.GONE);
            mIconWidget.setImageResource(mIcon.largeIconId);
            mIconWidget.setVisibility(View.VISIBLE);
            mClearIconButton.setEnabled(true);
        }
    }

    private void updatePreview() {
        mContextPreview.updateView(createItemFromUI(false));
    }
    

}
