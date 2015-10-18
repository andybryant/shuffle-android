package org.dodgybits.shuffle.android.list.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;

/**
 * Represents the coordinates of elements inside a ProjectListItem
 * (e.g. name, status, parallel icon, etc.) It will inflate a view,
 * and record the coordinates of each element after layout. This will allows us
 * to easily improve performance by creating custom view while still defining
 * layout in XML files.
 */
public class EntityListItemCoordinates {

    // Active and deleted state.
    int stateX;
    int stateY;

    int dragIndicatorX;
    int dragIndicatorWidth;
    int dragIndicatorYOffSet;
    int dragIndicatorHeight;

    // Name
    int nameX;
    int nameY;
    int nameWidth;
    int nameLineCount;
    int nameFontSize;
    int nameAscent;

    int labelX;
    int labelY;
    int labelFontSize;


    // Count.
    int countX;
    int countY;
    int countWidth;
    int countFontSize;
    int countAscent;

    // Selector
    int selectorX;
    int selectorY;
    int selectorWidth;
    int selectorHeight;
    int selectorFontSize;
    int selectorAscent;
    int selectorLabelLeft;
    int selectorLabelTop;
    Rect selectorSourceIconRect;
    RectF selectorRect;
    RectF activatedRect;

    // Cache to save Coordinates based on view width.
    private static SparseArray<EntityListItemCoordinates> mCache =
            new SparseArray<>();

    // Not directly instantiable.
    private EntityListItemCoordinates() {}

    /**
     * Returns the height of the view in this mode.
     */
    public static int getHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.list_item_height);
    }


    /**
     * Returns the width of a view.
     *
     * @param includeMargins whether or not to include margins when calculating
     *            width.
     */
    public static int getWidth(View view, boolean includeMargins) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return view.getWidth() + (includeMargins ? params.leftMargin + params.rightMargin : 0);
    }

    /**
     * Returns the height of a view.
     *
     * @param includeMargins whether or not to include margins when calculating
     *            height.
     */
    public static int getHeight(View view, boolean includeMargins) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return view.getHeight() + (includeMargins ? params.topMargin + params.bottomMargin : 0);
    }

    /**
     * Reset the caches associated with the coordinate layouts.
     */
    static void resetCaches() {
        mCache.clear();
    }

    /**
     * Returns coordinates for elements inside a conversation header view given
     * the view width.
     */
    public static EntityListItemCoordinates forWidth(Context context, int width) {
        EntityListItemCoordinates coordinates = mCache.get(width);
        if (coordinates == null) {
            coordinates = new EntityListItemCoordinates();
            mCache.put(width, coordinates);

            // Layout the appropriate view.
            int height = getHeight(context);
            View view = LayoutInflater.from(context).inflate(R.layout.entity_list_item, null);
            FontUtils.setCustomFont(view, context.getAssets());
            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            view.measure(widthSpec, heightSpec);
            view.layout(0, 0, width, height);

            View state = view.findViewById(R.id.active_state);
            coordinates.stateX = UiUtilities.getX(state);
            coordinates.stateY = UiUtilities.getY(state);

            TextView name = (TextView) view.findViewById(R.id.name);
            coordinates.nameX = UiUtilities.getX(name);
            coordinates.nameY = UiUtilities.getY(name);
            coordinates.nameWidth = getWidth(name, false);
            coordinates.nameLineCount = 1; //getLineCount(name);
            coordinates.nameFontSize = (int) name.getTextSize();
            coordinates.nameAscent = Math.round(name.getPaint().ascent());


            coordinates.labelX = coordinates.nameX;
            coordinates.labelY = coordinates.nameY;
            coordinates.labelFontSize = (int)(coordinates.nameFontSize * 1.3);

            TextView count = (TextView) view.findViewById(R.id.count);
            coordinates.countX = UiUtilities.getX(count);
            coordinates.countY = UiUtilities.getY(count);
            coordinates.countWidth = getWidth(count, false);
            coordinates.countFontSize = (int) count.getTextSize();
            coordinates.countAscent = Math.round(count.getPaint().ascent());

            TextView selector = (TextView) view.findViewById(R.id.selector_block);
            float padding = selector.getPaddingLeft();
            coordinates.selectorX = UiUtilities.getX(selector);
            coordinates.selectorY = UiUtilities.getY(selector);
            coordinates.selectorWidth = getWidth(selector, false);
            coordinates.selectorHeight = getHeight(selector, false);

            coordinates.selectorLabelLeft = context.getResources().
                    getDimensionPixelSize(R.dimen.selector_single_label_left);
            coordinates.selectorLabelTop = context.getResources().
                    getDimensionPixelSize(R.dimen.selector_single_label_top);
            coordinates.selectorFontSize = (int) selector.getTextSize();
            coordinates.selectorAscent = Math.round(selector.getPaint().ascent());

            coordinates.selectorRect = new RectF(
                    coordinates.selectorX,
                    coordinates.selectorY,
                    coordinates.selectorX + coordinates.selectorWidth,
                    coordinates.selectorY + coordinates.selectorHeight);
            coordinates.activatedRect = inset(coordinates.selectorRect, padding);

            coordinates.dragIndicatorX = context.getResources().
                    getDimensionPixelSize(R.dimen.move_indicator_padding);
            coordinates.dragIndicatorWidth = coordinates.selectorX  - 2 * coordinates.dragIndicatorX;
            coordinates.dragIndicatorHeight = context.getResources().
                    getDimensionPixelOffset(R.dimen.move_indicator_height);
            coordinates.dragIndicatorYOffSet = context.getResources().
                    getDimensionPixelOffset(R.dimen.move_indicator_vertical_offset);

        }
        return coordinates;
    }

    private static RectF inset(RectF src, float inset) {
        return new RectF(
                src.left + inset,
                src.top + inset,
                src.right - inset,
                src.bottom - inset);
    }

    @Override
    public String toString() {
        return "EntityListItemCoordinates{" +
                "stateX=" + stateX +
                ", stateY=" + stateY +
                ", nameX=" + nameX +
                ", nameY=" + nameY +
                ", nameWidth=" + nameWidth +
                ", nameLineCount=" + nameLineCount +
                ", nameFontSize=" + nameFontSize +
                ", nameAscent=" + nameAscent +
                ", countX=" + countX +
                ", countY=" + countY +
                ", countWidth=" + countWidth +
                ", countFontSize=" + countFontSize +
                ", countAscent=" + countAscent +
                ", selectorX=" + selectorX +
                ", selectorY=" + selectorY +
                ", selectorWidth=" + selectorWidth +
                ", selectorHeight=" + selectorHeight +
                ", selectorSourceIconRect=" + selectorSourceIconRect +
                ", selectorRect=" + selectorRect +
                ", activatedRect=" + activatedRect +
                '}';
    }
}
