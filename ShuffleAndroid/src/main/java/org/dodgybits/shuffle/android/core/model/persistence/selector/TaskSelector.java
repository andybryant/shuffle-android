package org.dodgybits.shuffle.android.core.model.persistence.selector;

import android.content.Context;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.Log;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.model.ListSettings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.dodgybits.shuffle.android.core.model.persistence.selector.Flag.*;

public class TaskSelector extends AbstractEntitySelector<TaskSelector> implements Parcelable {
    private static final String TAG = "TaskSelector";
    private static final String[] UNDEFINED_ARGS = new String[] {};

    private ListQuery mListQuery;
    private Id mProjectId = Id.NONE;
    private Id mContextId = Id.NONE;
    private Flag mComplete = ignored;
    private Flag mPending = ignored;

    private String mSelection = null;
    private String[] mSelectionArgs = UNDEFINED_ARGS;
    private String mSearchQuery = null;

    private TaskSelector() {
    }
    
    public final ListQuery getListQuery() {
        return mListQuery;
    }
    
    public final Id getProjectId() {
        return mProjectId;
    }

    public final Id getContextId() {
        return mContextId;
    }

    public final Flag getComplete() {
        return mComplete;
    }

    public final Flag getPending() {
        return mPending;
    }

    public final String getSearchQuery() {
        return mSearchQuery;
    }

    @Override
    public Uri getContentUri() {
        return TaskProvider.Tasks.CONTENT_URI;
    }

    @Override
    public final String getSelection(android.content.Context context) {
        if (mSelection == null) {
            List<String> expressions = getSelectionExpressions(context);
            mSelection = StringUtils.join(expressions, " AND ");
            Log.d(TAG, mSelection);
        }
        return mSelection;
    }

