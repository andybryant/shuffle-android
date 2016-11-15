package org.dodgybits.shuffle.android.core.model.protocol;

import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.dto.ShuffleProtos.Project.Builder;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;

import static org.dodgybits.shuffle.android.core.model.protocol.ProtocolUtil.toDate;

public class ProjectProtocolTranslator implements EntityProtocolTranslator<Project, org.dodgybits.shuffle.dto.ShuffleProtos.Project> {

    private EntityDirectory<Context> mContextDirectory;

    public ProjectProtocolTranslator(EntityDirectory<Context> contextDirectory) {
        mContextDirectory = contextDirectory;
    }
    
    public org.dodgybits.shuffle.dto.ShuffleProtos.Project toMessage(Project project) {
        Builder builder = org.dodgybits.shuffle.dto.ShuffleProtos.Project.newBuilder();
        builder
            .setId(project.getLocalId().getId())
            .setName((project.getName()))
            .setParallel(project.isParallel())
            .setActive(project.isActive())
            .setDeleted(project.isDeleted())
            .setOrder(project.getOrder())
            .setChangeSet(project.getChangeSet().getChangeSet());
        if (project.getModifiedDate() > 0L) {
            builder.setModified(toDate(project.getModifiedDate()));
        }

        Id gaeId = project.getGaeId();
        if (gaeId.isInitialised()) {
            builder.setGaeEntityId(gaeId.getId());
        }

        final Id defaultContextId = project.getDefaultContextId();
        if (defaultContextId.isInitialised()) {
            builder.setDefaultContextId(defaultContextId.getId());
        }

        return builder.build();
    }

    public Project fromMessage(
            org.dodgybits.shuffle.dto.ShuffleProtos.Project dto) {
        Project.Builder builder = Project.newBuilder();
        builder
            .setLocalId(Id.create(dto.getId()))
            .setName(dto.getName())
            .setModifiedDate(ProtocolUtil.fromDate(dto.getModified()))
            .setParallel(dto.getParallel());

        if (dto.hasGaeEntityId()) {
            builder.setGaeId(Id.create(dto.getGaeEntityId()));
        }
        if (dto.hasChangeSet()) {
            builder.setChangeSet(ProjectChangeSet.fromChangeSet(dto.getChangeSet()));
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
        if (dto.hasOrder()) {
            builder.setOrder(dto.getOrder());
        }

        if (dto.hasDefaultContextId()) {
            Id defaultContextId = Id.create(dto.getDefaultContextId());
            Context context = mContextDirectory.findById(defaultContextId);

            // it's possible the default context no longer exists so check for it
            builder.setDefaultContextId(context == null ? Id.NONE : context.getLocalId());
        }

        return builder.build();
    }      
    
}
