/*
 * Copyright (C) 2012 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dodgybits.shuffle.android.core.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.google.common.annotations.VisibleForTesting;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.controller.AbstractActivityController;

public class TwoPaneLayout extends FrameLayout {
    private static final String TAG = "TwoPaneLayout";
    private static final long SLIDE_DURATION_MS = 300;

    private final double mTaskListWeight;
    private final TimeInterpolator mSlideInterpolator;
    /**
     * True if and only if the task list is collapsible in the current device configuration.
     * See {@link #isTaskListCollapsed()} to see whether it is currently collapsed
     * (based on the current view mode).
     */
    private final boolean mListCollapsible;

    /**
     * The current mode that the tablet layout is in. This is a constant integer that holds values
     * that are {@link ViewMode} constants like {@link ViewMode#TASK}.
     */
    private int mCurrentMode = ViewMode.UNKNOWN;
    /**
     * This mode represents the current positions of the three panes. This is split out from the
     * current mode to give context to state transitions.
     */
    private int mPositionedMode = ViewMode.UNKNOWN;

    private AbstractActivityController mController;
    private LayoutListener mListener;
    private boolean mIsSearchResult;

    private DrawerLayout mDrawerLayout;

    private View mMiscellaneousView;
    private View mTaskView;
    private View mListView;

    public static final int MISCELLANEOUS_VIEW_ID = R.id.miscellaneous_pane;

    private final Runnable mTransitionCompleteRunnable = new Runnable() {
        @Override
        public void run() {
            onTransitionComplete();
        }
    };

    private boolean mDrawerInitialSetupComplete;    
    
    /**
     * True if and only if the task list is collapsible in the current device configuration.
     * See {@link #isTaskListCollapsed()} to see whether it is currently collapsed
     * (based on the current view mode).
     */
    private final boolean mListCollapsible;

    public TwoPaneLayout(Context context) {
        this(context, null);
    }

    public TwoPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = getResources();

        // The task list might be visible now, depending on the layout: in portrait we
        // don't show the task list, but in landscape we do.  This information is stored
        // in the constants
        mListCollapsible = res.getBoolean(R.bool.list_collapsible);

        final int taskListWeight = res.getInteger(R.integer.task_list_weight);
        final int taskViewWeight = res.getInteger(R.integer.task_view_weight);
        mTaskListWeight = (double) taskListWeight
                / (taskListWeight + taskViewWeight);

        mDrawerInitialSetupComplete = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mListView = findViewById(R.id.task_list_pane);
        mTaskView = findViewById(R.id.task_pane);
        mMiscellaneousView = findViewById(MISCELLANEOUS_VIEW_ID);

        // all panes start GONE in initial UNKNOWN mode to avoid drawing misplaced panes
        mCurrentMode = ViewMode.UNKNOWN;
        mListView.setVisibility(GONE);
        mTaskView.setVisibility(GONE);
        mMiscellaneousView.setVisibility(GONE);
    }

    @VisibleForTesting
    public void setController(AbstractActivityController controller, boolean isSearchResult) {
        mController = controller;
        mListener = controller;
        mIsSearchResult = isSearchResult;
    }

    public void setDrawerLayout(DrawerLayout drawerLayout) {
        mDrawerLayout = drawerLayout;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setupPaneWidths(MeasureSpec.getSize(widthMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed || mCurrentMode != mPositionedMode) {
            positionPanes(getMeasuredWidth());
        }
        super.onLayout(changed, l, t, r, b);
    }

    /**
     * Sizes up the three sliding panes. This method will ensure that the LayoutParams of the panes
     * have the correct widths set for the current overall size and view mode.
     *
     * @param parentWidth this view's new width
     */
    private void setupPaneWidths(int parentWidth) {
        final int foldersWidth = computeFolderListWidth(parentWidth);
        final int foldersFragmentWidth;
        if (isDrawerView(mFoldersView)) {
            foldersFragmentWidth = getResources().getDimensionPixelSize(R.dimen.drawer_width);
        } else {
            foldersFragmentWidth = foldersWidth;
        }
        final int convWidth = computeTaskWidth(parentWidth);

        setPaneWidth(mFoldersView, foldersFragmentWidth);

        // only adjust the fixed task view width when my width changes
        if (parentWidth != getMeasuredWidth()) {
            LogUtils.i(LOG_TAG, "setting up new TPL, w=%d fw=%d cv=%d", parentWidth,
                    foldersWidth, convWidth);

            setPaneWidth(mMiscellaneousView, convWidth);
            setPaneWidth(mTaskView, convWidth);
        }

        final int currListWidth = getPaneWidth(mListView);
        int listWidth = currListWidth;
        switch (mCurrentMode) {
            case ViewMode.AD:
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK:
                if (!mListCollapsible) {
                    listWidth = parentWidth - convWidth;
                }
                break;
            case ViewMode.TASK_LIST:
            case ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION:
            case ViewMode.SEARCH_RESULTS_LIST:
                listWidth = parentWidth - foldersWidth;
                break;
            default:
                break;
        }
        LogUtils.d(LOG_TAG, "task list width change, w=%d", listWidth);
        setPaneWidth(mListView, listWidth);

        if ((mCurrentMode != mPositionedMode && mPositionedMode != ViewMode.UNKNOWN)
                || mListCopyWidthOnComplete != null) {
            mListCopyWidthOnComplete = listWidth;
        } else {
            setPaneWidth(mListCopyView, listWidth);
        }
    }

    /**
     * Positions the three sliding panes at the correct X offset (using {@link View#setX(float)}).
     * When switching from list->task mode or vice versa, animate the change in X.
     *
     * @param width
     */
    private void positionPanes(int width) {
        if (mPositionedMode == mCurrentMode) {
            return;
        }

        boolean hasPositions = false;
        int convX = 0, listX = 0, foldersX = 0;

        switch (mCurrentMode) {
            case ViewMode.AD:
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK: {
                final int foldersW = getPaneWidth(mFoldersView);
                final int listW;
                listW = getPaneWidth(mListView);

                if (mListCollapsible) {
                    convX = 0;
                    listX = -listW;
                    foldersX = listX - foldersW;
                } else {
                    convX = listW;
                    listX = 0;
                    foldersX = -foldersW;
                }
                hasPositions = true;
                LogUtils.i(LOG_TAG, "task mode layout, x=%d/%d/%d", foldersX, listX, convX);
                break;
            }
            case ViewMode.TASK_LIST:
            case ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION:
            case ViewMode.SEARCH_RESULTS_LIST: {
                convX = width;
                listX = getPaneWidth(mFoldersView);
                foldersX = 0;

                hasPositions = true;
                LogUtils.i(LOG_TAG, "conv-list mode layout, x=%d/%d/%d", foldersX, listX, convX);
                break;
            }
            default:
                break;
        }

        if (hasPositions) {
            animatePanes(foldersX, listX, convX);
        }

        mPositionedMode = mCurrentMode;
    }

    private final AnimatorListenerAdapter mPaneAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mListCopyView.unbind();
            useHardwareLayer(false);
            fixupListCopyWidth();
            onTransitionComplete();
        }
        @Override
        public void onAnimationCancel(Animator animation) {
            mListCopyView.unbind();
            useHardwareLayer(false);
        }
    };

    /**
     * @param foldersX
     * @param listX
     * @param convX
     */
    private void animatePanes(int foldersX, int listX, int convX) {
        // If positioning has not yet happened, we don't need to animate panes into place.
        // This happens on first layout, rotate, and when jumping straight to a task from
        // a view intent.
        if (mPositionedMode == ViewMode.UNKNOWN) {
            mTaskView.setX(convX);
            mMiscellaneousView.setX(convX);
            mListView.setX(listX);
            if (!isDrawerView(mFoldersView)) {
                mFoldersView.setX(foldersX);
            }

            // listeners need to know that the "transition" is complete, even if one is not run.
            // defer notifying listeners because we're in a layout pass, and they might do layout.
            post(mTransitionCompleteRunnable);
            return;
        }

        final boolean useListCopy = getPaneWidth(mListView) != getPaneWidth(mListCopyView);

        if (useListCopy) {
            // freeze the current list view before it gets redrawn
            mListCopyView.bind(mListView);
            mListCopyView.setX(mListView.getX());

            mListCopyView.setAlpha(1.0f);
            mListView.setAlpha(0.0f);
        }

        useHardwareLayer(true);

        if (ViewMode.isAdMode(mCurrentMode)) {
            mMiscellaneousView.animate().x(convX);
        } else {
            mTaskView.animate().x(convX);
        }

        if (!isDrawerView(mFoldersView)) {
            mFoldersView.animate().x(foldersX);
        }
        if (useListCopy) {
            mListCopyView.animate().x(listX).alpha(0.0f);
        }
        mListView.animate()
                .x(listX)
                .alpha(1.0f)
                .setListener(mPaneAnimationListener);
        configureAnimations(mTaskView, mFoldersView, mListView, mListCopyView,
                mMiscellaneousView);
    }

    private void configureAnimations(View... views) {
        for (View v : views) {
            if (isDrawerView(v)) {
                continue;
            }
            v.animate()
                    .setInterpolator(mSlideInterpolator)
                    .setDuration(SLIDE_DURATION_MS);
        }
    }

    private void useHardwareLayer(boolean useHardware) {
        final int layerType = useHardware ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE;
        if (!isDrawerView(mFoldersView)) {
            mFoldersView.setLayerType(layerType, null);
        }
        mListView.setLayerType(layerType, null);
        mListCopyView.setLayerType(layerType, null);
        mTaskView.setLayerType(layerType, null);
        mMiscellaneousView.setLayerType(layerType, null);
        if (useHardware) {
            // these buildLayer calls are safe because layout is the only way we get here
            // (i.e. these views must already be attached)
            if (!isDrawerView(mFoldersView)) {
                mFoldersView.buildLayer();
            }
            mListView.buildLayer();
            mListCopyView.buildLayer();
            mTaskView.buildLayer();
            mMiscellaneousView.buildLayer();
        }
    }

    private void fixupListCopyWidth() {
        if (mListCopyWidthOnComplete == null ||
                getPaneWidth(mListCopyView) == mListCopyWidthOnComplete) {
            mListCopyWidthOnComplete = null;
            return;
        }
        LogUtils.i(LOG_TAG, "onAnimationEnd of list view, setting copy width to %d",
                mListCopyWidthOnComplete);
        setPaneWidth(mListCopyView, mListCopyWidthOnComplete);
        mListCopyWidthOnComplete = null;
    }

    private void onTransitionComplete() {
        if (mController.isDestroyed()) {
            // quit early if the hosting activity was destroyed before the animation finished
            LogUtils.i(LOG_TAG, "IN TPL.onTransitionComplete, activity destroyed->quitting early");
            return;
        }

        switch (mCurrentMode) {
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK:
                dispatchTaskVisibilityChanged(true);
                dispatchTaskListVisibilityChange(!isTaskListCollapsed());

                break;
            case ViewMode.TASK_LIST:
            case ViewMode.SEARCH_RESULTS_LIST:
                dispatchTaskVisibilityChanged(false);
                dispatchTaskListVisibilityChange(true);

                break;
            case ViewMode.AD:
                dispatchTaskVisibilityChanged(false);
                dispatchTaskListVisibilityChange(!isTaskListCollapsed());

                break;
            default:
                break;
        }
    }

    /**
     * Computes the width of the task list in stable state of the current mode.
     */
    public int computeTaskListWidth() {
        return computeTaskListWidth(getMeasuredWidth());
    }

    /**
     * Computes the width of the task list in stable state of the current mode.
     */
    private int computeTaskListWidth(int totalWidth) {
        switch (mCurrentMode) {
            case ViewMode.TASK_LIST:
            case ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION:
            case ViewMode.SEARCH_RESULTS_LIST:
                return totalWidth - computeFolderListWidth(totalWidth);
            case ViewMode.AD:
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK:
                return (int) (totalWidth * mTaskListWeight);
        }
        return 0;
    }

    public int computeTaskWidth() {
        return computeTaskWidth(getMeasuredWidth());
    }

    /**
     * Computes the width of the task pane in stable state of the
     * current mode.
     */
    private int computeTaskWidth(int totalWidth) {
        if (mListCollapsible) {
            return totalWidth;
        } else {
            return totalWidth - (int) (totalWidth * mTaskListWeight);
        }
    }

    /**
     * Computes the width of the folder list in stable state of the current mode.
     */
    private int computeFolderListWidth(int parentWidth) {
        if (mIsSearchResult) {
            return 0;
        } else if (isDrawerView(mFoldersView)) {
            return 0;
        } else {
            return (int) (parentWidth * mFolderListWeight);
        }
    }

    private void dispatchTaskListVisibilityChange(boolean visible) {
        if (mListener != null) {
            mListener.onTaskListVisibilityChanged(visible);
        }
    }

    private void dispatchTaskVisibilityChanged(boolean visible) {
        if (mListener != null) {
            mListener.onTaskVisibilityChanged(visible);
        }
    }

    // does not apply to drawer children. will return zero for those.
    private int getPaneWidth(View pane) {
        return isDrawerView(pane) ? 0 : pane.getLayoutParams().width;
    }

    private boolean isDrawerView(View child) {
        return child != null && child.getParent() == mDrawerLayout;
    }

    /**
     * @return Whether or not the task list is visible on screen.
     */
    public boolean isTaskListCollapsed() {
        return !ViewMode.isListMode(mCurrentMode) && mListCollapsible;
    }

    @Override
    public void onViewModeChanged(int newMode) {
        // make all initially GONE panes visible only when the view mode is first determined
        if (mCurrentMode == ViewMode.UNKNOWN) {
            mFoldersView.setVisibility(VISIBLE);
            mListView.setVisibility(VISIBLE);
            mListCopyView.setVisibility(VISIBLE);
        }

        if (ViewMode.isAdMode(newMode)) {
            mMiscellaneousView.setVisibility(VISIBLE);
            mTaskView.setVisibility(GONE);
        } else {
            mTaskView.setVisibility(VISIBLE);
            mMiscellaneousView.setVisibility(GONE);
        }

        // set up the drawer as appropriate for the configuration
        final ViewParent foldersParent = mFoldersView.getParent();
        if (mIsExpansiveLayout && foldersParent != this) {
            if (foldersParent != mDrawerLayout) {
                throw new IllegalStateException("invalid Folders fragment parent: " +
                        foldersParent);
            }
            mDrawerLayout.removeView(mFoldersView);
            addView(mFoldersView, 0);
            mFoldersView.findViewById(R.id.folders_pane_edge).setVisibility(VISIBLE);
            mFoldersView.setBackgroundDrawable(null);
        } else if (!mIsExpansiveLayout && foldersParent == this) {
            removeView(mFoldersView);
            mDrawerLayout.addView(mFoldersView);
            final DrawerLayout.LayoutParams lp =
                    (DrawerLayout.LayoutParams) mFoldersView.getLayoutParams();
            lp.gravity = Gravity.START;
            mFoldersView.setLayoutParams(lp);
            mFoldersView.findViewById(R.id.folders_pane_edge).setVisibility(GONE);
            mFoldersView.setBackgroundResource(R.color.list_background_color);
        }

        // detach the pager immediately from its data source (to prevent processing updates)
        if (ViewMode.isTaskMode(mCurrentMode)) {
            mController.disablePagerUpdates();
        }

        mDrawerInitialSetupComplete = true;
        mCurrentMode = newMode;
        LogUtils.i(LOG_TAG, "onViewModeChanged(%d)", newMode);

        // do all the real work in onMeasure/onLayout, when panes are sized and positioned for the
        // current width/height anyway
        requestLayout();
    }

    public boolean isModeChangePending() {
        return mPositionedMode != mCurrentMode;
    }

    private void setPaneWidth(View pane, int w) {
        final ViewGroup.LayoutParams lp = pane.getLayoutParams();
        if (lp.width == w) {
            return;
        }
        lp.width = w;
        pane.setLayoutParams(lp);
        if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
            final String s;
            if (pane == mFoldersView) {
                s = "folders";
            } else if (pane == mListView) {
                s = "conv-list";
            } else if (pane == mTaskView) {
                s = "conv-view";
            } else if (pane == mMiscellaneousView) {
                s = "misc-view";
            } else {
                s = "???:" + pane;
            }
            LogUtils.d(LOG_TAG, "TPL: setPaneWidth, w=%spx pane=%s", w, s);
        }
    }

    public boolean isDrawerEnabled() {
        return !mIsExpansiveLayout && mDrawerInitialSetupComplete;
    }

    public boolean isExpansiveLayout() {
        return mIsExpansiveLayout;
    }
}
