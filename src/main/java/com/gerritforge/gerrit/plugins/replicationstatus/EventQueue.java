package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;

import java.util.concurrent.ScheduledExecutorService;

public class EventQueue implements LifecycleListener {
    private final WorkQueue workQueue;
    private final String pluginName;
    private ScheduledExecutorService pool;

    @Inject
    EventQueue(WorkQueue workQueue, @PluginName String pluginName) {
        this.workQueue = workQueue;
        this.pluginName = pluginName;
    }

    /** {@inheritDoc} Create a new executor queue in WorkQueue for storing events. */
    @Override
    public void start() {
        pool = workQueue.createQueue(1, String.format("[%s] Monitor replication status", pluginName));
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
