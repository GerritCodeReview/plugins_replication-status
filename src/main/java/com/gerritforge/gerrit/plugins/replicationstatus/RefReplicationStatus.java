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

import com.google.auto.value.AutoValue;
import com.google.gerrit.entities.Project;
import java.io.Serializable;
import java.util.Map;

@AutoValue
public abstract class RefReplicationStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  static final String CACHE_NAME = "replication_status";

  static RefReplicationStatus create(Map<String, RemoteReplicationStatus> remoteStatus) {
    return new AutoValue_RefReplicationStatus(remoteStatus);
  }

  public abstract Map<String, RemoteReplicationStatus> remoteStatus();

  public boolean hasFailures() {
    return remoteStatus().values().stream().anyMatch(r -> r.status().isFailure());
  }

  @AutoValue
  public abstract static class Key implements Serializable {
    private static final long serialVersionUID = 1L;

    static Key create(Project.NameKey projectName, String ref) {
      return new AutoValue_RefReplicationStatus_Key(projectName, ref);
    }

    abstract Project.NameKey projectName();

    abstract String ref();
  }
}
