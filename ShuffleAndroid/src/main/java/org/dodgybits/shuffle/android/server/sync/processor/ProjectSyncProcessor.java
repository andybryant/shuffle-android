package org.dodgybits.shuffle.android.server.sync.processor;

import android.database.Cursor;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.protocol.EntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.HashEntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.MutableEntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.ProjectProtocolTranslator;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.dto.ShuffleProtos;

import java.util.*;

public class ProjectSyncProcessor {
    private static final String TAG = "ProjectSyncProcessor";

    @Inject
    private android.content.Context mContext;

    @Inject
    private ProjectPersister mProjectPersister;

    public EntityDirectory<Project> processProjects(ShuffleProtos.SyncResponse response,
                                                    EntityDirectory<Context> contextLocator) {
        ProjectProtocolTranslator translator = new ProjectProtocolTranslator(contextLocator);
        // build up the locator and list of new contacts
        MutableEntityDirectory<Project> projectLocator = new HashEntityDirectory<Project>();

        addExistingProjects(projectLocator);
        addNewProjects(response, translator, projectLocator);
        updateModifiedProjects(response, translator, projectLocator);
        updateLocallyNewProjects(response, projectLocator);
        deleteMissingProjects(response);
        clearChangeSets();

        return projectLocator;
    }

    private void addExistingProjects(MutableEntityDirectory<Project> projectLocator) {
        // preload with existing device projects
        Cursor cursor = mContext.getContentResolver().query(
                ProjectProvider.Projects.CONTENT_URI, ProjectProvider.Projects.FULL_PROJECTION,
                null, null, null);
        while (cursor.moveToNext()) {
            org.dodgybits.shuffle.android.core.model.Project project = mProjectPersister.read(cursor);
            if (project.getGaeId().isInitialised()) {
                projectLocator.addItem(project.getGaeId(), project.getName(), project);
            }
        }
        cursor.close();
    }

    private void addNewProjects(ShuffleProtos.SyncResponse response,
                                ProjectProtocolTranslator translator,
                                MutableEntityDirectory<Project> projectLocator) {
        List<ShuffleProtos.Project> protoProjects = response.getNewProjectsList();
        List<Project> newProjects = new ArrayList<Project>();
        for (ShuffleProtos.Project protoProject : protoProjects) {
            Project project = translator.fromMessage(protoProject);
            newProjects.add(project);
        }
        Log.d(TAG, "Added " + newProjects.size() + " new projects from server");
        mProjectPersister.bulkInsert(newProjects);

        // update locator with newly saved projects now we have local ids
        for (ShuffleProtos.Project protoProject : protoProjects) {
            Id projectId = Id.create(protoProject.getGaeEntityId());
            Project updatedProject = mProjectPersister.findByGaeId(projectId);
            projectLocator.addItem(projectId, updatedProject.getName(), updatedProject);
        }
    }

    private void updateModifiedProjects(ShuffleProtos.SyncResponse response,
                                        ProjectProtocolTranslator translator,
                                        MutableEntityDirectory<Project> projectLocator) {
        List<ShuffleProtos.Project> protoProjects = response.getModifiedProjectsList();
        for (ShuffleProtos.Project protoProject : protoProjects) {
            Project project = translator.fromMessage(protoProject);
            Id projectId = Id.create(protoProject.getGaeEntityId());
            String projectName = project.getName();
            projectLocator.addItem(projectId, projectName, project);
            mProjectPersister.update(project);
        }
        Log.d(TAG, "Updated " + protoProjects.size() + " modified tasks");
    }

    private void updateLocallyNewProjects(ShuffleProtos.SyncResponse response,
                                          MutableEntityDirectory<Project> projectLocator) {
        List<ShuffleProtos.Identifiers> pairsList = response.getAddedProjectIdsList();
        for (ShuffleProtos.Identifiers pair : pairsList) {
            Id localId = Id.create(pair.getDeviceEntityId());
            Id gaeId = Id.create(pair.getGaeEntityId());
            mProjectPersister.updateGaeId(localId, gaeId);
            // need to add project to locator as it's possible server
            // has returned new tasks referencing this
            Project updatedProject = mProjectPersister.findById(localId);
            projectLocator.addItem(gaeId, updatedProject.getName(), updatedProject);

        }
        Log.d(TAG, "Added gaeId for " + pairsList.size() + " new projects not previously on server");
    }

    private void deleteMissingProjects(ShuffleProtos.SyncResponse response) {
        List<Long> idsList = response.getDeletedProjectGaeIdsList();
        for (long gaeId : idsList) {
            mProjectPersister.deletePermanentlyByGaeId(Id.create(gaeId));
        }
        Log.w(TAG, "Permanently deleted " + idsList.size() + " missing projects");
    }

    private void clearChangeSets() {
        mProjectPersister.clearAllChangeSets();
    }

    /**
     * Attempts to match existing projects against a list of project names.
     *
     * @param names names to match
     * @return any matching projects in a Map, keyed on the project name
     */
    private Map<String, Project> fetchProjectsByName(Collection<String> names) {
        Map<String, Project> projects = new HashMap<String, Project>();
        if (names.size() > 0) {
            String params = StringUtils.repeat(names.size(), "?", ",");
            String[] paramValues = names.toArray(new String[0]);
            Cursor cursor = mContext.getContentResolver().query(
                    ProjectProvider.Projects.CONTENT_URI,
                    ProjectProvider.Projects.FULL_PROJECTION,
                    ProjectProvider.Projects.NAME + " IN (" + params + ")",
                    paramValues, ProjectProvider.Projects.NAME + " ASC");
            while (cursor.moveToNext()) {
                Project project = mProjectPersister.read(cursor);
                projects.put(project.getName(), project);
            }
            cursor.close();
        }
        return projects;
    }

}