    @Override
    protected List<String> getSelectionExpressions(android.content.Context context) {
        List<String> expressions = super.getSelectionExpressions(context);
        
        if (mListQuery != null) {
            expressions.add(predefinedSelection(context));
        }
        
        addActiveExpression(expressions);
        addDeletedExpression(expressions);
        addPendingExpression(expressions);

        addIdCheckExpression(expressions, TaskProvider.Tasks.PROJECT_ID, mProjectId);
        addContextExpression(expressions);
        addFlagExpression(expressions, TaskProvider.Tasks.COMPLETE, mComplete);

        return expressions;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getListQuery().name());
        dest.writeLong(getContextId().getId());
        dest.writeLong(getProjectId().getId());
        dest.writeString(getSearchQuery());
    }

    public static TaskSelector fromLocation(Context context, Location location) {
        TaskSelector.Builder builder = TaskSelector.newBuilder()
                .setListQuery(location.getListQuery())
                .setSearchQuery(location.getSearchQuery());
        if (location.getListQuery() == ListQuery.project) {
            builder.setProjectId(location.getProjectId());
        } else if (location.getListQuery() == ListQuery.context) {
            builder.setContextId(location.getContextId());
        }
        ListSettings settings = ListSettingsCache.findSettings(location.getListQuery());
        builder.applyListPreferences(context, settings);
        return builder.build();
    }

    public static final Parcelable.Creator<TaskSelector> CREATOR
        = new Parcelable.Creator<TaskSelector>() {

        @Override
        public TaskSelector createFromParcel(Parcel source) {
            String queryName = source.readString();
            long contextId = source.readLong();
            long projectId = source.readLong();
            String searchQuery = source.readString();
            ListQuery query = ListQuery.valueOf(queryName);
            TaskSelector.Builder builder = TaskSelector.newBuilder().setListQuery(query);
            if (contextId != 0L) {
                builder.setContextId(Id.create(contextId));
            }
            if (projectId != 0L) {
                builder.setProjectId(Id.create(projectId));
            }
            builder.setSearchQuery(searchQuery);

            return builder.build();
        }

        @Override
        public TaskSelector[] newArray(int size) {
            return new TaskSelector[size];
        }
    };


    private void addActiveExpression(List<String> expressions) {
        if (mActive == yes) {
            // A task is active if it is active and
            // either has no project or a project that is active and
            // either it has no contexts or at least one context is active.
            String expression = "(task.active = 1 " +
            		"AND (projectId is null OR projectId IN (select p._id from project p where p.active = 1)) " +
            		"AND (" +
                    "     ((select count(*) from taskContext tc where tc.taskId = task._id) = 0) OR " +
                    "     ((select count(*) from taskContext tc, context c where " +
                    "         tc.taskId = task._id and tc.contextId = c._id and c.active = 1) > 0)" +
                    "    )" +
            		")";
            expressions.add(expression);
        } else if (mActive == no) {
            // task is inactive if it is inactive or project is inactive or all contexts are inactive
            String expression = getInactiveExpression();
            expressions.add(expression);
        }
    }

    private String getInactiveExpression() {
        return "(task.active = 0 " +
                "OR (projectId is not null AND projectId IN (select p._id from project p where p.active = 0)) " +
                "OR (" +
                "     ((select count(*) from taskContext tc where tc.taskId = task._id) > 0) AND " +
                "     ((select count(*) from taskContext tc, context c where " +
                "         tc.taskId = task._id and tc.contextId = c._id and c.active = 1) = 0)" +
                "    )" +
                ")";
    }
    
    private void addDeletedExpression(List<String> expressions) {
        if (mDeleted == no) {
            // task is not deleted if it is not deleted and project is not deleted
            String expression = "(task.deleted = 0 " +
                "AND (projectId is null OR projectId IN (select p._id from project p where p.deleted = 0)) " +
                ")";
            expressions.add(expression);
        }
    }

    private void addPendingExpression(List<String> expressions) {
        long now = System.currentTimeMillis();
        if (mPending == yes) {
            String expression = "(start > " + now + ")";
            expressions.add(expression);
        } else if (mPending == no) {
            String expression = "(start <= " + now + ")";
            expressions.add(expression);
        }
    }

    private void addContextExpression(List<String> expressions) {
        if (mContextId.isInitialised()) {
            String expression = "(task._id IN (select tc.taskId from taskContext tc where tc.contextId = ?))";
            expressions.add(expression);
        }
    }

    private String predefinedSelection(android.content.Context context) {
        String result;
        long now = System.currentTimeMillis();
        switch (mListQuery) {
            case inbox:
                result = "(projectId is null AND (select count(*) from taskContext tc where tc.taskId = task._id) = 0)";
                break;

            case nextTasks:
                result = "((complete = 0) AND " +
                    "   (start < " + now + ") AND " +
                    "   ((projectId is null) OR " +
                    "   (projectId IN (select p._id from project p where p.parallel = 1)) OR " +
                    "   (task._id = (select t2._id FROM task t2 WHERE " +
                    "      t2.projectId = task.projectId AND t2.complete = 0 AND " +
                    "      t2.deleted = 0 " +
                    "      ORDER BY displayOrder ASC limit 1))" +
                    "))";
                break;

            case dueTasks:
                long startMS = 0L;
                long endOfToday = getEndDate();
                long endOfTomorrow = endOfToday + DateUtils.DAY_IN_MILLIS;
                result = "(due > " + startMS + ")" +
                        " AND ( (due < " + endOfToday + ") OR" +
                        "( allDay = 1 AND due < " + endOfTomorrow + " ))";
                break;

            case project:
            case context:
                // by default show all results (completely customizable)
                result = "(1 == 1)";
                break;

            case deferred:
                result = "((complete = 0) AND (start > " + now + "))";
                break;

            case deleted:
                result = "(deleted = 1)";
                break;

            case search:
                StringBuilder builder = new StringBuilder();
                builder.append("(description LIKE ");
                DatabaseUtils.appendEscapedSQLString(builder, '%' + mSearchQuery + '%');
                builder.append(" OR details LIKE ");
                DatabaseUtils.appendEscapedSQLString(builder, '%' + mSearchQuery + '%');
                builder.append(')');
                result = builder.toString();
                break;

            default:
                throw new RuntimeException("Unknown predefined selection " + mListQuery);
        }
        
        return result;
    }
    
    private long getEndDate() {
        long endMS = 0L;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        switch (mListQuery) {
        case dueTasks:
            cal.add(Calendar.MONTH, 1);
            endMS = cal.getTimeInMillis();
            break;
        }
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Due date ends " + endMS);
        }
        return endMS;
    }

    @Override
    public final String[] getSelectionArgs() {
        if (mSelectionArgs == UNDEFINED_ARGS) {
            List<String> args = new ArrayList<String>();
            addIdArg(args, mProjectId);
            addIdArg(args, mContextId);
            mSelectionArgs = args.size() > 0 ? args.toArray(new String[0]): null;
        }
        return mSelectionArgs;
    }

    @Override
    public String getSortOrder() {
        return "task." + TaskProvider.Tasks.DISPLAY_ORDER + " ASC";
    }

    @Override
    public Builder builderFrom() {
        return newBuilder().mergeFrom(this);
    }

    @Override
    public final String toString() {
        return String.format(
                "[TaskSelector query=%1$s project=%2$s contexts=%3$s " +
                "complete=%4$s sortOrder=%5$s active=%6$s deleted=%7$s pending=%8$s searchQuery=%9$s]",
                mListQuery, mProjectId, mContextId, mComplete,
                mSortOrder, mActive, mDeleted, mPending, mSearchQuery);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskSelector that = (TaskSelector) o;

        if (mComplete != that.mComplete) return false;
        if (!mContextId.equals(that.mContextId)) return false;
        if (mListQuery != that.mListQuery) return false;
        if (mPending != that.mPending) return false;
        if (!mProjectId.equals(that.mProjectId)) return false;
        if (mSearchQuery != null ? !mSearchQuery.equals(that.mSearchQuery) : that.mSearchQuery != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mListQuery.hashCode();
        result = 31 * result + mProjectId.hashCode();
        result = 31 * result + mContextId.hashCode();
        result = 31 * result + mComplete.hashCode();
        result = 31 * result + mPending.hashCode();
        result = 31 * result + (mSearchQuery != null ? mSearchQuery.hashCode() : 0);
        return result;
    }

    public static Builder newBuilder() {
        return Builder.create();
    }
 
    
    public static class Builder extends AbstractBuilder<TaskSelector> {

        private Builder() {
        }

        private static Builder create() {
            Builder builder = new Builder();
            builder.mResult = new TaskSelector();
            return builder;
        }
        
        public ListQuery getListQuery() {
            return mResult.mListQuery;
        }
        
        public Builder setListQuery(ListQuery value) {
            mResult.mListQuery = value;
            return this;
        }

        public Id getProjectId() {
            return mResult.mProjectId;
        }

        public Builder setProjectId(Id value) {
            mResult.mProjectId = value;
            return this;
        }
        
        public Id getContextId() {
            return mResult.mContextId;
        }

        public Builder setContextId(Id value) {
            mResult.mContextId = value;
            return this;
        }
                
        public Flag getComplete() {
            return mResult.mComplete;
        }
        
        public Builder setComplete(Flag value) {
            mResult.mComplete = value;
            return this;
        }

        public Flag getPending() {
            return mResult.mPending;
        }

        public Builder setPending(Flag value) {
            mResult.mPending = value;
            return this;
        }

        public String getSearchQuery() {
            return mResult.mSearchQuery;
        }

        public Builder setSearchQuery(String query) {
            mResult.mSearchQuery = query;
            return this;
        }
        
        public Builder mergeFrom(TaskSelector selector) {
            super.mergeFrom(selector);
            setSearchQuery(selector.mSearchQuery);
            setListQuery(selector.mListQuery);
            setProjectId(selector.mProjectId);
            setContextId(selector.mContextId);
            setComplete(selector.mComplete);
            setPending(selector.mPending);

            return this;
        }

        public Builder applyListPreferences(android.content.Context context, ListSettings settings) {
            super.applyListPreferences(context, settings);

            setComplete(settings.getCompleted(context));
            setPending(settings.getDefaultPending());

            return this;
        }

    }

}
