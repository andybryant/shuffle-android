package org.dodgybits.shuffle.android.list.view;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import com.bignerdranch.android.multiselector.SwappingHolder;

public abstract class AbstractCursorAdapter<T extends SwappingHolder> extends RecyclerView.Adapter<T> {

    Cursor dataCursor;

    @Override
    public int getItemCount() {
        return (dataCursor == null) ? 0 : dataCursor.getCount();
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (dataCursor == cursor) {
            return null;
        }
        Cursor oldCursor = dataCursor;
        this.dataCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

}
