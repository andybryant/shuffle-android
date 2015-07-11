package org.dodgybits.shuffle.android.core.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

public abstract class AbstractSwipeItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {

    private int mPositiveColor;
    private int mNegativeColor;
    private Bitmap mPositiveIcon;
    private Bitmap mNegativeIcon;

    public AbstractSwipeItemTouchHelperCallback(int dragDirs, int swipeDirs) {
        super(dragDirs, swipeDirs);
    }

    public void setPositiveColor(int positiveColor) {
        mPositiveColor = positiveColor;
    }

    public void setNegativeColor(int negativeColor) {
        mNegativeColor = negativeColor;
    }

    public void setPositiveIcon(Bitmap positiveIcon) {
        mPositiveIcon = positiveIcon;
    }

    public void setNegativeIcon(Bitmap negativeIcon) {
        mNegativeIcon = negativeIcon;
    }

    @Override
    public void onChildDraw(
            Canvas c, RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Get RecyclerView item from the ViewHolder
            View itemView = viewHolder.itemView;

            Paint p = new Paint();
            if (dX > 0 && mNegativeIcon != null) {
                Bitmap icon = mNegativeIcon;
                int y = itemView.getTop() + (itemView.getHeight() - icon.getHeight()) / 2;

                /* Set your color for positive displacement */
                p.setColor(mNegativeColor);

                // Draw Rect with varying right side, equal to displacement dX
                c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                        (float) itemView.getBottom(), p);

                int hOffset = icon.getWidth();
                int x = itemView.getLeft() + hOffset;
                if (dX > x) {

                    Rect destRect = new Rect(x, y,
                            (int)Math.min(x + icon.getScaledWidth(c), dX),
                            y + icon.getScaledHeight(c));
                    Rect srcRect = new Rect(0, 0, destRect.width(), destRect.height());
                    c.drawBitmap(icon, srcRect, destRect, null);
                }
            } else if (dX < 0 && mPositiveIcon != null ) {
                Bitmap icon = mPositiveIcon;
                int y = itemView.getTop() + (itemView.getHeight() - icon.getHeight()) / 2;

                        /* Set your color for negative displacement */
                p.setColor(mPositiveColor);

                // Draw Rect with varying left side, equal to the item's right side plus negative displacement dX
                c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                        (float) itemView.getRight(), (float) itemView.getBottom(), p);

                int hOffset = 2 * icon.getWidth();
                int x = itemView.getRight() - hOffset;
                if (itemView.getRight() + dX < x + icon.getScaledWidth(c)) {
                    Rect destRect = new Rect(
                            Math.max(x, (int) (itemView.getRight() + dX)), y,
                            x + icon.getScaledWidth(c),
                            y + icon.getScaledHeight(c));
                    Rect srcRect = new Rect(icon.getScaledWidth(c) - destRect.width(), 0,
                            icon.getScaledWidth(c), destRect.height());
                    c.drawBitmap(icon, srcRect, destRect, null);
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }


}
