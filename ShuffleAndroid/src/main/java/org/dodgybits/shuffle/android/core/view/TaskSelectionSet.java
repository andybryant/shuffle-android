package org.dodgybits.shuffle.android.core.view;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Task;

import java.util.*;

/**
 * A simple thread-safe wrapper over a set of tasks representing a
 * selection set (e.g. in a task list). This class dispatches changes
 * when the set goes empty, and when it becomes unempty. For simplicity, this
 * class <b>does not allow modifications</b> to the collection in observers when
 * responding to change events.
 */
public class TaskSelectionSet implements Parcelable {
    public static final ClassLoaderCreator<TaskSelectionSet> CREATOR =
            new ClassLoaderCreator<TaskSelectionSet>() {

        @Override
        public TaskSelectionSet createFromParcel(Parcel source) {
            return new TaskSelectionSet(source, null);
        }

        @Override
        public TaskSelectionSet createFromParcel(Parcel source, ClassLoader loader) {
            return new TaskSelectionSet(source, loader);
        }

        @Override
        public TaskSelectionSet[] newArray(int size) {
            return new TaskSelectionSet[size];
        }

    };

    private final Object mLock = new Object();
    /** Map of task ID to task objects. Every selected task is here. */
    private final HashMap<Id, Task> mInternalMap = new HashMap<>();
    /** Map of Task URI to Task ID. */
    private final BiMap<String, Id> mTaskUriToIdMap = HashBiMap.create();
    /** All objects that are interested in changes to the selected set. */
    @VisibleForTesting
    final ArrayList<TaskSetObserver> mObservers = new ArrayList<TaskSetObserver>();

    /**
     * Create a new object,
     */
    public TaskSelectionSet() {
        // Do nothing.
    }

    private TaskSelectionSet(Parcel source, ClassLoader loader) {
        Parcelable[] tasks = source.readParcelableArray(loader);
        for (Parcelable parceled : tasks) {
            Task task = (Task) parceled;
            put(task.getLocalId(), task);
        }
    }

    /**
     * Registers an observer to listen for interesting changes on this set.
     *
     * @param observer the observer to register.
     */
    public void addObserver(TaskSetObserver observer) {
        synchronized (mLock) {
            mObservers.add(observer);
        }
    }

    /**
     * Clear the selected set entirely.
     */
    public void clear() {
        synchronized (mLock) {
            boolean initiallyNotEmpty = !mInternalMap.isEmpty();
            mInternalMap.clear();
            mTaskUriToIdMap.clear();

            if (mInternalMap.isEmpty() && initiallyNotEmpty) {
                ArrayList<TaskSetObserver> observersCopy = Lists.newArrayList(mObservers);
                dispatchOnChange(observersCopy);
                dispatchOnEmpty(observersCopy);
            }
        }
    }

    /**
     * Returns true if the given key exists in the task selection set. This assumes
     * the internal representation holds task.id values.
     * @param key the id of the task
     * @return true if the key exists in this selected set.
     */
    private boolean containsKey(Id key) {
        synchronized (mLock) {
            return mInternalMap.containsKey(key);
        }
    }

