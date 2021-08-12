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

import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatusProjectRemoteResource.REPLICATION_STATUS_PROJECT_REMOTE_KIND;

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Scopes;
import com.googlesource.gerrit.plugins.replication.FanoutReplicationConfig;
import com.googlesource.gerrit.plugins.replication.ReplicationConfig;
import com.googlesource.gerrit.plugins.replication.ReplicationFileBasedConfig;
import java.nio.file.Files;

class ReplicationStatusApiModule extends RestApiModule {

  private final SitePaths site;

  public ReplicationStatusApiModule(SitePaths site) {
    this.site = site;
  }

  @Override
  protected void configure() {
    bind(ReplicationStatusAction.class).in(Scopes.SINGLETON);
    DynamicMap.mapOf(binder(), REPLICATION_STATUS_PROJECT_REMOTE_KIND);
    child(PROJECT_KIND, "remotes").to(ReplicationStatusProjectRemoteCollection.class);
    get(REPLICATION_STATUS_PROJECT_REMOTE_KIND, "replication-status")
        .to(ReplicationStatusAction.class);

    bind(ConfigParser.class).in(Scopes.SINGLETON);
    bind(ReplicationConfig.class).to(getReplicationConfigClass()).in(Scopes.SINGLETON);
  }

  private Class<? extends ReplicationConfig> getReplicationConfigClass() {
    if (Files.exists(site.etc_dir.resolve("replication"))) {
      return FanoutReplicationConfig.class;
    }
    return ReplicationFileBasedConfig.class;
  }
}
