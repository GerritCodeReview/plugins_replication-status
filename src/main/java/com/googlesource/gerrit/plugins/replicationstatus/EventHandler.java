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

import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.CACHE_NAME;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.Key;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationStatusResult;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationType;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationType.FETCH;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.ReplicationType.PUSH;

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
import com.googlesource.gerrit.plugins.replication.pull.FetchRefReplicatedEvent;
import com.googlesource.gerrit.plugins.replication.pull.FetchReplicationScheduledEvent;

class EventHandler implements EventListener {
  private final Cache<Key, ReplicationStatus> replicationStatusCache;
  private final String nodeInstanceId;

  public static final String SCHEDULED_REPLICATION = "scheduled";

  @Inject
  EventHandler(
      @Named(CACHE_NAME) Cache<Key, ReplicationStatus> replicationStatusCache,
      @Nullable @GerritInstanceId String nodeInstanceId) {
    this.replicationStatusCache = replicationStatusCache;
    this.nodeInstanceId = nodeInstanceId;
  }

  @Override
  public void onEvent(Event event) {
    // TODO: should consume is dependent on type
    if (shouldConsume(event)) {
      if (event instanceof RefReplicatedEvent) {
        RefReplicatedEvent replEvent = (RefReplicatedEvent) event;
        putCacheEntry(PUSH, replEvent, replEvent.targetNode, replEvent.status);
      } else if (event instanceof ReplicationScheduledEvent) {
        ReplicationScheduledEvent replEvent = (ReplicationScheduledEvent) event;
        putCacheEntry(PUSH, replEvent, replEvent.targetNode, SCHEDULED_REPLICATION);
      } else if (event instanceof FetchRefReplicatedEvent) {
        FetchRefReplicatedEvent replEvent = (FetchRefReplicatedEvent) event;
        putCacheEntry(FETCH, replEvent, replEvent.getSourceNode(), replEvent.getStatus());
      } else if (event instanceof FetchReplicationScheduledEvent) {
        FetchReplicationScheduledEvent replEvent = (FetchReplicationScheduledEvent) event;
        putCacheEntry(FETCH, replEvent, replEvent.getSourceNode(), SCHEDULED_REPLICATION);
      }
    }
  }

  private <T extends RefEvent> void putCacheEntry(
      ReplicationType type, T refEvent, String remote, String status) {
    Key cacheKey = Key.create(refEvent.getProjectNameKey(), remote, refEvent.getRefName());

    replicationStatusCache.put(
        cacheKey,
        ReplicationStatus.create(
            type, ReplicationStatusResult.fromString(status), refEvent.eventCreatedOn));
  }

  private boolean shouldConsume(Event event) {
    return nodeInstanceId == null && event.instanceId == null
        || nodeInstanceId != null && nodeInstanceId.equals(event.instanceId);
  }
}
