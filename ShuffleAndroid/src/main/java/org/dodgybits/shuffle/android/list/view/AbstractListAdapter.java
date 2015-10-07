package org.dodgybits.shuffle.android.list.view;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractListAdapter<VH extends RecyclerView.ViewHolder,E extends Entity>
        extends RecyclerView.Adapter<VH> {

    protected List<E> mItems;
    private Cursor mCursor;
    protected EntityPersister<E> mPersister;

    @Override
    public int getItemCount() {
        return (mItems == null) ? 0 : mItems.size();
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        this.mCursor = cursor;
        if (cursor != null) {
            mItems = new ArrayList<>(Arrays.asList(mPersister.readAll(mCursor)));
            sortItems();
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    protected abstract void sortItems();

    @Override
    public long getItemId(int position) {
        if (this.mItems == null || this.mItems.size() <= position) {
            return super.getItemId(position);
        }
        return mItems.get(position).getLocalId().getId();
    }

}
