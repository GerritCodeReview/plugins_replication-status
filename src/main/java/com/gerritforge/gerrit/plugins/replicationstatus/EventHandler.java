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

package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.RefReplicatedEvent;
import com.googlesource.gerrit.plugins.replication.RefReplicationDoneEvent;

class EventHandler implements EventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Gson gson;
  private final String nodeInstanceId;

  @Inject
  EventHandler(Gson gson, @GerritInstanceId String instanceId) {
    this.gson = gson;
    this.nodeInstanceId = instanceId;
  }

  @Override
  public void onEvent(Event event) {
    if (isGeneratedByThisInstance(event)) {

      if (event instanceof RefReplicatedEvent) {
        logger.atWarning().log("TONY EVENT: " + gson.toJson(event));
        RefReplicatedEvent replEvent = (RefReplicatedEvent) event;
        logger.atWarning().log(
            "TONY DONE ONE: project: %s ref: [%s]|when: [%s]|target: [%s]|refStatus: [%s]|status: [%s]",
            replEvent.project,
            replEvent.ref,
            replEvent.eventCreatedOn,
            replEvent.targetNode,
            replEvent.refStatus,
            replEvent.status);
      }
      if (event instanceof RefReplicationDoneEvent) {
        logger.atWarning().log("TONY EVENT: " + gson.toJson(event));
        RefReplicationDoneEvent doneEvent = (RefReplicationDoneEvent) event;
        logger.atWarning().log(
            "TONY DONE ALL: project:[%s]|ref:[%s]|when:[%s]|branch:[%s]",
            doneEvent.getProjectNameKey(),
            doneEvent.getRefName(),
            doneEvent.eventCreatedOn,
            doneEvent.getBranchNameKey());
      }

      //      if(event instanceof FetchRefRe) // add dependency in BUILD
    }
  }

  private boolean isGeneratedByThisInstance(Event event) {
    return nodeInstanceId.equals(event.instanceId);
  }
}
