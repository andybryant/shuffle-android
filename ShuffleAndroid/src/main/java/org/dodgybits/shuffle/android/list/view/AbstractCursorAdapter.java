package org.dodgybits.shuffle.android.list.view;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import com.bignerdranch.android.multiselector.SelectableHolder;
import com.bignerdranch.android.multiselector.SwappingHolder;

public abstract class AbstractCursorAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

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

    @Override
    public long getItemId(int position) {
        if (dataCursor.isClosed()) {
            return super.getItemId(position);
        }
        dataCursor.moveToPosition(position);
        return dataCursor.getLong(0);
    }

    public int getItemPosition(long id) {
        int position = -1;
        int i = 0;
        if (dataCursor.moveToFirst()) {
            do {
                if (dataCursor.getLong(0) == id) {
                    position = i;
                    break;
                }
                i++;
            } while (dataCursor.moveToNext());
        }
        return position;
    }

}
