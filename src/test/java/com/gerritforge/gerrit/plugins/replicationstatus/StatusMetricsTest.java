// Copyright (C) 2019 The Android Open Source Project
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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.junit.Before;
import org.junit.Test;
import java.util.*;

public class StatusMetricsTest {

  @Inject private ListeningExecutorService executor;
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
  public void shouldReturnAllTimeStapms() {
    Long elapsed = 100L;

    statusStore.updateLastReplicationTime("myProject", elapsed);
    setWithStatusStore(statusStore);

    //XXX This get() should handle the optional!
    assertThat(testMetrics.getLastReplicationTime("myProject").get()).isEqualTo(elapsed);
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

    @Override
    public <V> CallbackMetric0<V> newCallbackMetric(
        String name, Class<V> valueClass, Description desc) {
      return new CallbackMetric0<V>() {
        @Override
        public void set(V value) {
          latency = (Long) value;
        }

        @Override
        public void remove() {}
      };
    }
  }
}
