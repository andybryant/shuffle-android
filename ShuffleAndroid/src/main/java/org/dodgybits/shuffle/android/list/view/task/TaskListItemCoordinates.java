package org.dodgybits.shuffle.android.list.view.task;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;

/**
 * Represents the coordinates of elements inside a TaskListItem
 * (e.g. project, description, contexts, etc.) It will inflate a view,
 * and record the coordinates of each element after layout. This will allows us
 * to easily improve performance by creating custom view while still defining
 * layout in XML files.
 */
public class TaskListItemCoordinates {

    // Active and deleted state.
    int stateX;
    int stateY;

    // Project
    int projectX;
    int projectY;
    int projectWidth;
    int projectLineCount;
    int projectFontSize;
    int projectAscent;
    int projectOffset;

    // Details.
    int detailsX;
    int detailsY;
    int detailsWidth;
    int detailsLineCount;
    int detailsFontSize;
    int detailsAscent;

    // Description.
    int descriptionX;
    int descriptionY;
    int descriptionWidth;
    int descriptionLineCount;
    int descriptionFontSize;
    int descriptionAscent;

    // Contexts
    int contextsX;
    int contextsY;
    int contextsWidth;
    int contextsHeight;
    int contextsLabelLeft;
    int contextsLabelTop;
    int contextsSingleLabelLeft;
    int contextsSingleLabelTop;
    int contextsSingleFontSize;
    int contextsMultiFontSize;
    int contextsAscent;
    Rect contextSourceIconRect;
    RectF[][] contextRects = new RectF[5][4];
    RectF[][] contextDestIconRects = new RectF[5][4];
    Rect contextMoreRect;
    RectF activatedRect;
    RectF selectedRect;

    // Date.
    int dateX;
    int dateXEnd;
    int dateY;
    int dateWidth;
    int dateFontSize;
    int dateAscent;

    // Cache to save Coordinates based on view width.
    private static SparseArray<TaskListItemCoordinates> mCache =
            new SparseArray<>();

    // Not directly instantiable.
    private TaskListItemCoordinates() {}

    /**
     * Returns a value array multiplied by the specified density.
     */
    public static int[] getDensityDependentArray(int[] values, float density) {
        int result[] = new int[values.length];
        for (int i = 0; i < values.length; ++i) {
            result[i] = (int) (values[i] * density);
        }
        return result;
    }

