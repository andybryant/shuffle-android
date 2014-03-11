package org.dodgybits.shuffle.android.server.sync.processor;

import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.protocol.EntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.TaskProtocolTranslator;
import org.dodgybits.shuffle.dto.ShuffleProtos;

import java.util.ArrayList;
import java.util.List;

public class TaskSyncProcessor {
    private static final String TAG = "TaskSyncProcessor";

    @Inject
    private android.content.Context mContext;

    @Inject
    private TaskPersister mTaskPersister;

    public void processTasks(ShuffleProtos.SyncResponse response,
                                                    EntityDirectory<Context> contextLocator,
                                                    EntityDirectory<Project> projectLocator) {
        TaskProtocolTranslator translator = new TaskProtocolTranslator(contextLocator, projectLocator);

        addNewTasks(response, translator);
        updateModifiedTasks(response, translator);
        updateLocallyNewTasks(response);
        deleteMissingTasks(response);
        clearChangeSets();
    }

    private void addNewTasks(ShuffleProtos.SyncResponse response,
                                TaskProtocolTranslator translator) {
        List<ShuffleProtos.Task> protoTasks = response.getNewTasksList();
        List<Task> newTasks = new ArrayList<>();
        for (ShuffleProtos.Task protoTask : protoTasks) {
            Task task = translator.fromMessage(protoTask);
            newTasks.add(task);
        }
        Log.d(TAG, "Added " + newTasks.size() + " new tasks from server");
        mTaskPersister.bulkInsert(newTasks);
    }

    private void updateModifiedTasks(ShuffleProtos.SyncResponse response,
                                        TaskProtocolTranslator translator) {
        List<ShuffleProtos.Task> protoTasks = response.getModifiedTasksList();
        for (ShuffleProtos.Task protoTask : protoTasks) {
            Task task = translator.fromMessage(protoTask);
            mTaskPersister.update(task);
        }
        Log.d(TAG, "Updated " + protoTasks.size() + " modified tasks");
    }

    private void updateLocallyNewTasks(ShuffleProtos.SyncResponse response) {
        List<ShuffleProtos.SyncIdPair> pairsList = response.getAddedTaskIdPairsList();
        for (ShuffleProtos.SyncIdPair pair : pairsList) {
            Id localId = Id.create(pair.getDeviceEntityId());
            Id gaeId = Id.create(pair.getGaeEntityId());
            mTaskPersister.updateGaeId(localId, gaeId);
        }
        Log.d(TAG, "Added gaeId for " + pairsList.size() + " tasks not previously on server");
    }

    private void deleteMissingTasks(ShuffleProtos.SyncResponse response) {
        List<Long> idsList = response.getDeletedTaskGaeIdsList();
        for (long gaeId : idsList) {
            mTaskPersister.deletePermanentlyByGaeId(Id.create(gaeId));
        }
        Log.w(TAG, "Permanently deleted " + idsList.size() + " missing tasks");
    }

    private void clearChangeSets() {
        mTaskPersister.clearAllChangeSets();
    }

}
