/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
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

package org.dodgybits.shuffle.android.core.model;

import android.text.TextUtils;
import com.google.common.collect.Lists;
import org.dodgybits.shuffle.sync.model.ContextChangeSet;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;

import java.util.List;

public final class Task implements Entity {
    private Id mLocalId = Id.NONE;
    private String mDescription;
    private String mDetails;
    private List mContextIds = Lists.newArrayList();
    private Id mProjectId = Id.NONE;
    private long mCreatedDate;
    private long mModifiedDate;
    private long mStartDate;
    private long mDueDate;
    private String mTimezone;
    private boolean mAllDay;
    private boolean mHasAlarms;
    private boolean mActive = true;
    private boolean mDeleted;
    private Id mCalendarEventId = Id.NONE;
    // 0-indexed order within a project.
    private int mOrder;
    private boolean mComplete;
    private Id mGaeId = Id.NONE;
    private TaskChangeSet mChangeSet = TaskChangeSet.newChangeSet();

    private Task() {
    };
    
    @Override
    public final Id getLocalId() {
        return mLocalId;
    }

    public final String getDescription() {
        return mDescription;
    }

    public final String getDetails() {
        return mDetails;
    }

    public final List<Id> getContextIds() {
        return mContextIds;
    }

    public final Id getProjectId() {
        return mProjectId;
    }

    public final long getCreatedDate() {
        return mCreatedDate;
    }

    @Override
    public final long getModifiedDate() {
        return mModifiedDate;
    }

    public final long getStartDate() {
        return mStartDate;
    }

    public final long getDueDate() {
        return mDueDate;
    }

    public final String getTimezone() {
        return mTimezone;
    }

    public final boolean isAllDay() {
        return mAllDay;
    }

    public final boolean hasAlarms() {
        return mHasAlarms;
    }

    public final Id getCalendarEventId() {
        return mCalendarEventId;
    }

    public final int getOrder() {
        return mOrder;
    }

    public final boolean isComplete() {
        return mComplete;
    }

    public final String getLocalName() {
        return mDescription;
    }
    
    @Override
    public boolean isDeleted() {
        return mDeleted;
    }
    
    @Override
    public boolean isActive() {
        return mActive;
    }

    public boolean isPending() {
        long now = System.currentTimeMillis();
        return mStartDate > now;
    }

    @Override
    public Id getGaeId() {
        return mGaeId;
    }

    public TaskChangeSet getChangeSet() {
        return mChangeSet;
    }

    @Override
    public final boolean isValid() {
        if (TextUtils.isEmpty(mDescription)) {
            return false;
        }
        return true;
    }

    @Override
    public final String toString() {
        return String.format(
                "[Task id=%7$s description='%1$s' detail='%2$s' contextId=%3$s projectId=%4$s " +
                "order=%5$s complete=%6$s deleted=%8$s active=%9$s gaeId=%10$s changeSet=%11$s]",
                mDescription, mDetails, mContextIds, mProjectId,
                mOrder, mComplete, mLocalId, mDeleted, mActive,
                mGaeId, mChangeSet);
    }
    
    public static Builder newBuilder() {
        return Builder.create();
    }

    
    public static class Builder implements EntityBuilder<Task> {

        private Builder() {
        }

        private Task result;

        private static Builder create() {
            Builder builder = new Builder();
            builder.result = new Task();
            return builder;
        }

        public Id getLocalId() {
            return result.mLocalId;
        }

        public Builder setLocalId(Id value) {
            assert value != null;
            result.mLocalId = value;
            return this;
        }

        public String getDescription() {
            return result.mDescription;
        }

        public Builder setDescription(String value) {
            result.mDescription = value;
            return this;
        }

        public String getDetails() {
            return result.mDetails;
        }

        public Builder setDetails(String value) {
            result.mDetails = value;
            return this;
        }

        public List<Id> getContextIds() {
            return result.mContextIds;
        }

