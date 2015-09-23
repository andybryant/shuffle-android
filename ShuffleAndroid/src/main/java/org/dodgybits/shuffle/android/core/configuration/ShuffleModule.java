package org.dodgybits.shuffle.android.core.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.ContextEncoder;
import org.dodgybits.shuffle.android.core.model.encoding.EntityEncoder;
import org.dodgybits.shuffle.android.core.model.encoding.ProjectEncoder;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.*;

public class ShuffleModule extends AbstractModule {

    @Override
	protected void configure() {
        addCaches();
        addPersisters();
        addEncoders();
	}

    private void addCaches() {
        bind(new TypeLiteral<EntityCache<Context>>() {}).to(new TypeLiteral<DefaultEntityCache<Context>>() {});
        bind(new TypeLiteral<EntityCache<Project>>() {}).to(new TypeLiteral<DefaultEntityCache<Project>>() {});
    }

    private void addPersisters() {
        bind(new TypeLiteral<EntityPersister<Context>>() {}).to(ContextPersister.class);
        bind(new TypeLiteral<EntityPersister<Project>>() {}).to(ProjectPersister.class);
        bind(new TypeLiteral<EntityPersister<Task>>() {}).to(TaskPersister.class);
    }

    private void addEncoders() {
        bind(new TypeLiteral<EntityEncoder<Context>>() {}).to(ContextEncoder.class);
        bind(new TypeLiteral<EntityEncoder<Project>>() {}).to(ProjectEncoder.class);
        bind(new TypeLiteral<EntityEncoder<Task>>() {}).to(TaskEncoder.class);
    }

}
