package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        bind(LifecycleListener.class).to(StatusMetrics.class);
        install(new ReplicationStatusApiModule());
    }
}
