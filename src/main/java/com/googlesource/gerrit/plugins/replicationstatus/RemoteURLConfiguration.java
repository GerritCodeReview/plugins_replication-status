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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlesource.gerrit.plugins.replication.ReplicationFilter;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;

public class RemoteURLConfiguration {

  private final ImmutableList<String> urls;
  private final ImmutableList<String> projects;
  private final String name;
  private final String remoteNameStyle;

  public RemoteURLConfiguration(RemoteConfig remoteConfig, Config cfg) {
    name = remoteConfig.getName();
    urls = ImmutableList.copyOf(cfg.getStringList("remote", name, "url"));
    projects = ImmutableList.copyOf(cfg.getStringList("remote", name, "projects"));
    remoteNameStyle =
        MoreObjects.firstNonNull(cfg.getString("remote", name, "remoteNameStyle"), "slash");
  }

  public ImmutableList<String> getUrls() {
    return urls;
  }

  public boolean isSingleProjectMatch() {
    boolean ret = (projects.size() == 1);
    if (ret) {
      String projectMatch = projects.get(0);
      if (ReplicationFilter.getPatternType(projectMatch)
          != ReplicationFilter.PatternType.EXACT_MATCH) {
        // projectMatch is either regular expression, or wild-card.
        //
        // Even though they might refer to a single project now, they need not
        // after new projects have been created. Hence, we do not treat them as
        // matching a single project.
        ret = false;
      }
    }
    return ret;
  }

  public String getName() {
    return name;
  }

  public String getRemoteNameStyle() {
    return remoteNameStyle;
  }
}
