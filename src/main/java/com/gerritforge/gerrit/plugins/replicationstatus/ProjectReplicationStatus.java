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
import java.util.Map;

@AutoValue
public abstract class ProjectReplicationStatus {
  static ProjectReplicationStatus create(
      Map<String, RefReplicationStatus> refStatus,
      ProjectReplicationStatusResult status,
      String project) {
    return new AutoValue_ProjectReplicationStatus(refStatus, status, project);
  }

  public abstract Map<String, RefReplicationStatus> refStatus();

  public abstract ProjectReplicationStatusResult status();

  public abstract String project();

  enum ProjectReplicationStatusResult {
    FAILED,
    OK;

    public boolean isFailure() {
      return this == FAILED;
    }
  }
}
