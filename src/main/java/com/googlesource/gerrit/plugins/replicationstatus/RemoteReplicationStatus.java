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

import com.google.auto.value.AutoValue;
import com.google.common.flogger.FluentLogger;

@AutoValue
public abstract class RemoteReplicationStatus {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static RemoteReplicationStatus create(ReplicationStatusResult status, long when) {
    return new AutoValue_RemoteReplicationStatus(status, when);
  }

  public abstract ReplicationStatusResult status();

  public abstract long when();

  enum ReplicationStatusResult {
    FAILED,
    NOT_ATTEMPTED,
    SUCCEEDED,
    UNKNOWN;

    static ReplicationStatusResult fromString(String result) {
      switch (result.toLowerCase()) {
        case "succeeded":
          return SUCCEEDED;
        case "not_attempted":
          return NOT_ATTEMPTED;
        case "failed":
          return FAILED;
        default:
          logger.atSevere().log(
              "Could not parse result into a valid replication status: %", result);
          return UNKNOWN;
      }
    }

    public boolean isFailure() {
      return this == FAILED;
    }
  }
}
