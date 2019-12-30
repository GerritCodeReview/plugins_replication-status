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

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.permissions.GlobalPermission;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

class ReplicationStatusAction implements RestReadView<ProjectResource> {

  private final PermissionBackend permissionBackend;

  @Inject
  ReplicationStatusAction(PermissionBackend permissionBackend) {
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<ReplicationStatus> apply(ProjectResource resource)
      throws AuthException, PermissionBackendException, BadRequestException,
          ResourceConflictException {

    checkIsOwner(resource.getNameKey());

    return Response.ok(ReplicationStatus.create("test"));
  }

  private void checkIsOwner(Project.NameKey project) throws AuthException {
    if (!permissionBackend.currentUser().testOrFalse(GlobalPermission.ADMINISTRATE_SERVER)
        && !permissionBackend
            .currentUser()
            .project(project)
            .testOrFalse(ProjectPermission.WRITE_CONFIG)) {
      throw new AuthException("Administrate Server or Project owner required");
    }
  }
}
