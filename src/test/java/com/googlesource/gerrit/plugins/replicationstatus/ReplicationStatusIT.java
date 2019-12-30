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
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import com.google.gerrit.server.config.SitePaths;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.RefReplicatedEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "replication-status",
    sysModule = "com.googlesource.gerrit.plugins.replicationstatus.Module")
public class ReplicationStatusIT extends LightweightPluginDaemonTest {
  private static final String REF_MASTER = Constants.R_HEADS + Constants.MASTER;

  private static final Gson gson = newGson();

  @Inject protected SitePaths sitePaths;
  @Inject private ProjectOperations projectOperations;

  private EventHandler eventHandler;
  private ReplicationConfigParser replicationConfigParser;
  private FileBasedConfig config;
  protected Path gitPath;

  @Before
  public void setUp() throws IOException {
    initConfig();
    replicationConfigParser = new ReplicationConfigParser(config);
    eventHandler = plugin.getSysInjector().getInstance(EventHandler.class);
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeOKSuccessForAdminUsers() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    RestResponse result = adminRestSession.get(endpoint(project, remote));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project, remote));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeOKSuccessForProjectOwners() throws Exception {
    makeProjectOwner(user, project);
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());

    RestResponse result = userRestSession.get(endpoint(project, remote));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project, remote));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeForbiddenForNonProjectOwners() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());

    RestResponse result = userRestSession.get(endpoint(project, remote));
    result.assertForbidden();

    assertThat(result.getEntityContent()).contains("Administrate Server or Project owner required");
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldBeForbiddenForAnonymousUsers() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());

    RestResponse result = anonymousRestSession.get(endpoint(project, remote));
    result.assertForbidden();

    assertThat(result.getEntityContent()).contains("Administrate Server or Project owner required");
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldNotReportStatusOfReplicationsGeneratedOnDifferentNodes() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    eventHandler.onEvent(
        successReplicatedEvent("testInstanceId-2", System.currentTimeMillis(), remote));

    RestResponse result = adminRestSession.get(endpoint(project, remote));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project, remote));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldReturnSuccessfulProjectReplicationStatus() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    long eventCreatedOn = System.currentTimeMillis();

    eventHandler.onEvent(successReplicatedEvent("testInstanceId-1", eventCreatedOn, remote));
    RestResponse result = adminRestSession.get(endpoint(project, remote));

    result.assertOK();
    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(successReplicationStatus(remote, project, eventCreatedOn));
  }

  @Test
  public void shouldConsumeEventsThatHaveNoInstanceId() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    long eventCreatedOn = System.currentTimeMillis();

    eventHandler.onEvent(successReplicatedEvent(null, eventCreatedOn, remote));
    RestResponse result = adminRestSession.get(endpoint(project, remote));

    result.assertOK();
    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(successReplicationStatus(remote, project, eventCreatedOn));
  }

  @Test
  public void shouldNotConsumeEventsWhenNodeInstanceIdIsNullButEventHasIt() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    eventHandler.onEvent(
        successReplicatedEvent("testInstanceId-2", System.currentTimeMillis(), remote));

    RestResponse result = adminRestSession.get(endpoint(project, remote));
    result.assertOK();

    assertThat(contentWithoutMagicJson(result)).isEqualTo(emptyReplicationStatus(project, remote));
  }

  @Test
  public void shouldConsumeEventsWhenBothNodeAndEventHaveNoInstanceId() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    long eventCreatedOn = System.currentTimeMillis();

    eventHandler.onEvent(successReplicatedEvent(null, eventCreatedOn, remote));
    RestResponse result = adminRestSession.get(endpoint(project, remote));

    result.assertOK();
    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(successReplicationStatus(remote, project, eventCreatedOn));
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId-1")
  public void shouldReturnFailedProjectReplicationStatus() throws Exception {
    String remote = remoteNameFor(project);
    setReplicationDestination(remote, ImmutableList.of("-mirror"), Optional.empty());
    long eventCreatedOn = System.currentTimeMillis();

    eventHandler.onEvent(failedReplicatedEvent("testInstanceId-1", eventCreatedOn, remote));
    RestResponse result = adminRestSession.get(endpoint(project, remote));

    result.assertStatus(SC_INTERNAL_SERVER_ERROR);
    assertThat(contentWithoutMagicJson(result))
        .isEqualTo(failedReplicationStatus(remote, project, eventCreatedOn));
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

  private RefReplicatedEvent successReplicatedEvent(
      @Nullable String instanceId, long when, String remote) throws URISyntaxException {

    ArrayList<String> remoteUrls =
        Lists.newArrayList(replicationConfigParser.remoteUrlsForStanza(remote, project));

    assertThat(remoteUrls.size()).isEqualTo(1);

    return replicatedEvent(
        instanceId,
        when,
        REF_MASTER,
        remoteUrls.get(0),
        RefPushResult.SUCCEEDED,
        RemoteRefUpdate.Status.OK);
  }

  private RefReplicatedEvent failedReplicatedEvent(
      @Nullable String instanceId, long when, String remote) throws URISyntaxException {
    ArrayList<String> remoteUrls =
        Lists.newArrayList(replicationConfigParser.remoteUrlsForStanza(remote, project));

    assertThat(remoteUrls.size()).isEqualTo(1);

    return replicatedEvent(
        instanceId,
        when,
        REF_MASTER,
        remoteUrls.get(0),
        RefPushResult.FAILED,
        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD);
  }

  private static String endpoint(Project.NameKey project, String remote) {
    return String.format("/projects/%s/remotes/%s/replication-status", project.get(), remote);
  }

  private String emptyReplicationStatus(Project.NameKey project, String remote)
      throws URISyntaxException {
    ArrayList<String> remoteUrls =
        Lists.newArrayList(replicationConfigParser.remoteUrlsForStanza(remote, project));

    assertThat(remoteUrls.size()).isEqualTo(1);

    return gson.toJson(
        ProjectReplicationStatus.create(
            ImmutableMap.of(
                remoteUrls.get(0), RemoteReplicationStatus.create(Collections.emptyMap())),
            ProjectReplicationStatus.ProjectReplicationStatusResult.OK,
            project.get()));
  }

  private String successReplicationStatus(String remote, Project.NameKey project, long when)
      throws URISyntaxException {
    return projectReplicationStatus(
        remote,
        project,
        when,
        ProjectReplicationStatus.ProjectReplicationStatusResult.OK,
        ReplicationStatus.ReplicationStatusResult.SUCCEEDED);
  }

  private String failedReplicationStatus(String remote, Project.NameKey project, long when)
      throws URISyntaxException {
    return projectReplicationStatus(
        remote,
        project,
        when,
        ProjectReplicationStatus.ProjectReplicationStatusResult.FAILED,
        ReplicationStatus.ReplicationStatusResult.FAILED);
  }

  private String projectReplicationStatus(
      String remote,
      Project.NameKey project,
      long when,
      ProjectReplicationStatus.ProjectReplicationStatusResult projectReplicationStatusResult,
      ReplicationStatus.ReplicationStatusResult replicationStatusResult)
      throws URISyntaxException {

    ArrayList<String> remoteUrls =
        Lists.newArrayList(replicationConfigParser.remoteUrlsForStanza(remote, project));

    assertThat(remoteUrls.size()).isEqualTo(1);

    return gson.toJson(
        ProjectReplicationStatus.create(
            ImmutableMap.of(
                remoteUrls.get(0),
                RemoteReplicationStatus.create(
                    ImmutableMap.of(
                        REF_MASTER, ReplicationStatus.create(replicationStatusResult, when)))),
            projectReplicationStatusResult,
            project.get()));
  }

  private void setReplicationDestination(
      String remoteName, List<String> replicaSuffixes, Optional<String> project)
      throws IOException {

    List<String> replicaUrls =
        replicaSuffixes.stream()
            .map(suffix -> gitPath.resolve("${name}" + suffix + ".git").toString())
            .collect(toList());

    config.setStringList("remote", remoteName, "url", replicaUrls);
    project.ifPresent(prj -> config.setString("remote", remoteName, "projects", prj));
    config.save();
  }

  private void initConfig() throws IOException {
    if (config == null) {
      gitPath = sitePaths.site_path.resolve("git");
      config =
          new FileBasedConfig(
              sitePaths.etc_dir.resolve("replication.config").toFile(), FS.DETECTED);
      config.save();
    }
  }

  private String remoteNameFor(Project.NameKey projectNameKey) {
    return String.join("-", project.get(), "remote");
  }
}
