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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.EntityListVisibilityChangeEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.TaskVisibilityChangeEvent;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import roboguice.RoboGuice;
import roboguice.event.EventManager;
import roboguice.event.Observes;

/**
 * This is a custom layout that manages the possible views of Shuffle's large screen (read: tablet)
 * activity, and the transitions between them.
 *
 * This is not intended to be a generic layout; it is specific to the {@code Fragment}s
 * available in {@link org.dodgybits.shuffle.android.core.activity.AbstractMainActivity}
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
     * The current view that the tablet layout is in.
     */
    private Location mCurrentView = Location.newBuilder().build();

    /**
     * This mode represents the current positions of the three panes. This is split out from the
     * current mode to give context to state transitions.
     */
    private Location mPositionedView = Location.newBuilder().build();

    private boolean mIsSearchResult;

    private DrawerLayout mDrawerLayout;

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

    @Inject
    private LocationProvider mLocationProvider;

    public TwoPaneLayout(Context context) {
        this(context, null);
    }

    public TwoPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = getResources();

        // The task list might be visible now, depending on the layout: in portrait we
        // don't show the task list, but in landscape we do.  This information is stored
        // in the constants
        mListCollapsible = UiUtilities.isListCollapsible(res);

        final int taskListWeight = res.getInteger(R.integer.task_list_weight);
        final int taskViewWeight = res.getInteger(R.integer.task_view_weight);
        mTaskListWeight = (double) taskListWeight
                / (taskListWeight + taskViewWeight);

        mDrawerInitialSetupComplete = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mListView = findViewById(R.id.entity_list_pane);
        mTaskView = findViewById(R.id.task_pane);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // all panes start GONE in initial UNKNOWN mode to avoid drawing misplaced panes
        mCurrentView = Location.newBuilder().build();
        mListView.setVisibility(GONE);
        mTaskView.setVisibility(GONE);

        Log.d(TAG, "Injecting dependencies");
        RoboGuice.getInjector(getContext()).injectMembersWithoutViews(this);
        onViewChanged(mLocationProvider.getLocation());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setupPaneWidths(MeasureSpec.getSize(widthMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed || !mCurrentView.equals(mPositionedView)) {
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
        ViewMode viewMode = mCurrentView.getViewMode();

        // only adjust the fixed task view width when my width changes
        if (parentWidth != getMeasuredWidth()) {
            Log.i(TAG, "setting up new TPL, w=" + parentWidth + " tw=" + taskWidth + " mode=" + viewMode);
            setPaneWidth(mTaskView, taskWidth);
        }

        final int currListWidth = getPaneWidth(mListView);
        int listWidth = currListWidth;
        switch (viewMode) {
            case TASK:
            case SEARCH_RESULTS_TASK:
                if (!mListCollapsible) {
                    listWidth = parentWidth - taskWidth;
                }
                break;
            case TASK_LIST:
            case PROJECT_LIST:
            case CONTEXT_LIST:
            case SEARCH_RESULTS_LIST:
                listWidth = parentWidth;
                break;
            default:
                break;
        }
        Log.d(TAG, "task list width change, w=" + listWidth);
        setPaneWidth(mListView, listWidth);
    }

    /**
     * Positions the two panes at the correct X offset (using {@link View#setX(float)}).
     *
     * @param width
     */
    private void positionPanes(int width) {
        if (!isModeChangePending()) {
            return;
        }

        boolean hasPositions = false;
        int taskX = 0, listX = 0;

        switch (mCurrentView.getViewMode()) {
            case TASK:
            case SEARCH_RESULTS_TASK: {
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
            case TASK_LIST:
            case PROJECT_LIST:
            case CONTEXT_LIST:
            case SEARCH_RESULTS_LIST: {
                taskX = width;
                listX = 0;
                hasPositions = true;
                Log.i(TAG, "task-list mode layout, x=" + listX + "/" + taskX);
                break;
            }
            default:
                break;
        }

        if (hasPositions) {
            mListView.setX(listX);
            mTaskView.setX(taskX);

            // listeners need to know that the "transition" is complete, even if one is not run.
            // defer notifying listeners because we're in a layout pass, and they might do layout.
            post(mTransitionCompleteRunnable);
        }

        // For views that are not on the screen, let's set their visibility for accessibility.
        mListView.setVisibility(listX >= 0 ? VISIBLE : INVISIBLE);
        mTaskView.setVisibility(taskX < width ? VISIBLE : INVISIBLE);

        mPositionedView = mCurrentView;
    }

    private void onTransitionComplete() {
        switch (mCurrentView.getViewMode()) {
            case TASK:
            case SEARCH_RESULTS_TASK:
                mEventManager.fire(new TaskVisibilityChangeEvent(true));
                mEventManager.fire(new EntityListVisibilityChangeEvent(!isEntityListCollapsed()));
                break;
            case TASK_LIST:
            case PROJECT_LIST:
            case CONTEXT_LIST:
            case SEARCH_RESULTS_LIST:
                mEventManager.fire(new TaskVisibilityChangeEvent(false));
                mEventManager.fire(new EntityListVisibilityChangeEvent(true));
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
        switch (mCurrentView.getViewMode()) {
            case TASK_LIST:
            case PROJECT_LIST:
            case CONTEXT_LIST:
            case SEARCH_RESULTS_LIST:
                return totalWidth;
            case TASK:
            case SEARCH_RESULTS_TASK:
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
     * @return Whether or not the entity list is visible on screen.
     */
    public boolean isEntityListCollapsed() {
        return !ViewMode.isListMode(mCurrentView.getViewMode()) && mListCollapsible;
    }

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        Location newView = event.getLocation();
        onViewChanged(newView);
    }

    private void onViewChanged(Location newView) {
        // make all initially GONE panes visible only when the view mode is first determined
        ViewMode currentMode = mCurrentView.getViewMode();

        // detach the pager immediately from its data source (to prevent processing updates)
        if (ViewMode.isTaskMode(currentMode)) {
//            mController.disablePagerUpdates();
        }

        mListView.setVisibility(VISIBLE);
        mTaskView.setVisibility(VISIBLE);

        mDrawerInitialSetupComplete = true;
        mCurrentView = newView;
        Log.i(TAG, "onViewChanged " + newView);

        // do all the real work in onMeasure/onLayout, when panes are sized and positioned for the
        // current width/height anyway
        requestLayout();
    }

    public boolean isModeChangePending() {
        return !mPositionedView.equals(mCurrentView);
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
            s = "task-list";
        } else if (pane == mTaskView) {
            s = "task-view";
        } else {
            s = "???:" + pane;
        }
        Log.d(TAG, "TPL: setPaneWidth, w=" + w + " pane=" + s);
    }
}
