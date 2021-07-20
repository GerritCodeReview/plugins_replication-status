// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.replicationstatus;

import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationStatusResult;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationStatusResult.SCHEDULED;

import com.google.common.cache.Cache;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.replication.RefReplicatedEvent;
import com.googlesource.gerrit.plugins.replication.ReplicationScheduledEvent;

class EventHandler implements EventListener {
  private final Cache<ReplicationStatus.Key, ReplicationStatus> replicationStatusCache;
  private final String nodeInstanceId;

  @Inject
  EventHandler(
      @Named(ReplicationStatus.CACHE_NAME)
          Cache<ReplicationStatus.Key, ReplicationStatus> replicationStatusCache,
      @Nullable @GerritInstanceId String nodeInstanceId) {
    this.replicationStatusCache = replicationStatusCache;
    this.nodeInstanceId = nodeInstanceId;
  }

  @Override
  public void onEvent(Event event) {
    if (shouldConsume(event)) {
      if (event instanceof RefReplicatedEvent) {
        RefReplicatedEvent replEvent = (RefReplicatedEvent) event;
        putCacheEntry(replEvent, replEvent.targetNode, replEvent.status);
      } else if (event instanceof ReplicationScheduledEvent) {
        ReplicationScheduledEvent replEvent = (ReplicationScheduledEvent) event;
        putCacheEntry(replEvent, replEvent.targetNode, SCHEDULED.name());
      }
    }
  }

  private <T extends RefEvent> void putCacheEntry(T refEvent, String targetNode, String status) {
    ReplicationStatus.Key cacheKey =
        ReplicationStatus.Key.create(
            refEvent.getProjectNameKey(), targetNode, refEvent.getRefName());

    replicationStatusCache.put(
        cacheKey,
        ReplicationStatus.create(
            ReplicationStatusResult.fromString(status), refEvent.eventCreatedOn));
  }

  private boolean shouldConsume(Event event) {
    return (nodeInstanceId == null && event.instanceId == null)
        || (nodeInstanceId != null && nodeInstanceId.equals(event.instanceId));
  }
}