    /**
     * Returns true if the given task is stored in the selection set.
     * @param task
     * @return true if the task exists in the selected set.
     */
    public boolean contains(Task task) {
        synchronized (mLock) {
            return containsKey(task.getLocalId());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void dispatchOnBecomeUnempty(ArrayList<TaskSetObserver> observers) {
        synchronized (mLock) {
            for (TaskSetObserver observer : observers) {
                observer.onSetPopulated(this);
            }
        }
    }

    private void dispatchOnChange(ArrayList<TaskSetObserver> observers) {
        synchronized (mLock) {
            // Copy observers so that they may unregister themselves as listeners on
            // event handling.
            for (TaskSetObserver observer : observers) {
                observer.onSetChanged(this);
            }
        }
    }

    private void dispatchOnEmpty(ArrayList<TaskSetObserver> observers) {
        synchronized (mLock) {
            for (TaskSetObserver observer : observers) {
                observer.onSetEmpty();
            }
        }
    }

    /**
     * Is this task set empty?
     * @return true if the task selection set is empty. False otherwise.
     */
    public boolean isEmpty() {
        synchronized (mLock) {
            return mInternalMap.isEmpty();
        }
    }

    private void put(Id id, Task info) {
        synchronized (mLock) {
            final boolean initiallyEmpty = mInternalMap.isEmpty();
            mInternalMap.put(id, info);
            mTaskUriToIdMap.put(info.uri.toString(), id);

            final ArrayList<TaskSetObserver> observersCopy = Lists.newArrayList(mObservers);
            dispatchOnChange(observersCopy);
            if (initiallyEmpty) {
                dispatchOnBecomeUnempty(observersCopy);
            }
        }
    }

    /** @see java.util.HashMap#remove */
    private void remove(Id id) {
        synchronized (mLock) {
            removeAll(Collections.singleton(id));
        }
    }

    private void removeAll(Collection<Id> ids) {
        synchronized (mLock) {
            final boolean initiallyNotEmpty = !mInternalMap.isEmpty();

            final BiMap<Id, String> inverseMap = mTaskUriToIdMap.inverse();

            for (Id id : ids) {
                mInternalMap.remove(id);
                inverseMap.remove(id);
            }

            ArrayList<TaskSetObserver> observersCopy = Lists.newArrayList(mObservers);
            dispatchOnChange(observersCopy);
            if (mInternalMap.isEmpty() && initiallyNotEmpty) {
                dispatchOnEmpty(observersCopy);
            }
        }
    }

    /**
     * Unregisters an observer for change events.
     *
     * @param observer the observer to unregister.
     */
    public void removeObserver(TaskSetObserver observer) {
        synchronized (mLock) {
            mObservers.remove(observer);
        }
    }

    /**
     * Returns the number of tasks that are currently selected
     * @return the number of selected tasks.
     */
    public int size() {
        synchronized (mLock) {
            return mInternalMap.size();
        }
    }

    /**
     * Toggles the existence of the given task in the selection set. If the task is
     * currently selected, it is deselected. If it doesn't exist in the selection set, then it is
     * selected.
     * @param task
     */
    public void toggle(Task task) {
        final Id taskId = task.getLocalId();
        if (containsKey(taskId)) {
            // We must not do anything with view here.
            remove(taskId);
        } else {
            put(taskId, task);
        }
    }

    /** @see java.util.HashMap#values */
    public Collection<Task> values() {
        synchronized (mLock) {
            return mInternalMap.values();
        }
    }

    /** @see java.util.HashMap#keySet() */
    public Set<Id> keySet() {
        synchronized (mLock) {
            return mInternalMap.keySet();
        }
    }

    /**
     * Puts all tasks given in the input argument into the selection set. If there are
     * any listeners they are notified once after adding <em>all</em> tasks to the selection
     * set.
     * @see java.util.HashMap#putAll(java.util.Map)
     */
    public void putAll(TaskSelectionSet other) {
        if (other == null) {
            return;
        }

        final boolean initiallyEmpty = mInternalMap.isEmpty();
        mInternalMap.putAll(other.mInternalMap);

        final ArrayList<TaskSetObserver> observersCopy = Lists.newArrayList(mObservers);
        dispatchOnChange(observersCopy);
        if (initiallyEmpty) {
            dispatchOnBecomeUnempty(observersCopy);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Task[] values = values().toArray(new Task[size()]);
        dest.writeParcelableArray(values, flags);
    }

    /**
     * @param deletedRows an arraylist of task IDs which have been deleted.
     */
    public void delete(ArrayList<Id> deletedRows) {
        for (Id Id : deletedRows) {
            remove(id);
        }
    }

    /**
     * Iterates through a cursor of tasks and ensures that the current set is present
     * within the result set denoted by the cursor. Any tasks not foun in the result set
     * is removed from the collection.
     */
    public void validateAgainstCursor(TaskCursor cursor) {
        synchronized (mLock) {
            if (isEmpty()) {
                return;
            }

            if (cursor == null) {
                clear();
                return;
            }

            // First ask the TaskCursor for the list of tasks that have been deleted
            final Set<String> deletedTasks = cursor.getDeletedItems();
            // For each of the uris in the deleted set, add the task id to the
            // itemsToRemoveFromBatch set.
            final Set<Id> itemsToRemoveFromBatch = Sets.newHashSet();
            for (String taskUri : deletedTasks) {
                final Id taskId = mTaskUriToIdMap.get(taskUri);
                if (taskId != null) {
                    itemsToRemoveFromBatch.add(taskId);
                }
            }

            // Get the set of the items that had been in the batch
            final Set<Id> batchTaskToCheck = new HashSet<Id>(keySet());

            // Remove all of the items that we know are missing.  This will leave the items where
            // we need to check for existence in the cursor
            batchTaskToCheck.removeAll(itemsToRemoveFromBatch);
            // At this point batchTaskToCheck contains the task ids for the
            // tasks that had been in the batch selection, with the items we know have been
            // deleted removed.

            // This set contains the task ids that are in the task cursor
            final Set<Id> cursorTaskIds = cursor.getTaskIds();

            // We want to remove all of the valid items that are in the task cursor, from
            // the batchTasks to check.  The goal is after this block, anything remaining
            // would be items that don't exist in the task cursor anymore.
            if (!batchTaskToCheck.isEmpty() && cursorTaskIds != null) {
                batchTaskToCheck.removeAll(cursorTaskIds);
            }

            // At this point any of the item that are remaining in the batchTaskToCheck set
            // are to be removed from the selected task set
            itemsToRemoveFromBatch.addAll(batchTaskToCheck);

            removeAll(itemsToRemoveFromBatch);
        }
    }

    @Override
    public String toString() {
        synchronized (mLock) {
            return String.format("%s:%s", super.toString(), mInternalMap);
        }
    }
}
