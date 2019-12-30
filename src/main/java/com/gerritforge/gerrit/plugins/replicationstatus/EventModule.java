package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.internal.UniqueAnnotations;

public class EventModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventQueue.class).in(Scopes.SINGLETON);
        bind(EventHandler.class).in(Scopes.SINGLETON);
        bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(EventQueue.class);
        bind(LifecycleListener.class)
                .annotatedWith(UniqueAnnotations.create())
                .to(EventCleanerQueue.class);
        DynamicSet.bind(binder(), EventListener.class).to(EventHandler.class);
    }
}