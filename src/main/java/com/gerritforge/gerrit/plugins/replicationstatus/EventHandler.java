package com.gerritforge.gerrit.plugins.replicationstatus;


import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Inject;

class EventHandler implements EventListener {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @Inject
    private StatusStore statusStore;

    @Override
    public void onEvent(Event event) {
        if (event instanceof RefUpdatedEvent) {
            String projectName = ((RefUpdatedEvent) event).getProjectNameKey().get();
            statusStore.updateLastReplicationTime(projectName,event.eventCreatedOn);
        } else {
            logger.atInfo().log("Not a ref updated event, skipping {}", event.type);
        }
    }
}
