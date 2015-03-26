/**
 * Copyright (C) 2014 Android Shuffle Open Source Project
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
package org.dodgybits.shuffle.android.core.controller;


/**
 * This is an abstract implementation of the Activity Controller. This class
 * knows how to respond to menu items, state changes, layout changes, etc. It
 * weaves together the views and listeners, dispatching actions to the
 * respective underlying classes.
 * <p>
 * Even though this class is abstract, it should provide default implementations
 * for most, if not all the methods in the ActivityController interface. This
 * makes the task of the subclasses easier: OnePaneController and
 * TwoPaneController can be concise when the common functionality is in
 * AbstractActivityController.
 * </p>
 */
public abstract class AbstractActivityController {
//
//    /** Key to store {@link #mTaskListScrollPositions} */
//    private static final String SAVED_TASK_LIST_SCROLL_POSITIONS =
//            "saved-task-list-scroll-positions";
//
//
//
//    protected MainActivity mActivity;
//    protected ViewMode mViewMode;
//
//    private Cursor mTaskListCursor;
//    private TaskListContext mListContext;
//
//
//    /** A map of {@link ListQuery} to scroll position in the task list. */
//    private final Bundle mTaskListScrollPositions = new Bundle();
//
//
//
//    private Map<ListQuery,Integer> mQueryIndex;
//
//    /**
//     * Selected conversations, if any.
//     */
//    private final TaskSelectionSet mSelectedSet = new TaskSelectionSet();
//
//    final private FragmentManager mFragmentManager;
//
//    @Inject
//    protected AbstractActivityController(MainActivity activity, ViewMode viewMode) {
//        mActivity = activity;
//        mViewMode = viewMode;
//        mFragmentManager = activity.getSupportFragmentManager();
//        // Allow the fragment to observe changes to its own selection set. No other object is
//        // aware of the selected set.
//
////        mSelectedSet.addObserver(this);
//
//    }
//
//    /**
//     * Check if the fragment is attached to an activity and has a root view.
//     * @param in fragment to be checked
//     * @return true if the fragment is valid, false otherwise
//     */
//    private static boolean isValidFragment(Fragment in) {
//        return !(in == null || in.getActivity() == null || in.getView() == null);
//    }
//
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//    }
//
//    @Override
//    public boolean onBackPressed() {
//        if (isDrawerEnabled() && mNavigationDrawerFragment.isDrawerVisible()) {
//            mNavigationDrawerFragment.closeDrawers();
//            return true;
//        }
//
//        return handleBackPress();
//    }
//
//    @Override
//    public boolean onUpPressed() {
//        return handleUpPress();
//    }
//
//    @Override
//    public void setTaskListScrollPosition(ListQuery listQuery, Parcelable savedPosition) {
//        mTaskListScrollPositions.putParcelable(listQuery.name(), savedPosition);
//    }
//
//    @Override
//    public Parcelable getTaskListScrollPosition(ListQuery listQuery) {
//        return mTaskListScrollPositions.getParcelable(listQuery.name());
//    }
//
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedState) {
//        mTaskListScrollPositions.clear();
//        mTaskListScrollPositions.putAll(
//                savedState.getBundle(SAVED_TASK_LIST_SCROLL_POSITIONS));
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        mViewMode.handleSaveInstanceState(outState);
//
//        outState.putBundle(SAVED_TASK_LIST_SCROLL_POSITIONS,
//                mTaskListScrollPositions);
//    }
//
//    @Override
//    public void startSearch() {
////        mActionBarView.expandSearch();
//    }
//
//    @Override
//    public void exitSearchMode() {
//        if (mViewMode.getMode() == ViewMode.SEARCH_RESULTS_LIST) {
//            mActivity.finish();
//        }
//    }
//
//    public void disablePagerUpdates() {
////        mPagerController.stopListening();
//    }
//

}

