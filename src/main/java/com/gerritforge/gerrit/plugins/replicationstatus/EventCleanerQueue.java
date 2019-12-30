package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class EventCleanerQueue implements LifecycleListener {
    private final WorkQueue workQueue;
    private final String pluginName;
    private ScheduledExecutorService pool;

    @Inject
    public EventCleanerQueue(WorkQueue workQueue, @PluginName String pluginName) {
        this.workQueue = workQueue;
        this.pluginName = pluginName;
    }

    @Override
    public void start() {
        pool = workQueue.createQueue(1, String.format("[%s] Stop listening for events", pluginName));
    }

    @Override
    public void stop() {
        if (pool != null) {
            pool = null;
        }
    }

    ScheduledExecutorService getPool() {
        return this.pool;
    }
}