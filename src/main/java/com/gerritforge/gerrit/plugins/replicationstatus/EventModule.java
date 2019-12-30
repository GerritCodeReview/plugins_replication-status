package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.events.EventListener;

public class EventModule extends LifecycleModule {

    @Override
    protected void configure() {
        DynamicSet.bind(binder(), EventListener.class).to(EventHandler.class);
    }
}