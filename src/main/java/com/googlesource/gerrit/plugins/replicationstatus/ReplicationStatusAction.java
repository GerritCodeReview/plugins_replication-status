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

import static com.googlesource.gerrit.plugins.replicationstatus.RefReplicationStatus.CACHE_NAME;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import com.google.common.cache.Cache;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.permissions.GlobalPermission;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

class ReplicationStatusAction implements RestReadView<ProjectResource> {

  private final PermissionBackend permissionBackend;
  private final GitRepositoryManager repoManager;
  private final Cache<RefReplicationStatus.Key, RefReplicationStatus> replicationStatusCache;

  @Inject
  ReplicationStatusAction(
      PermissionBackend permissionBackend,
      GitRepositoryManager repoManager,
      @Named(CACHE_NAME)
          Cache<RefReplicationStatus.Key, RefReplicationStatus> replicationStatusCache) {
    this.permissionBackend = permissionBackend;
    this.repoManager = repoManager;
    this.replicationStatusCache = replicationStatusCache;
  }

  @Override
  public Response<ProjectReplicationStatus> apply(ProjectResource resource)
      throws AuthException, PermissionBackendException, BadRequestException,
          ResourceConflictException, IOException {

    Project.NameKey projectNameKey = resource.getNameKey();

    checkIsOwnerOrAdmin(projectNameKey);

    ProjectReplicationStatus.ProjectReplicationStatusResult overallStatus =
        ProjectReplicationStatus.ProjectReplicationStatusResult.OK;
    Map<String, RefReplicationStatus> refStatuses = new HashMap<>();
    try (Repository git = repoManager.openRepository(projectNameKey)) {
      for (Ref r : git.getRefDatabase().getRefs()) {
        RefReplicationStatus refReplicationStatus =
            replicationStatusCache.getIfPresent(
                RefReplicationStatus.Key.create(projectNameKey, r.getName()));

        if (refReplicationStatus != null) {
          refStatuses.put(r.getName(), refReplicationStatus);
          if (refReplicationStatus.hasFailures()) {
            overallStatus = ProjectReplicationStatus.ProjectReplicationStatusResult.FAILED;
          }
        }
      }

      ProjectReplicationStatus projectStatus =
          ProjectReplicationStatus.create(refStatuses, overallStatus, resource.getName());

      return overallStatus.isFailure()
          ? Response.withStatusCode(SC_INTERNAL_SERVER_ERROR, projectStatus)
          : Response.ok(projectStatus);

    } catch (RepositoryNotFoundException e) {
      throw new BadRequestException(String.format("Project %s does not exist", resource.getName()));
    }
  }

  private void checkIsOwnerOrAdmin(Project.NameKey project) throws AuthException {
    if (!permissionBackend.currentUser().testOrFalse(GlobalPermission.ADMINISTRATE_SERVER)
        && !permissionBackend
            .currentUser()
            .project(project)
            .testOrFalse(ProjectPermission.WRITE_CONFIG)) {
      throw new AuthException("Administrate Server or Project owner required");
    }
  }
}
