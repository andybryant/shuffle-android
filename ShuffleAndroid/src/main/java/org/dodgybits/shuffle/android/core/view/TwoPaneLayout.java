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

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.controller.AbstractActivityController;
import org.dodgybits.shuffle.android.core.event.EntityListVisibiltyChangeEvent;
import org.dodgybits.shuffle.android.core.event.ModeChangeEvent;
import org.dodgybits.shuffle.android.core.event.TaskVisibilityChangeEvent;
import roboguice.RoboGuice;
import roboguice.event.EventManager;
import roboguice.event.Observes;

/**
 * This is a custom layout that manages the possible views of Shuffle's large screen (read: tablet)
 * activity, and the transitions between them.
 *
 * This is not intended to be a generic layout; it is specific to the {@code Fragment}s
 * available in {@link org.dodgybits.shuffle.android.core.activity.MainActivity}
 * and assumes their existence. It merely configures them
 * according to the specific <i>modes</i> the {@link android.app.Activity} can be in.
 *
 * Currently, the layout differs in three dimensions: orientation, two aspects of view modes.
 * This results in essentially three states: One where an entity list (context, project or task)
 * takes up the whole screen, another where the task list is on the left and task on the right
 * and one where just the task shows.
 *
 * In task, context or project list view, tasks are hidden.
 * This is the case in both portrait and landscape
 */
public class TwoPaneLayout extends FrameLayout {
    private static final String TAG = "TwoPaneLayout";

    private final double mTaskListWeight;
    /**
     * True if and only if the task list is collapsible in the current device configuration.
     * See {@link #isEntityListCollapsed()} to see whether it is currently collapsed
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
    private boolean mIsSearchResult;

    private DrawerLayout mDrawerLayout;

    private View mQuickAddView;
    private View mTaskView;
    private View mListView;
    private boolean mDrawerInitialSetupComplete;

    private final Runnable mTransitionCompleteRunnable = new Runnable() {
        @Override
        public void run() {
            onTransitionComplete();
        }
    };

    @Inject
    protected EventManager mEventManager;

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

        mQuickAddView = findViewById(R.id.quick_add);
        mListView = findViewById(R.id.entity_list_pane);
        mTaskView = findViewById(R.id.task_pane);

        // all panes start GONE in initial UNKNOWN mode to avoid drawing misplaced panes
        mCurrentMode = ViewMode.UNKNOWN;
        mQuickAddView.setVisibility(GONE);
        mListView.setVisibility(GONE);
        mTaskView.setVisibility(GONE);

        Log.d(TAG, "Injecting dependencies");
        RoboGuice.getInjector(getContext()).injectMembersWithoutViews(this);
    }

    @VisibleForTesting
    public void setController(AbstractActivityController controller, boolean isSearchResult) {
        mController = controller;
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
     * Sizes up the two panes. This method will ensure that the LayoutParams of the panes
     * have the correct widths set for the current overall size and view mode.
     *
     * @param parentWidth this view's new width
     */
    private void setupPaneWidths(int parentWidth) {
        final int taskWidth = computeTaskWidth(parentWidth);

        // only adjust the fixed task view width when my width changes
        if (parentWidth != getMeasuredWidth()) {
            Log.i(TAG, "setting up new TPL, w=" + parentWidth + "tv=" + taskWidth);
            setPaneWidth(mTaskView, taskWidth);
        }

        final int currListWidth = getPaneWidth(mListView);
        int listWidth = currListWidth;
        switch (mCurrentMode) {
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK:
                if (!mListCollapsible) {
                    listWidth = parentWidth - taskWidth;
                }
                break;
            case ViewMode.TASK_LIST:
            case ViewMode.PROJECT_LIST:
            case ViewMode.CONTEXT_LIST:
            case ViewMode.SEARCH_RESULTS_LIST:
                listWidth = parentWidth;
                break;
            default:
                break;
        }
        Log.d(TAG, "task list width change, w=" + listWidth);
        setPaneWidth(mListView, listWidth);
        setPaneWidth(mQuickAddView, listWidth);
    }

    /**
     * Positions the two panes at the correct X offset (using {@link View#setX(float)}).
     *
     * @param width
     */
    private void positionPanes(int width) {
        if (mPositionedMode == mCurrentMode) {
            return;
        }

        boolean hasPositions = false;
        int taskX = 0, listX = 0;

        switch (mCurrentMode) {
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK: {
                final int listW = getPaneWidth(mListView);

                if (mListCollapsible) {
                    taskX = 0;
                    listX = -listW;
                } else {
                    taskX = listW;
                    listX = 0;
                }
                hasPositions = true;
                Log.i(TAG, "task mode layout, x=" + listX + "/" + taskX);
                break;
            }
            case ViewMode.TASK_LIST:
            case ViewMode.PROJECT_LIST:
            case ViewMode.CONTEXT_LIST:
            case ViewMode.SEARCH_RESULTS_LIST: {
                taskX = width;
                listX = 0;
                hasPositions = true;
                Log.i(TAG, "conv-list mode layout, x=" + listX + "/" + taskX);
                break;
            }
            default:
                break;
        }

        if (hasPositions) {
            mListView.setX(listX);
            mTaskView.setX(taskX);
            mQuickAddView.setX(listX);

            // listeners need to know that the "transition" is complete, even if one is not run.
            // defer notifying listeners because we're in a layout pass, and they might do layout.
            post(mTransitionCompleteRunnable);
        }

        mPositionedMode = mCurrentMode;
    }

    private void onTransitionComplete() {
        switch (mCurrentMode) {
            case ViewMode.TASK:
            case ViewMode.SEARCH_RESULTS_TASK:
                mEventManager.fire(new TaskVisibilityChangeEvent(true));
                mEventManager.fire(new EntityListVisibiltyChangeEvent(!isEntityListCollapsed()));
                break;
            case ViewMode.TASK_LIST:
            case ViewMode.PROJECT_LIST:
            case ViewMode.CONTEXT_LIST:
            case ViewMode.SEARCH_RESULTS_LIST:
                mEventManager.fire(new TaskVisibilityChangeEvent(false));
                mEventManager.fire(new EntityListVisibiltyChangeEvent(true));
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
            case ViewMode.PROJECT_LIST:
            case ViewMode.CONTEXT_LIST:
            case ViewMode.SEARCH_RESULTS_LIST:
                return totalWidth;
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
    public boolean isEntityListCollapsed() {
        return !ViewMode.isListMode(mCurrentMode) && mListCollapsible;
    }

    public void onViewModeChanged(@Observes ModeChangeEvent modeChangeEvent) {
        int newMode = modeChangeEvent.getNewMode();
        // make all initially GONE panes visible only when the view mode is first determined
        if (mCurrentMode == ViewMode.UNKNOWN) {
            mQuickAddView.setVisibility(VISIBLE);
            mListView.setVisibility(VISIBLE);
            mTaskView.setVisibility(VISIBLE);
        }

        // detach the pager immediately from its data source (to prevent processing updates)
        if (ViewMode.isTaskMode(mCurrentMode)) {
            mController.disablePagerUpdates();
        }

        mDrawerInitialSetupComplete = true;
        mCurrentMode = newMode;
        Log.i(TAG, "onViewModeChanged " + newMode);

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
        final String s;
        if (pane == mListView) {
            s = "conv-list";
        } else if (pane == mTaskView) {
            s = "conv-view";
        } else {
            s = "???:" + pane;
        }
        Log.d(TAG, "TPL: setPaneWidth, w=" + w + " pane=" + s);
    }
}
