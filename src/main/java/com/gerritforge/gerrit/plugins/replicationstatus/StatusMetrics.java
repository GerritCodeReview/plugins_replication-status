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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
public class StatusMetrics implements LifecycleListener {
  private static final Logger log = LoggerFactory.getLogger(StatusMetrics.class);
  private static final String GLOBAL_LATENCY_METRIC = "global-latency-metric";
  private final MetricMaker metricMaker;
  private final Set<RegistrationHandle> registeredMetrics;
  private final Set<String> projectsWithMetrics;
  private final Set<Runnable> triggers;
  private StatusStore statusStore;

  @Inject
  public StatusMetrics(MetricMaker metricMaker, StatusStore statusStore) {
    this.metricMaker = metricMaker;
    this.statusStore = statusStore;
    this.projectsWithMetrics = Collections.synchronizedSet(new HashSet<>());
    this.registeredMetrics = Collections.synchronizedSet(new HashSet<>());
    this.triggers = Collections.synchronizedSet(new HashSet<>());
  }

  @Override
  public void start() {
    log.info("Starting replication-status metrics collector");
  }

  @Override
  public void stop() {
    for (RegistrationHandle handle : registeredMetrics) {
      handle.remove();
    }
  }

  public void upsertMertricForProject(String projectName) {
    if (!projectsWithMetrics.contains(projectName)) {
      addLatencyMetricFor(projectName);
    } else {
      log.info("Metric already exists for project " + projectName);
    }
    if (!projectsWithMetrics.contains(GLOBAL_LATENCY_METRIC)) {
      addGlobalLatencyMetric();
    } else {
      log.info("Metric already exists for global project");
    }
  }

  private void addGlobalLatencyMetric() {
    String name = "replicationstatus-global";
    CallbackMetric0 <Long> latestReplicationTimeMetricCB =
            metricMaker.newCallbackMetric(
                    String.format("%s/latest_replication_time", name),
                    Long.class,
                    new Description(String.format("%s last replication timestamp (ms)", name))
                            .setGauge()
                            .setUnit(Description.Units.MILLISECONDS));


    Runnable trigger =
            () -> latestReplicationTimeMetricCB.set(statusStore.getGlobalLastReplicationTime());
    RegistrationHandle registrationHandle = metricMaker.newTrigger(latestReplicationTimeMetricCB, trigger);
    projectsWithMetrics.add(GLOBAL_LATENCY_METRIC);
    registeredMetrics.add(registrationHandle);
    triggers.add(trigger);
  }

  private void addLatencyMetricFor(String projectName) {
    String name = "replicationstatus-" + projectName;
    CallbackMetric0 <Long> latestReplicationTimeMetricCB =
            metricMaker.newCallbackMetric(
                    String.format("%s/latest_replication_time", name),
                    Long.class,
                    new Description(String.format("%s last replication timestamp (ms)", name))
                            .setGauge()
                            .setUnit(Description.Units.MILLISECONDS));

    Runnable trigger =
            () -> latestReplicationTimeMetricCB.set(statusStore.getLastReplicationTime(projectName).orElse(0L));

    RegistrationHandle registrationHandle = metricMaker.newTrigger(latestReplicationTimeMetricCB, trigger);
    log.info("Added callback for " + projectName);

    projectsWithMetrics.add(projectName);
    registeredMetrics.add(registrationHandle);
    triggers.add(trigger);
  }
}
