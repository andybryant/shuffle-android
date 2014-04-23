package org.dodgybits.shuffle.android.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.view.ViewParent;
import android.widget.ListView;
import org.dodgybits.android.shuffle.R;

public class UiUtilities {
    private UiUtilities() {
    }

    /**
     * Returns a boolean indicating whether the table UI should be shown.
     */
    public static boolean useTabletUI(Resources res) {
        return res.getBoolean(R.bool.use_tablet_ui);
    }

    /** Generics version of {@link android.app.Activity#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(Activity parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /** Generics version of {@link View#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(View parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /**
     * Same as {@link Activity#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(Activity parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(Activity parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Returns the x coordinates of a view by tracing up its hierarchy.
     */
    public static int getX(View view) {
        int x = 0;
        while (view != null) {
            if (OSUtils.atLeastHoneycomb())
            {
                x += (int) view.getX();
            } else {
                x += view.getLeft();
            }
            ViewParent parent = view.getParent();
            view = parent != null ? (View) parent : null;
        }
        return x;
    }

    /**
     * Returns the y coordinates of a view by tracing up its hierarchy.
     */
    public static int getY(View view) {
        int y = 0;
        while (view != null) {
            if (OSUtils.atLeastHoneycomb())
            {
                y += (int) view.getY();
            }
            else
            {
                y += view.getTop();
            }
            ViewParent parent = view.getParent();
            view = parent != null ? (View) parent : null;
        }
        return y;
    }

    /**
     * Workaround for the {@link android.widget.ListView#smoothScrollToPosition} randomly scroll the view bug
     * if it's called right after {@link android.widget.ListView#setAdapter}.
     */
    public static void listViewSmoothScrollToPosition(final Activity activity,
                                                      final ListView listView, final int position) {
        // Workarond: delay-call smoothScrollToPosition()
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) {
                    return; // Activity being destroyed
                }
                listView.smoothScrollToPosition(position);
            }
        });
    }

    public static String getCountForUi(Context context, int count,
                                              boolean replaceZeroWithBlank) {
        if (replaceZeroWithBlank && (count == 0)) {
            return "";
        } else if (count > 999) {
            return context.getString(R.string.more_than_999);
        } else {
            return Integer.toString(count);
        }
    }

}