    /**
     * Returns the height of the view in this mode.
     */
    public static int getHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.task_list_item_height);
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
     * Returns the number of lines of this text view.
     */
    private static int getLineCount(TextView textView) {
        return textView.getHeight() / textView.getLineHeight();
    }

    /**
     * Returns the length (maximum of characters) of details in this mode.
     */
    public static int getDetailsLength(Context context) {
        return context.getResources().getInteger(R.integer.content_length);
    }

    /**
     * Returns the length (maximum of characters) of description in this mode.
     */
    public static int getDescriptionLength(Context context) {
        return context.getResources().getInteger(R.integer.content_length);
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
    public static TaskListItemCoordinates forWidth(Context context, int width) {
        TaskListItemCoordinates coordinates = mCache.get(width);
        if (coordinates == null) {
            coordinates = new TaskListItemCoordinates();
            mCache.put(width, coordinates);

            // Layout the appropriate view.
            int height = getHeight(context);
            View view = LayoutInflater.from(context).inflate(R.layout.task_list_item, null);
            FontUtils.setCustomFont(view, context.getAssets());
            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            view.measure(widthSpec, heightSpec);
            view.layout(0, 0, width, height);

            View state = view.findViewById(R.id.active_state);
            coordinates.stateX = UiUtilities.getX(state);
            coordinates.stateY = UiUtilities.getY(state);

            TextView project = (TextView) view.findViewById(R.id.project);
            coordinates.projectX = UiUtilities.getX(project);
            coordinates.projectY = UiUtilities.getY(project);
            coordinates.projectWidth = getWidth(project, false);
            coordinates.projectLineCount = 1; //getLineCount(project);
            coordinates.projectFontSize = (int) project.getTextSize();
            coordinates.projectAscent = Math.round(project.getPaint().ascent());
            coordinates.projectOffset = context.getResources().
                    getDimensionPixelSize(R.dimen.task_list_project_offset);

            TextView details = (TextView) view.findViewById(R.id.details);
            coordinates.detailsX = UiUtilities.getX(details);
            coordinates.detailsY = UiUtilities.getY(details);
            coordinates.detailsWidth = getWidth(details, false);
            coordinates.detailsLineCount = 1; //getLineCount(details);
            coordinates.detailsFontSize = (int) details.getTextSize();
            coordinates.detailsAscent = Math.round(details.getPaint().ascent());

            TextView description = (TextView) view.findViewById(R.id.description);
            coordinates.descriptionX = UiUtilities.getX(description);
            coordinates.descriptionY = UiUtilities.getY(description);
            coordinates.descriptionWidth = getWidth(description, false);
            coordinates.descriptionLineCount = 1; //getLineCount(description);
            coordinates.descriptionFontSize = (int) description.getTextSize();
            coordinates.descriptionAscent = Math.round(description.getPaint().ascent());

            TextView contexts = (TextView) view.findViewById(R.id.context_block);
            coordinates.contextsX = UiUtilities.getX(contexts);
            coordinates.contextsY = UiUtilities.getY(contexts);

            coordinates.contextsLabelLeft = context.getResources().
                    getDimensionPixelSize(R.dimen.context_label_left);
            coordinates.contextsLabelTop = context.getResources().
                    getDimensionPixelSize(R.dimen.context_label_top);
            coordinates.contextsSingleLabelLeft = context.getResources().
                    getDimensionPixelSize(R.dimen.context_single_label_left);
            coordinates.contextsSingleLabelTop = context.getResources().
                    getDimensionPixelSize(R.dimen.context_single_label_top);
            coordinates.contextsMultiFontSize = (int) contexts.getTextSize();
            coordinates.contextsSingleFontSize = coordinates.contextsMultiFontSize * 2;
            coordinates.contextsAscent = Math.round(contexts.getPaint().ascent());
            coordinates.contextsWidth = getWidth(contexts, false);
            coordinates.contextsHeight = getHeight(contexts, false);
            float doublePadding = contexts.getPaddingLeft();
            float padding = contexts.getPaddingLeft() / 2f;
            coordinates.contextRects[0][0] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextDestIconRects[0][0] = inset(coordinates.contextRects[0][0], doublePadding);
            coordinates.contextRects[1][0] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextDestIconRects[1][0] = inset(coordinates.contextRects[1][0], doublePadding);
            coordinates.contextRects[2][0] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight / 2);
            coordinates.contextRects[2][1] = new RectF(
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight / 2);
            coordinates.contextRects[3][0] = new RectF(
                    coordinates.contextsX + coordinates.contextsWidth / 4,
                    coordinates.contextsY + coordinates.contextsHeight / 20,
                    coordinates.contextsX + 3 * coordinates.contextsWidth / 4,
                    coordinates.contextsY + 11 * coordinates.contextsHeight / 20);
            coordinates.contextRects[3][1] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY + coordinates.contextsHeight / 2,
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextRects[3][2] = new RectF(
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight / 2,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextRects[4][0] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight / 2);
            coordinates.contextRects[4][1] = new RectF(
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight / 2);
            coordinates.contextRects[4][2] = new RectF(
                    coordinates.contextsX,
                    coordinates.contextsY + coordinates.contextsHeight / 2,
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextRects[4][3] = new RectF(
                    coordinates.contextsX + coordinates.contextsWidth / 2,
                    coordinates.contextsY + coordinates.contextsHeight / 2,
                    coordinates.contextsX + coordinates.contextsWidth,
                    coordinates.contextsY + coordinates.contextsHeight);
            coordinates.contextMoreRect = new Rect(
                    coordinates.contextsX + coordinates.contextsWidth / 4,
                    coordinates.contextsY + coordinates.contextsHeight +
                            coordinates.projectOffset / 2,
                    coordinates.contextsX + 3 * coordinates.contextsWidth / 4,
                    coordinates.contextsY + coordinates.contextsHeight +
                            coordinates.projectOffset / 2 +
                            coordinates.contextsWidth / 2);
            coordinates.activatedRect = inset(coordinates.contextRects[0][0], padding);

            View selectedIndicator = view.findViewById(R.id.selected_indicator);
            int x = UiUtilities.getX(selectedIndicator);
            int y = UiUtilities.getY(selectedIndicator);
            coordinates.selectedRect = new RectF(
                    x,
                    y,
                    x + getWidth(selectedIndicator, false),
                    y + getHeight(selectedIndicator, false));

            for (int i = 2; i <= 4; i++) {
                for (int j = 0; j < i; j++) {
                    coordinates.contextDestIconRects[i][j] = inset(coordinates.contextRects[i][j], padding);
                }
            }

            TextView date = (TextView) view.findViewById(R.id.date);
            coordinates.dateX = UiUtilities.getX(date);
            coordinates.dateXEnd = UiUtilities.getX(date) + date.getWidth();
            coordinates.dateY = UiUtilities.getY(date);
            coordinates.dateWidth = getWidth(date, false);
            coordinates.dateFontSize = (int) date.getTextSize();
            coordinates.dateAscent = Math.round(date.getPaint().ascent());
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
        return "TaskListItemCoordinates{" +
                "stateX=" + stateX +
                ", stateY=" + stateY +
                ", projectX=" + projectX +
                ", projectY=" + projectY +
                ", projectWidth=" + projectWidth +
                ", projectLineCount=" + projectLineCount +
                ", projectFontSize=" + projectFontSize +
                ", projectAscent=" + projectAscent +
                ", detailsX=" + detailsX +
                ", detailsY=" + detailsY +
                ", detailsWidth=" + detailsWidth +
                ", detailsLineCount=" + detailsLineCount +
                ", detailsFontSize=" + detailsFontSize +
                ", detailsAscent=" + detailsAscent +
                ", descriptionX=" + descriptionX +
                ", descriptionY=" + descriptionY +
                ", descriptionWidth=" + descriptionWidth +
                ", descriptionLineCount=" + descriptionLineCount +
                ", descriptionFontSize=" + descriptionFontSize +
                ", descriptionAscent=" + descriptionAscent +
                ", contextsX=" + contextsX +
                ", contextsY=" + contextsY +
                ", contextsWidth=" + contextsWidth +
                ", contextsHeight=" + contextsHeight +
                ", dateX=" + dateX +
                ", dateXEnd=" + dateXEnd +
                ", dateY=" + dateY +
                ", dateWidth=" + dateWidth +
                ", dateFontSize=" + dateFontSize +
                ", dateAscent=" + dateAscent +
                '}';
    }
}
