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

import static com.googlesource.gerrit.plugins.replicationstatus.RemoteReplicationStatus.ReplicationStatusResult;
import static com.googlesource.gerrit.plugins.replicationstatus.RemoteReplicationStatus.create;

import com.google.common.cache.Cache;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.replication.RefReplicatedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class EventHandler implements EventListener {
  private final Cache<RefReplicationStatus.Key, RefReplicationStatus> replicationStatusCache;
  private final String nodeInstanceId;

  @Inject
  EventHandler(
      @Named(RefReplicationStatus.CACHE_NAME)
          Cache<RefReplicationStatus.Key, RefReplicationStatus> replicationStatusCache,
      @GerritInstanceId String nodeInstanceId) {
    this.replicationStatusCache = replicationStatusCache;
    this.nodeInstanceId = nodeInstanceId;
  }

  @Override
  public void onEvent(Event event) {
    if (isGeneratedByThisInstance(event)) {
      if (event instanceof RefReplicatedEvent) {
        RefReplicatedEvent replEvent = (RefReplicatedEvent) event;

        RefReplicationStatus.Key cacheKey =
            RefReplicationStatus.Key.create(Project.nameKey(replEvent.project), replEvent.ref);

        Map<String, RemoteReplicationStatus> remoteStatus =
            Optional.ofNullable(replicationStatusCache.getIfPresent(cacheKey))
                .map(RefReplicationStatus::remoteStatus)
                .orElse(new HashMap<>());

        remoteStatus.put(
            replEvent.targetNode,
            create(ReplicationStatusResult.fromString(replEvent.status), replEvent.eventCreatedOn));

        replicationStatusCache.put(cacheKey, RefReplicationStatus.create(remoteStatus));
      }
    }
  }

  private boolean isGeneratedByThisInstance(Event event) {
    return nodeInstanceId.equals(event.instanceId);
  }
}
