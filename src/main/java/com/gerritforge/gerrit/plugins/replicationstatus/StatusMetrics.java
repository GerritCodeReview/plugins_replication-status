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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
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
  private final MetricMaker metricMaker;
  private final Set<RegistrationHandle> registeredMetrics;
  private final Set<Runnable> triggers;
  private StatusStore statusStore;

  @Inject
  public StatusMetrics(MetricMaker metricMaker, StatusStore statusStore) {
    this.metricMaker = metricMaker;
    this.statusStore = statusStore;
    this.registeredMetrics = Collections.synchronizedSet(new HashSet<>());
    this.triggers = Collections.synchronizedSet(new HashSet<>());
  }

  @Override
  public void start() {

    // TODO Need to trigger an update of the metric when e new repo comes in
    // XXX All the projects will have to be fetched at startup time to register all the metrics
    ArrayList<String> projects = new ArrayList<String>();
    projects.add("All-Users");
    projects.add("All-Projects");

    log.error("Starting metrics collection....");
    for (String project : projects) {
      String name = "replicationstatus-" + project;

      CallbackMetric0<Long> latencyMetric =
          metricMaker.newCallbackMetric(
              String.format("%s/latest_replication_time", name),
              Long.class,
              new Description(String.format("%s last replication timestamp (ms)", name))
                  .setGauge()
                  .setUnit(Description.Units.MILLISECONDS));


      Runnable metricCallBack =
          () -> {
            latencyMetric.set(statusStore.getLastReplicationTime(project).orElse(0L)); //XXX Map here!
          };

      // XXX Need to check if metric already exist at startup ??
      registeredMetrics.add(metricMaker.newTrigger(latencyMetric, metricCallBack));
      triggers.add(metricCallBack);
      log.error("Added callbacks for " + name);
    }
  }

  @Override
  public void stop() {
    for (RegistrationHandle handle : registeredMetrics) {
      handle.remove();
    }
  }

  @VisibleForTesting
  public void triggerAll() {
    triggers.forEach(Runnable::run);
  }
}
