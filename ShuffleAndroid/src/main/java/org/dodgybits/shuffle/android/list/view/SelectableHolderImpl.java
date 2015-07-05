package org.dodgybits.shuffle.android.list.view;

import android.view.View;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.MultiSelectorBindingHolder;

public class SelectableHolderImpl extends MultiSelectorBindingHolder {

    private boolean mIsSelectable;

    public SelectableHolderImpl(View itemView, MultiSelector multiSelector) {
        super(itemView, multiSelector);
        this.mIsSelectable = false;
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
        this.mIsSelectable = isSelectable;
    }

}
