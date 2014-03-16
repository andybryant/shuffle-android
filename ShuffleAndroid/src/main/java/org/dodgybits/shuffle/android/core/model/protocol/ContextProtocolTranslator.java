package org.dodgybits.shuffle.android.core.model.protocol;

import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.dto.ShuffleProtos.Context.Builder;
import org.dodgybits.shuffle.sync.model.ContextChangeSet;

public class ContextProtocolTranslator  implements EntityProtocolTranslator<Context , org.dodgybits.shuffle.dto.ShuffleProtos.Context>{

    public org.dodgybits.shuffle.dto.ShuffleProtos.Context toMessage(Context context) {
        Builder builder = org.dodgybits.shuffle.dto.ShuffleProtos.Context.newBuilder();
        builder
            .setId(context.getLocalId().getId())
            .setName((context.getName()))
            .setModified(ProtocolUtil.toDate(context.getModifiedDate()))
            .setColourIndex(context.getColourIndex())
            .setActive(context.isActive())
            .setDeleted(context.isDeleted())
            .setChangeSet(context.getChangeSet().getChangeSet());

        Id gaeId = context.getGaeId();
        if (gaeId.isInitialised()) {
            builder.setGaeEntityId(gaeId.getId());
        }

        final String iconName = context.getIconName();
        if (iconName != null) {
            builder.setIcon(iconName);
        }
        
        return builder.build();
    }

    public Context fromMessage(
            org.dodgybits.shuffle.dto.ShuffleProtos.Context dto) {
        Context.Builder builder = Context.newBuilder();
        builder
            .setLocalId(Id.create(dto.getId()))
            .setName(dto.getName())
            .setModifiedDate(ProtocolUtil.fromDate(dto.getModified()))
            .setColourIndex(dto.getColourIndex());

        if (dto.hasGaeEntityId()) {
            builder.setGaeId(Id.create(dto.getGaeEntityId()));
        }
        if (dto.hasChangeSet()) {
            builder.setChangeSet(ContextChangeSet.fromChangeSet(dto.getChangeSet()));
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

        if (dto.hasIcon()) {
            builder.setIconName(dto.getIcon());
        }

        return builder.build();
    }      

}
