package org.dodgybits.shuffle.android.core.model.encoding;

import android.os.Bundle;
import com.google.inject.Singleton;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Context.Builder;
import org.dodgybits.shuffle.android.core.util.BundleUtils;
import org.dodgybits.shuffle.sync.model.ContextChangeSet;

import static android.provider.BaseColumns._ID;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.ACTIVE;
import static org.dodgybits.shuffle.android.persistence.provider.AbstractCollectionProvider.ShuffleTable.DELETED;
import static org.dodgybits.shuffle.android.persistence.provider.ContextProvider.Contexts.*;

@Singleton
public class ContextEncoder implements EntityEncoder<Context> {

    @Override
    public void save(Bundle icicle, Context context) {
        BundleUtils.putId(icicle, _ID, context.getLocalId());
        icicle.putLong(MODIFIED_DATE, context.getModifiedDate());
        icicle.putBoolean(DELETED, context.isDeleted());
        icicle.putBoolean(ACTIVE, context.isActive());

        icicle.putString(NAME, context.getName());
        icicle.putInt(COLOUR, context.getColourIndex());
        icicle.putString(ICON, context.getIconName());
        BundleUtils.putId(icicle, GAE_ID, context.getGaeId());
        icicle.putLong(CHANGE_SET, context.getChangeSet().getChangeSet());
    }
    
    @Override
    public Context restore(Bundle icicle) {
        if (icicle == null) return null;

        Builder builder = Context.newBuilder();
        builder.setLocalId(BundleUtils.getId(icicle, _ID));
        builder.setModifiedDate(icicle.getLong(MODIFIED_DATE, 0L));
        builder.setDeleted(icicle.getBoolean(DELETED));
        builder.setActive(icicle.getBoolean(ACTIVE));

        builder.setName(icicle.getString(NAME));
        builder.setColourIndex(icicle.getInt(COLOUR));
        builder.setIconName(icicle.getString(ICON));
        builder.setGaeId(BundleUtils.getId(icicle, GAE_ID));
        builder.setChangeSet(ContextChangeSet.fromChangeSet(icicle.getLong(CHANGE_SET)));

        return builder.build();
    }

    
}