        public Builder setContextIds(List<Id> value) {
            assert value != null;
            result.mContextIds = value;
            return this;
        }

        public Id getProjectId() {
            return result.mProjectId;
        }

        public Builder setProjectId(Id value) {
            assert value != null;
            result.mProjectId = value;
            return this;
        }

        public long getCreatedDate() {
            return result.mCreatedDate;
        }

        public Builder setCreatedDate(long value) {
            result.mCreatedDate = value;
            return this;
        }

        public long getModifiedDate() {
            return result.mModifiedDate;
        }

        public Builder setModifiedDate(long value) {
            result.mModifiedDate = value;
            return this;
        }

        public long getStartDate() {
            return result.mStartDate;
        }

        public Builder setStartDate(long value) {
            result.mStartDate = value;
            return this;
        }

        public long getDueDate() {
            return result.mDueDate;
        }

        public Builder setDueDate(long value) {
            result.mDueDate = value;
            return this;
        }

        public String getTimezone() {
            return result.mTimezone;
        }

        public Builder setTimezone(String value) {
            result.mTimezone = value;
            return this;
        }

        public boolean isAllDay() {
            return result.mAllDay;
        }

        public Builder setAllDay(boolean value) {
            result.mAllDay = value;
            return this;
        }

        public boolean hasAlarms() {
            return result.mHasAlarms;
        }

        public Builder setHasAlarm(boolean value) {
            result.mHasAlarms = value;
            return this;
        }

        public Id getCalendarEventId() {
            return result.mCalendarEventId;
        }

        public Builder setCalendarEventId(Id value) {
            assert value != null;
            result.mCalendarEventId = value;
            return this;
        }

        public int getOrder() {
            return result.mOrder;
        }

        public Builder setOrder(int value) {
            result.mOrder = value;
            return this;
        }

        public boolean isComplete() {
            return result.mComplete;
        }

        public Builder setComplete(boolean value) {
            result.mComplete = value;
            return this;
        }

        public boolean isDeleted() {
            return result.mDeleted;
        }
        
        @Override
        public Builder setDeleted(boolean value) {
            result.mDeleted = value;
            return this;
        }
        
        public boolean isActive() {
            return result.mActive;
        }
        
        @Override
        public Builder setActive(boolean value) {
            result.mActive = value;
            return this;
        }

        public Builder setGaeId(Id gaeId) {
            result.mGaeId = gaeId;
            return this;
        }

        public Id getGaeId() {
            return result.mGaeId;
        }

        public Builder setChangeSet(TaskChangeSet changeSet) {
            result.mChangeSet = changeSet;
            return this;
        }

        public TaskChangeSet getChangeSet() {
            return result.mChangeSet;
        }

        public final boolean isInitialized() {
            return result.isValid();
        }

        public Task build() {
            if (result == null) {
                throw new IllegalStateException(
                        "build() has already been called on this Builder.");
            }
            Task returnMe = result;
            result = null;
            return returnMe;
        }
        
        public Builder mergeFrom(Task task) {
            setLocalId(task.mLocalId);
            setDescription(task.mDescription);
            setDetails(task.mDetails);
            setContextIds(task.mContextIds);
            setProjectId(task.mProjectId);
            setCreatedDate(task.mCreatedDate);
            setModifiedDate(task.mModifiedDate);
            setStartDate(task.mStartDate);
            setDueDate(task.mDueDate);
            setTimezone(task.mTimezone);
            setAllDay(task.mAllDay);
            setDeleted(task.mDeleted);
            setHasAlarm(task.mHasAlarms);
            setCalendarEventId(task.mCalendarEventId);
            setOrder(task.mOrder);
            setComplete(task.mComplete);
            setDeleted(task.mDeleted);
            setActive(task.mActive);
            setGaeId(task.mGaeId);
            setChangeSet(task.mChangeSet);
            return this;
        }


    }

}
