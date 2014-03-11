package org.dodgybits.shuffle.android.server.sync.processor;

import android.database.Cursor;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.protocol.ContextProtocolTranslator;
import org.dodgybits.shuffle.android.core.model.protocol.EntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.HashEntityDirectory;
import org.dodgybits.shuffle.android.core.model.protocol.MutableEntityDirectory;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.dto.ShuffleProtos;

import java.util.*;

public class ContextSyncProcessor {
    private static final String TAG = "ContextSyncProcessor";

    @Inject
    private android.content.Context mContext;

    @Inject
    private ContextPersister mContextPersister;

    public EntityDirectory<Context> processContexts(ShuffleProtos.SyncResponse response) {
        Log.d(TAG, "Parsing context updates");
        ContextProtocolTranslator translator = new ContextProtocolTranslator();
        // build up the locator and list of new contacts
        MutableEntityDirectory<Context> contextLocator = new HashEntityDirectory<>();


        addExistingContexts(contextLocator);
        addNewContexts(response, translator, contextLocator);
        updateModifiedContexts(response, translator, contextLocator);
        updateLocallyNewContexts(response, contextLocator);
        deleteMissingContexts(response);
        clearChangeSets();

        return contextLocator;
    }

    private void addExistingContexts(MutableEntityDirectory<Context> contextLocator) {
        // preload with existing device contexts
        Cursor cursor = mContext.getContentResolver().query(
                ContextProvider.Contexts.CONTENT_URI, ContextProvider.Contexts.FULL_PROJECTION,
                null, null, null);
        while (cursor.moveToNext()) {
            org.dodgybits.shuffle.android.core.model.Context context = mContextPersister.read(cursor);
            if (context.getGaeId().isInitialised()) {
                contextLocator.addItem(context.getGaeId(), context.getName(), context);
            }
        }
        cursor.close();
    }

    private void addNewContexts(ShuffleProtos.SyncResponse response,
                                ContextProtocolTranslator translator,
                                MutableEntityDirectory<Context> contextLocator) {
        List<ShuffleProtos.Context> protoContexts = response.getNewContextsList();
        List<Context> newContexts = new ArrayList<Context>();
        for (ShuffleProtos.Context protoContext : protoContexts) {
            Context context = translator.fromMessage(protoContext);
            newContexts.add(context);
        }
        Log.d(TAG, "Added " + newContexts.size() + " new contexts from server");
        mContextPersister.bulkInsert(newContexts);

        // update locator with newly saved contexts now we have local ids
        for (ShuffleProtos.Context protoContext : protoContexts) {
            Id contextId = Id.create(protoContext.getGaeEntityId());
            Context updatedContext = mContextPersister.findByGaeId(contextId);
            contextLocator.addItem(contextId, updatedContext.getName(), updatedContext);
        }
    }

    private void updateModifiedContexts(ShuffleProtos.SyncResponse response,
                                        ContextProtocolTranslator translator,
                                        MutableEntityDirectory<Context> contextLocator) {
        List<ShuffleProtos.Context> protoContexts = response.getModifiedContextsList();
        for (org.dodgybits.shuffle.dto.ShuffleProtos.Context protoContext : protoContexts) {
            Context context = translator.fromMessage(protoContext);
            Id contextId = Id.create(protoContext.getGaeEntityId());
            String contextName = context.getName();
            contextLocator.addItem(contextId, contextName, context);
            mContextPersister.update(context);
        }
        Log.d(TAG, "Updated " + protoContexts.size() + " modified contexts");
    }

    private void updateLocallyNewContexts(ShuffleProtos.SyncResponse response,
                                          MutableEntityDirectory<Context> contextLocator) {
        List<ShuffleProtos.SyncIdPair> pairsList = response.getAddedContextIdPairsList();
        for (ShuffleProtos.SyncIdPair pair : pairsList) {
            Id localId = Id.create(pair.getDeviceEntityId());
            Id gaeId = Id.create(pair.getGaeEntityId());
            mContextPersister.updateGaeId(localId, gaeId);
            // need to add context to locator as it's possible server
            // has returned new tasks or projects referencing this
            Context updatedContext = mContextPersister.findById(localId);
            contextLocator.addItem(gaeId, updatedContext.getName(), updatedContext);
        }
        Log.d(TAG, "Added gaeId for " + pairsList.size() + " new contexts not previously on server");
    }

    private void deleteMissingContexts(ShuffleProtos.SyncResponse response) {
        List<Long> idsList = response.getDeletedContextGaeIdsList();
        for (long gaeId : idsList) {
            mContextPersister.deletePermanentlyByGaeId(Id.create(gaeId));
        }
        Log.w(TAG, "Permanently deleted " + idsList.size() + " missing contexts");
    }

    private void clearChangeSets() {
        mContextPersister.clearAllChangeSets();
    }

    /**
     * Attempts to match existing contexts against a list of context names.
     *
     * @param names names to match
     * @return any matching contexts in a Map, keyed on the context name
     */
    private Map<String, Context> fetchContextsByName(Collection<String> names) {
        Map<String, Context> contexts = new HashMap<String, Context>();
        if (names.size() > 0) {
            String params = StringUtils.repeat(names.size(), "?", ",");
            String[] paramValues = names.toArray(new String[0]);
            Cursor cursor = mContext.getContentResolver().query(
                    ContextProvider.Contexts.CONTENT_URI,
                    ContextProvider.Contexts.FULL_PROJECTION,
                    ContextProvider.Contexts.NAME + " IN (" + params + ")",
                    paramValues, ContextProvider.Contexts.NAME + " ASC");
            while (cursor.moveToNext()) {
                Context context = mContextPersister.read(cursor);
                contexts.put(context.getName(), context);
            }
            cursor.close();
        }
        return contexts;
    }

}
