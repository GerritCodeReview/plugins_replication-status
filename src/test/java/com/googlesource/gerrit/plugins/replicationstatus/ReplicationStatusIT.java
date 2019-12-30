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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allow;
import static com.googlesource.gerrit.plugins.replication.ReplicationState.RefPushResult;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.common.data.Permission;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.groups.GroupInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.httpd.restapi.RestApiServlet;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.RefReplicatedEvent;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "replication-status",
    sysModule = "com.googlesource.gerrit.plugins.replicationstatus.Module")
public class ReplicationStatusIT extends LightweightPluginDaemonTest {
  private static final String REF_MASTER = Constants.R_HEADS + Constants.MASTER;
  private static final String REMOTE = "ssh://some.remote.host";

  private static final Gson gson = newGson();

  @Inject private ProjectOperations projectOperations;

  private EventHandler eventHandler;

  private static String endpoint(Project.NameKey project) {
    return String.format("/projects/%s/replication-status", project.get());
  }

  private static String emptyReplicationStatus(Project.NameKey project) {
    return gson.toJson(
        ProjectReplicationStatus.create(
            Collections.emptyMap(),
            ProjectReplicationStatus.ProjectReplicationStatusResult.OK,
            project.get()));
  }

  private static String successReplicationStatus(Project.NameKey project, long when) {
    return projectReplicationStatus(
        project,
        when,
        ProjectReplicationStatus.ProjectReplicationStatusResult.OK,
        RemoteReplicationStatus.ReplicationStatusResult.SUCCEEDED);
  }

  private static String failedReplicationStatus(Project.NameKey project, long when) {
    return projectReplicationStatus(
        project,
        when,
        ProjectReplicationStatus.ProjectReplicationStatusResult.FAILED,
        RemoteReplicationStatus.ReplicationStatusResult.FAILED);
  }

  private static String projectReplicationStatus(
      Project.NameKey project,
      long when,
      ProjectReplicationStatus.ProjectReplicationStatusResult projectReplicationStatusResult,
      RemoteReplicationStatus.ReplicationStatusResult replicationStatusResult) {
    return gson.toJson(
        ProjectReplicationStatus.create(
            ImmutableMap.of(
                REF_MASTER,
                RefReplicationStatus.create(
                    ImmutableMap.of(
                        REMOTE, RemoteReplicationStatus.create(replicationStatusResult, when)))),
            projectReplicationStatusResult,
            project.get()));
  }

  @Before
  public void setUp() {
    eventHandler = plugin.getSysInjector().getInstance(EventHandler.class);
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeOKSuccessForAdminUsers() throws Exception {
    RestResponse result = adminRestSession.get(endpoint(project));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeOKSuccessForProjectOwners() throws Exception {
    makeProjectOwner(user, project);

    RestResponse result = userRestSession.get(endpoint(project));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeForbiddenForNonProjectOwners() throws Exception {
    RestResponse result = userRestSession.get(endpoint(project));
    result.assertForbidden();

    assertThat(result.getEntityContent()).contains("Administrate Server or Project owner required");
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeForbiddenForAnonymousUsers() throws Exception {
    RestResponse result = anonymousRestSession.get(endpoint(project));
    result.assertForbidden();

    assertThat(result.getEntityContent()).contains("Administrate Server or Project owner required");
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldNotReportStatusOfReplicationsGeneratedOnDifferentNodes() throws Exception {
    eventHandler.onEvent(successReplicatedEvent("testInstanceId-2", System.currentTimeMillis()));
    RestResponse result = adminRestSession.get(endpoint(project));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldReturnSuccessfulProjectReplicationStatus() throws Exception {
    long eventCreatedOn = System.currentTimeMillis();
    eventHandler.onEvent(successReplicatedEvent("testInstanceId-1", eventCreatedOn));
    RestResponse result = adminRestSession.get(endpoint(project));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(successReplicationStatus(project, eventCreatedOn));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldReturnFailedProjectReplicationStatus() throws Exception {
    long eventCreatedOn = System.currentTimeMillis();
    eventHandler.onEvent(failedReplicatedEvent("testInstanceId-1", eventCreatedOn));
    RestResponse result = adminRestSession.get(endpoint(project));
    result.assertStatus(SC_INTERNAL_SERVER_ERROR);

    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(failedReplicationStatus(project, eventCreatedOn));
  }

  private String contentWithoutMagicJson(RestResponse response) throws IOException {
    return response.getEntityContent().substring(RestApiServlet.JSON_MAGIC.length);
  }

  private AccountGroup.UUID createGroup(String name, TestAccount member) throws RestApiException {
    GroupInput groupInput = new GroupInput();
    groupInput.name = name(name);
    groupInput.members = Collections.singletonList(String.valueOf(member.id().get()));
    return AccountGroup.uuid(gApi.groups().create(groupInput).get().id);
  }

  private void makeProjectOwner(TestAccount user, Project.NameKey project) throws RestApiException {
    projectOperations
        .project(project)
        .forUpdate()
        .add(
            allow(Permission.OWNER)
                .ref(AccessSection.ALL)
                .group(createGroup("projectOwners", user)))
        .update();
  }

  private RefReplicatedEvent replicatedEvent(
      @Nullable String instanceId,
      long when,
      String ref,
      String remote,
      RefPushResult status,
      RemoteRefUpdate.Status refStatus) {
    RefReplicatedEvent replicatedEvent =
        new RefReplicatedEvent(project.get(), ref, remote, status, refStatus);
    replicatedEvent.instanceId = instanceId;
    replicatedEvent.eventCreatedOn = when;

    return replicatedEvent;
  }

  private RefReplicatedEvent successReplicatedEvent(@Nullable String instanceId, long when) {
    return replicatedEvent(
        instanceId, when, REF_MASTER, REMOTE, RefPushResult.SUCCEEDED, RemoteRefUpdate.Status.OK);
  }

  private RefReplicatedEvent failedReplicatedEvent(@Nullable String instanceId, long when) {
    return replicatedEvent(
        instanceId,
        when,
        REF_MASTER,
        REMOTE,
        RefPushResult.FAILED,
        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD);
  }
}
