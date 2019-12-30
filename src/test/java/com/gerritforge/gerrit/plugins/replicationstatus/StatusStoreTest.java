// Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Optional.*;

import org.junit.Test;
import java.time.Instant;

public class StatusStoreTest {


  @Test
  public void shouldUpdateGlobalStatus() {
    Instant instant = Instant.now();
    Long nowish = instant.toEpochMilli();
    StatusStore statusStore = new StatusStore();

    statusStore.updateLastReplicationTime("myProject", nowish);

    assertThat(statusStore.getGlobalLastReplicationTime()).isEqualTo(nowish);
  }

  @Test
  public void shouldUpdateProjectStatus() {
    String projectName = "myProject";
    Instant instant = Instant.now();
    Long nowish = instant.toEpochMilli();
    StatusStore statusStore = new StatusStore();

    statusStore.updateLastReplicationTime(projectName, nowish);

    assertThat(statusStore.getLastReplicationTime(projectName)).isEqualTo(of(nowish));
  }

  @Test
  public void shouldReturnProjectStatus() {
    StatusStore statusStore = new StatusStore();

    assertThat(statusStore.getLastReplicationTime("nonExistentProject")).isEqualTo(empty());
  }
}
