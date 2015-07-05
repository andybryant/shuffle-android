package org.dodgybits.shuffle.android.list.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.MultiSelectorBindingHolder;
import org.dodgybits.android.shuffle.R;

public class SelectableHolderImpl extends MultiSelectorBindingHolder {

    private MultiSelector mMultiSelector;
    private Drawable mSelectionModeBackgroundDrawable;
    private Drawable mDefaultModeBackgroundDrawable;
    private boolean mIsSelectable;

    public SelectableHolderImpl(View itemView, MultiSelector multiSelector) {
        super(itemView, multiSelector);
        this.mIsSelectable = false;
        this.mMultiSelector = multiSelector;

        this.setSelectionModeBackgroundDrawable(getAccentStateDrawable(itemView.getContext()));
        this.setDefaultModeBackgroundDrawable(itemView.getBackground());
    }

    @Override
    public boolean isActivated() {
        return this.itemView.isActivated();
    }

    @Override
    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    @Override
    public boolean isSelectable() {
        return this.mIsSelectable;
    }

    @Override
    public void setSelectable(boolean isSelectable) {
        boolean changed = isSelectable != this.mIsSelectable;
        this.mIsSelectable = isSelectable;
        if(changed) {
            this.refreshChrome();
        }

    }

    public Drawable getSelectionModeBackgroundDrawable() {
        return this.mSelectionModeBackgroundDrawable;
    }

    public void setSelectionModeBackgroundDrawable(Drawable selectionModeBackgroundDrawable) {
//        this.mSelectionModeBackgroundDrawable = selectionModeBackgroundDrawable;
//        if(this.mIsSelectable) {
//            this.itemView.setBackgroundDrawable(selectionModeBackgroundDrawable);
//        }

    }

    public Drawable getDefaultModeBackgroundDrawable() {
        return this.mDefaultModeBackgroundDrawable;
    }

    public void setDefaultModeBackgroundDrawable(Drawable defaultModeBackgroundDrawable) {
//        this.mDefaultModeBackgroundDrawable = defaultModeBackgroundDrawable;
//        if(!this.mIsSelectable) {
//            this.itemView.setBackgroundDrawable(this.mDefaultModeBackgroundDrawable);
//        }

    }


    private void refreshChrome() {
//        Drawable backgroundDrawable = this.mIsSelectable?this.mSelectionModeBackgroundDrawable:this.mDefaultModeBackgroundDrawable;
//        this.itemView.setBackgroundDrawable(backgroundDrawable);
//        if(backgroundDrawable != null) {
//            backgroundDrawable.jumpToCurrentState();
//        }
    }

    private static Drawable getAccentStateDrawable(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);

        Drawable colorDrawable = new ColorDrawable(typedValue.data);

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_activated}, colorDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, null);

        return stateListDrawable;
    }


}
