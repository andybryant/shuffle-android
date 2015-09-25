package org.dodgybits.shuffle.android.list.view;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import org.dodgybits.shuffle.android.core.model.Entity;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;

public abstract class AbstractArrayAdapter<VH extends RecyclerView.ViewHolder,E extends Entity>
        extends RecyclerView.Adapter<VH> {

    protected E[] mItems;
    private Cursor mCursor;
    protected EntityPersister<E> mPersister;

    @Override
    public int getItemCount() {
        return (mItems == null) ? 0 : mItems.length;
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
            mItems = mPersister.readAll(mCursor);
            sortItems();
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    protected abstract void sortItems();

    @Override
    public long getItemId(int position) {
        if (this.mItems == null || this.mItems.length <= position) {
            return super.getItemId(position);
        }
        return mItems[position].getLocalId().getId();
    }

}
