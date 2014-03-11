package org.dodgybits.shuffle.android.core.model.encoding;

import android.os.Bundle;
import com.google.inject.Singleton;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Project.Builder;
import org.dodgybits.shuffle.android.core.util.BundleUtils;
import org.dodgybits.shuffle.sync.model.ProjectChangeSet;

import static android.provider.BaseColumns._ID;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.*;

@Singleton
public class ProjectEncoder implements EntityEncoder<Project> {

    @Override
    public void save(Bundle icicle, Project project) {
        BundleUtils.putId(icicle, _ID, project.getLocalId());
        icicle.putLong(MODIFIED_DATE, project.getModifiedDate());
        icicle.putBoolean(DELETED, project.isDeleted());
        icicle.putBoolean(ACTIVE, project.isActive());

        icicle.putString(NAME, project.getName());
        BundleUtils.putId(icicle, DEFAULT_CONTEXT_ID, project.getDefaultContextId());
        icicle.putBoolean(ARCHIVED, project.isArchived());
        icicle.putBoolean(PARALLEL, project.isParallel());
        BundleUtils.putId(icicle, GAE_ID, project.getGaeId());
        icicle.putLong(CHANGE_SET, project.getChangeSet().getChangeSet());
    }
    
    @Override
    public Project restore(Bundle icicle) {
        if (icicle == null) return null;

        Builder builder = Project.newBuilder();
        builder.setLocalId(BundleUtils.getId(icicle, _ID));
        builder.setModifiedDate(icicle.getLong(MODIFIED_DATE, 0L));
        builder.setDeleted(icicle.getBoolean(DELETED));
        builder.setActive(icicle.getBoolean(ACTIVE));

        builder.setName(icicle.getString(NAME));
        builder.setDefaultContextId(BundleUtils.getId(icicle, DEFAULT_CONTEXT_ID));
        builder.setArchived(icicle.getBoolean(ARCHIVED));
        builder.setParallel(icicle.getBoolean(PARALLEL));
        builder.setGaeId(BundleUtils.getId(icicle, GAE_ID));
        builder.setChangeSet(ProjectChangeSet.fromChangeSet(icicle.getLong(CHANGE_SET)));

        return builder.build();
    }
    
}
