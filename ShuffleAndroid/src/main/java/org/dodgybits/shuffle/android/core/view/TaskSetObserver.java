package org.dodgybits.shuffle.android.core.view;

/**
 * Interface for classes that want to respond to changes in task sets.  A task set
 * is a list of tasks selected by the user to perform an action on. The user could select
 * five tasks and delete them. The five tasks form a set. Constructing such a set
 * involves many user actions: tapping on multiple checkboxes. This interface allows the class to
 * listen to such user actions.
 */
public interface TaskSetObserver {

    /**
     * Called when the selection set becomes empty.
     */
    void onSetEmpty();

    /**
     * Handle when the selection set is populated with some items. The observer should not make any
     * modifications to the set while handling this event.
     */
    void onSetPopulated(TaskSelectionSet set);

    /**
     * Handle when the selection set gets an element added or removed. The observer should not
     * make any modifications to the set while handling this event.
     */
    void onSetChanged(TaskSelectionSet set);
}
