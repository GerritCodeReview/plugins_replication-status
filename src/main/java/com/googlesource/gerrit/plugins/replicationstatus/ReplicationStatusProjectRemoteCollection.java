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

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.NotImplementedException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
public class ReplicationStatusProjectRemoteCollection
    implements ChildCollection<ProjectResource, ReplicationStatusProjectRemoteResource> {
  private final DynamicMap<RestView<ReplicationStatusProjectRemoteResource>> views;
  private final ReplicationConfigParser replicationConfigParser;

  @Inject
  ReplicationStatusProjectRemoteCollection(
      DynamicMap<RestView<ReplicationStatusProjectRemoteResource>> views,
      ReplicationConfigParser replicationConfigParser) {
    this.views = views;
    this.replicationConfigParser = replicationConfigParser;
  }

  @Override
  public RestView<ProjectResource> list() throws RestApiException {
    throw new NotImplementedException();
  }

  @Override
  public ReplicationStatusProjectRemoteResource parse(ProjectResource parent, IdString id)
      throws ResourceNotFoundException, Exception {
    Project.NameKey projectNameKey = parent.getNameKey();
    String remoteStanza = id.get();

    Set<String> remoteUrls =
        replicationConfigParser.remoteUrlsForStanza(remoteStanza, projectNameKey);

    if (!remoteUrls.isEmpty()) {
      return new ReplicationStatusProjectRemoteResource(projectNameKey, remoteStanza, remoteUrls);
    }

    throw new ResourceNotFoundException(
        String.format(
            "Could not find remote %s that replicates project %s",
            remoteStanza, projectNameKey.get()));
  }

  @Override
  public DynamicMap<RestView<ReplicationStatusProjectRemoteResource>> views() {
    return views;
  }
}
