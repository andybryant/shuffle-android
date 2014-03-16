package org.dodgybits.shuffle.android.core.model.protocol;

import com.google.common.collect.Lists;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.dto.ShuffleProtos.Task.Builder;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;

import java.util.List;

public class TaskProtocolTranslator implements EntityProtocolTranslator<Task, org.dodgybits.shuffle.dto.ShuffleProtos.Task> {

    private final EntityDirectory<Context> mContextDirectory;
    private final EntityDirectory<Project> mProjectDirectory;
    
    public TaskProtocolTranslator(
            EntityDirectory<Context> contextDirectory,
            EntityDirectory<Project> projectDirectory) {
        mContextDirectory = contextDirectory;
        mProjectDirectory = projectDirectory;
    }

    public org.dodgybits.shuffle.dto.ShuffleProtos.Task toMessage(Task task) {
        Builder builder = org.dodgybits.shuffle.dto.ShuffleProtos.Task.newBuilder();
        builder
            .setId(task.getLocalId().getId())
            .setDescription(task.getDescription())
            .setCreated(ProtocolUtil.toDate(task.getCreatedDate()))
            .setModified(ProtocolUtil.toDate(task.getModifiedDate()))
            .setStartDate(ProtocolUtil.toDate(task.getStartDate()))
            .setDueDate(ProtocolUtil.toDate(task.getDueDate()))
            .setAllDay(task.isAllDay())
            .setOrder(task.getOrder())
            .setComplete(task.isComplete())
            .setActive(task.isActive())
            .setDeleted(task.isDeleted())
            .setChangeSet(task.getChangeSet().getChangeSet());

        Id gaeId = task.getGaeId();
        if (gaeId.isInitialised()) {
            builder.setGaeEntityId(gaeId.getId());
        }

        final String details = task.getDetails();
        if (details != null) {
            builder.setDetails(details);
        }

        boolean first = true;
        for (Id contextId : task.getContextIds()) {
            long id = contextId.getId();
            if (first) {
                // add first as contextId for backward compatibility
                builder.setContextId(id);
                first = false;
            }
            builder.addContextIds(id);
        }
        
        final Id projectId = task.getProjectId();
        if (projectId.isInitialised()) {
            builder.setProjectId(projectId.getId());
        }

        final String timezone = task.getTimezone();
        if (timezone != null) {
            builder.setTimezone(timezone);
        }

        final Id calEventId = task.getCalendarEventId();
        if (calEventId.isInitialised()) {
            builder.setCalEventId(calEventId.getId());
        }

        return builder.build();
    }

    public Task fromMessage(
            org.dodgybits.shuffle.dto.ShuffleProtos.Task dto) {
        Task.Builder builder = Task.newBuilder();
        builder
            .setLocalId(Id.create(dto.getId()))
            .setDescription(dto.getDescription())
            .setDetails(dto.getDetails())
            .setCreatedDate(ProtocolUtil.fromDate(dto.getCreated()))
            .setModifiedDate(ProtocolUtil.fromDate(dto.getModified()))
            .setStartDate(ProtocolUtil.fromDate(dto.getStartDate()))
            .setDueDate(ProtocolUtil.fromDate(dto.getDueDate()))
            .setTimezone(dto.getTimezone())
            .setAllDay(dto.getAllDay())
            .setHasAlarm(false)
            .setOrder(dto.getOrder())
            .setComplete(dto.getComplete());

        if (dto.hasGaeEntityId()) {
            builder.setGaeId(Id.create(dto.getGaeEntityId()));
        }
        if (dto.hasChangeSet()) {
            builder.setChangeSet(TaskChangeSet.fromChangeSet(dto.getChangeSet()));
        }

        if (dto.hasActive()) {
            builder.setActive(dto.getActive());
        } else {
            builder.setActive(true);
        }

        if (dto.hasDeleted()) {
            builder.setDeleted(dto.getDeleted());
        } else {
            builder.setDeleted(false);
        }

        List<Id> contextIds = Lists.newArrayList();
        if (dto.getContextIdsCount() > 0) {
            for (long id : dto.getContextIdsList()) {
                Id contextId = Id.create(id);
                Context context = mContextDirectory.findById(contextId);
                if (context != null) {
                    contextIds.add(context.getLocalId());
                }
            }
        } else if (dto.hasContextId()) {
            Id contextId = Id.create(dto.getContextId());
            Context context = mContextDirectory.findById(contextId);
            if (context != null) {
                contextIds.add(context.getLocalId());
            }
        }
        builder.setContextIds(contextIds);

        if (dto.hasProjectId()) {
            Id projectId = Id.create(dto.getProjectId());
            Project project = mProjectDirectory.findById(projectId);
            builder.setProjectId(project == null ? Id.NONE : project.getLocalId());
        }
        
        if (dto.hasCalEventId()) {
            builder.setCalendarEventId(Id.create(dto.getCalEventId()));
        }

        return builder.build();
    }    
    
}
