package org.dodgybits.shuffle.android.roboguice;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.widget.RemoteViewsService;
import com.google.inject.Injector;
import com.google.inject.Key;
import roboguice.RoboGuice;
import roboguice.context.event.OnConfigurationChangedEvent;
import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.context.event.OnStartEvent;
import roboguice.event.EventManager;
import roboguice.util.RoboContext;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link roboguice.service.RoboService} extends from {@link android.app.Service} to provide dynamic
 * injection of collaborators, using Google Guice.<br /> <br />
 *
 * Your own services that usually extend from {@link android.widget.RemoteViewsService} should now extend from
 * {@link RoboRemoteViewsService}.<br /> <br />
 *
 * If we didn't provide what you need, you have two options : either post an issue on <a
 * href="http://code.google.com/p/roboguice/issues/list">the bug tracker</a>, or
 * implement it yourself. Have a look at the source code of this class (
 * {@link RoboRemoteViewsService}), you won't have to write that much changes. And of
 * course, you are welcome to contribute and send your implementations to the
 * RoboGuice project.<br /> <br />
 *
 * You can have access to the Guice
 * {@link com.google.inject.Injector} at any time, by calling {@link roboguice.RoboGuice#getInjector(android.content.Context)} ()}.<br />
 *
 * However, you will not have access to ContextSingleton scoped beans until
 * {@link #onCreate()} is called. <br /> <br />
 *
 * @author Mike Burton
 * @author Christine Karman
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class RoboRemoteViewsService extends RemoteViewsService implements RoboContext {

    protected EventManager eventManager;
    protected HashMap<Key<?>,Object> scopedObjects = new HashMap<Key<?>, Object>();

    @Override
    public void onCreate() {
        final Injector injector = RoboGuice.getInjector(this);
        eventManager = injector.getInstance(EventManager.class);
        injector.injectMembers(this);
        super.onCreate();
        eventManager.fire(new OnCreateEvent<Service>(this,null) );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int startCont = super.onStartCommand(intent,flags, startId);
        eventManager.fire(new OnStartEvent<Service>(this));
        return startCont;
    }

    @Override
    public void onDestroy() {
        try {
            if(eventManager!=null) // may be null during test: http://code.google.com/p/roboguice/issues/detail?id=140
                eventManager.fire(new OnDestroyEvent<Service>(this) );
        } finally {
            try {
                RoboGuice.destroyInjector(this);
            } finally {
                super.onDestroy();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        final Configuration currentConfig = getResources().getConfiguration();
        super.onConfigurationChanged(newConfig);
        eventManager.fire(new OnConfigurationChangedEvent<Service>(this,currentConfig, newConfig) );
    }

    @Override
    public Map<Key<?>, Object> getScopedObjectMap() {
        return scopedObjects;
    }

}
