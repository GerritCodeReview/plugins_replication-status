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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.junit.Before;
import org.junit.Test;
import java.util.*;

public class StatusMetricsTest {

  private TestMetrics testMetrics = new TestMetrics();
  private StatusStore statusStore = new StatusStore();

  @Before
  public void setUp() throws Exception {
    testMetrics.reset();
    statusStore.emptyReplicationTimePerProject();
  }

  private void setWithStatusStore(StatusStore statusStore) {

    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(MetricMaker.class).toInstance(testMetrics);
                bind(LifecycleListener.class).to(StatusMetrics.class);
              }
            });
    StatusMetrics statusMetrics = injector.getInstance(StatusMetrics.class);

    statusMetrics.start();
  }

  @Test
  public void shouldReturnAvailableReplicationTime() {
    Long elapsed = 100L;

    statusStore.updateLastReplicationTime("myProject", elapsed);
    setWithStatusStore(statusStore);

    //XXX This get() should handle the optional!
    assertThat(testMetrics.getLastReplicationTime("myProject").get()).isEqualTo(elapsed);
  }

  @Test
  public void shouldNotReturnReplicationTimeForUndefinedProject() {
    //XXX This get() should handle the optional!
    assertThat(testMetrics.getLastReplicationTime("myProject").isPresent()).isFalse();
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new Module(), testModule);
  }

  @Singleton
  private class TestMetrics extends DisabledMetricMaker {
    private Long latency = 0L;

    public Optional<Long> getLastReplicationTime(String project) {
      return statusStore.getLastReplicationTime(project);
    }

    public void reset() {
      latency = 0L;
    }
  }
}
