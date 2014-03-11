package org.dodgybits.shuffle.android.server.sync;

import android.database.Cursor;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.protocol.*;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.dto.ShuffleProtos;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class SyncRequestBuilder {
    private static final String TAG = "SyncRequestBuilder";

    @Inject
    private android.content.Context mContext;
    @Inject
    private ContextPersister mContextPersister;
    @Inject
    private ProjectPersister mProjectPersister;
    @Inject
    private TaskPersister mTaskPersister;


    public ShuffleProtos.SyncRequest createRequest() {
        ShuffleProtos.SyncRequest.Builder builder = ShuffleProtos.SyncRequest.newBuilder();
        builder.setDeviceIdentity(Preferences.getSyncDeviceIdentity(mContext));
        String lastSyncId = Preferences.getLastSyncId(mContext);
        if (lastSyncId != null) {
            builder.setLastSyncId(lastSyncId);
        }

        String gcmRegistrationId = Preferences.getGcmRegistrationId(mContext);
        if (!gcmRegistrationId.isEmpty()) {
            builder.setGcmRegistrationId(gcmRegistrationId);
        }

        long lastSyncDate = Preferences.getLastSyncLocalDate(mContext);

        builder.setLastSyncDeviceDate(lastSyncDate);
        builder.setLastSyncGaeDate(Preferences.getLastSyncGaeDate(mContext));

        long lastDeletedDate = Preferences.getLastPermanentlyDeletedDate(mContext);
        builder.setEntitiesPermanentlyDeleted(lastDeletedDate > lastSyncDate);

        EntityDirectory<Context> contextDirectory = addContexts(builder, lastSyncDate);
        EntityDirectory<Project> projectDirectory = addProjects(builder, lastSyncDate, contextDirectory);
        addTasks(builder, lastSyncDate, contextDirectory, projectDirectory);

        builder.setCurrentDeviceDate(System.currentTimeMillis());

        return builder.build();
    }

    private EntityDirectory<Context> addContexts(ShuffleProtos.SyncRequest.Builder builder, long lastSyncDate) {
        Log.d(TAG, "Adding contexts");
        HashEntityDirectory<Context> directory = new HashEntityDirectory<Context>();
        Cursor cursor = mContext.getContentResolver().query(
                ContextProvider.Contexts.CONTENT_URI, ContextProvider.Contexts.FULL_PROJECTION,
                null, null, null);
        ContextProtocolTranslator translator = new ContextProtocolTranslator();
        while (cursor.moveToNext()) {
            org.dodgybits.shuffle.android.core.model.Context context = mContextPersister.read(cursor);
            directory.addItem(context.getLocalId(), context.getName(), context);
            if (!context.getGaeId().isInitialised()) {
                // update everything for new items
                context.getChangeSet().markAll();
                builder.addNewContexts(translator.toMessage(context));
            } else if (context.getChangeSet().hasChanges()) {
                builder.addModifiedContexts(translator.toMessage(context));
            } else {
                ShuffleProtos.SyncIdPair.Builder pairBuilder = ShuffleProtos.SyncIdPair.newBuilder();
                pairBuilder.setDeviceEntityId(context.getLocalId().getId());
                pairBuilder.setGaeEntityId(context.getGaeId().getId());
                builder.addUnmodifiedContextIdPairs(pairBuilder);
            }
        }
        cursor.close();
        return directory;
    }

    private EntityDirectory<Project> addProjects(ShuffleProtos.SyncRequest.Builder builder, long lastSyncDate,
                             EntityDirectory<Context> contextDirectory) {
        Log.d(TAG, "Adding projects");
        HashEntityDirectory<Project> directory = new HashEntityDirectory<Project>();
        Cursor cursor = mContext.getContentResolver().query(
                ProjectProvider.Projects.CONTENT_URI, ProjectProvider.Projects.FULL_PROJECTION,
                null, null, null);
        ProjectProtocolTranslator translator = new ProjectProtocolTranslator(contextDirectory);
        while (cursor.moveToNext()) {
            Project project = mProjectPersister.read(cursor);
            directory.addItem(project.getLocalId(), project.getName(), project);
            if (!project.getGaeId().isInitialised()) {
                // update everything for new items
                project.getChangeSet().markAll();
                builder.addNewProjects(translator.toMessage(project));
            } else if (project.getChangeSet().hasChanges()) {
                builder.addModifiedProjects(translator.toMessage(project));
            } else {
                ShuffleProtos.SyncIdPair.Builder pairBuilder = ShuffleProtos.SyncIdPair.newBuilder();
                pairBuilder.setDeviceEntityId(project.getLocalId().getId());
                pairBuilder.setGaeEntityId(project.getGaeId().getId());
                builder.addUnmodifiedProjectIdPairs(pairBuilder);
            }
        }
        cursor.close();
        return  directory;
    }

    private void addTasks(ShuffleProtos.SyncRequest.Builder builder, long lastSyncDate,
                          EntityDirectory<Context> contextDirectory, EntityDirectory<Project> projectDirectory) {
        Log.d(TAG, "Adding tasks");
        Cursor cursor = mContext.getContentResolver().query(
                TaskProvider.Tasks.CONTENT_URI, TaskProvider.Tasks.FULL_PROJECTION,
                null, null, null);
        TaskProtocolTranslator translator = new TaskProtocolTranslator(contextDirectory, projectDirectory);
        while (cursor.moveToNext()) {
            org.dodgybits.shuffle.android.core.model.Task task = mTaskPersister.read(cursor);
            if (!task.getGaeId().isInitialised()) {
                // update everything for new items
                task.getChangeSet().markAll();
                builder.addNewTasks(translator.toMessage(task));
            } else if (task.getChangeSet().hasChanges()) {
                builder.addModifiedTasks(translator.toMessage(task));
            } else {
                ShuffleProtos.SyncIdPair.Builder pairBuilder = ShuffleProtos.SyncIdPair.newBuilder();
                pairBuilder.setDeviceEntityId(task.getLocalId().getId());
                pairBuilder.setGaeEntityId(task.getGaeId().getId());
                builder.addUnmodifiedTaskIdPairs(pairBuilder);
            }
        }
        cursor.close();
    }


}
