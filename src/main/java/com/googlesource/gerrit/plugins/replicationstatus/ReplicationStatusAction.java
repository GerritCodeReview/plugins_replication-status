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

import static com.googlesource.gerrit.plugins.replication.PushResultProcessing.resolveNodeName;
import static com.googlesource.gerrit.plugins.replication.ReplicationFileBasedConfig.replaceName;
import static com.googlesource.gerrit.plugins.replicationstatus.ReplicationStatus.CACHE_NAME;

import com.google.common.cache.Cache;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
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
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.replication.ReplicationConfig;
import com.googlesource.gerrit.plugins.replicationstatus.ProjectReplicationStatus.ProjectReplicationStatusResult;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

class ReplicationStatusAction implements RestReadView<ReplicationStatusProjectRemoteResource> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PermissionBackend permissionBackend;
  private final GitRepositoryManager repoManager;
  private final Cache<ReplicationStatus.Key, ReplicationStatus> replicationStatusCache;
  private final List<RemoteURLConfiguration> remoteConfigurations;

  @Inject
  ReplicationStatusAction(
      PermissionBackend permissionBackend,
      GitRepositoryManager repoManager,
      @Named(CACHE_NAME) Cache<ReplicationStatus.Key, ReplicationStatus> replicationStatusCache,
      ReplicationConfig replicationConfig,
      ConfigParser configParser)
      throws ConfigInvalidException {
    this.permissionBackend = permissionBackend;
    this.repoManager = repoManager;
    this.replicationStatusCache = replicationStatusCache;
    this.remoteConfigurations = configParser.parseRemotes(replicationConfig.getConfig());
  }

  @Override
  public Response<ProjectReplicationStatus> apply(ReplicationStatusProjectRemoteResource resource)
      throws AuthException, PermissionBackendException, BadRequestException,
          ResourceConflictException, IOException {

    Project.NameKey projectNameKey = resource.getProjectNameKey();
    String remoteName = resource.getRemote();
    Optional<RemoteURLConfiguration> remoteConfig =
        remoteConfigurations.stream()
            .filter(config -> config.getName().equals(remoteName))
            .findFirst();

    checkIsOwnerOrAdmin(projectNameKey);

    Map<String, RemoteReplicationStatus> remoteStatuses = new HashMap<>();

    try (Repository git = repoManager.openRepository(projectNameKey)) {
      List<Ref> refs = git.getRefDatabase().getRefs();

      remoteConfig.ifPresent(
          config ->
              config.getUrls().stream()
                  .forEach(
                      url -> {
                        getTargetURL(projectNameKey, config, url)
                            .ifPresent(
                                uri -> {
                                  Map<String, ReplicationStatus> refStatuses =
                                      getRefStatuses(projectNameKey, refs, uri);

                                  remoteStatuses.put(
                                      uri, RemoteReplicationStatus.create(refStatuses));
                                });
                      }));

      ProjectReplicationStatus.ProjectReplicationStatusResult overallStatus =
          getOverallStatus(remoteStatuses);
      ProjectReplicationStatus projectStatus =
          ProjectReplicationStatus.create(remoteStatuses, overallStatus, projectNameKey.get());

      return Response.ok(projectStatus);

    } catch (RepositoryNotFoundException e) {
      throw new BadRequestException(
          String.format("Project %s does not exist", projectNameKey.get()));
    }
  }

  private ProjectReplicationStatusResult getOverallStatus(
      Map<String, RemoteReplicationStatus> remoteStatuses) {
    return remoteStatuses.values().stream()
        .flatMap(status -> status.status().values().stream())
        .filter(status -> status.isFailure())
        .findFirst()
        .map(status -> ProjectReplicationStatus.ProjectReplicationStatusResult.FAILED)
        .orElse(ProjectReplicationStatus.ProjectReplicationStatusResult.OK);
  }

  private Map<String, ReplicationStatus> getRefStatuses(
      Project.NameKey projectNameKey, List<Ref> refs, String uri) {
    Map<String, ReplicationStatus> refStatuses = new HashMap<>();
    for (Ref r : refs) {
      ReplicationStatus.Key key = ReplicationStatus.Key.create(projectNameKey, uri, r.getName());
      ReplicationStatus replicationStatus = replicationStatusCache.getIfPresent(key);

      if (replicationStatus != null) {
        refStatuses.put(r.getName(), replicationStatus);
      }
    }
    return refStatuses;
  }

  private Optional<String> getTargetURL(
      Project.NameKey projectNameKey, RemoteURLConfiguration config, String url) {
    try {
      return Optional.of(resolveNodeName(getURI(config, new URIish(url), projectNameKey)));
    } catch (URISyntaxException e) {
      logger.atSevere().withCause(e).log(
          "Cannot resolve target URI for template: %s and project name: %s", url, projectNameKey);
      return Optional.empty();
    }
  }

  /**
   * This method was copied from replication plugin where the method is in protected scope {@link
   * com.googlesource.gerrit.plugins.replication.Destination#getURI(URIish, NameKey)}
   */
  @SuppressWarnings("javadoc")
  private URIish getURI(RemoteURLConfiguration config, URIish template, Project.NameKey project) {
    String name = project.get();
    if (needsUrlEncoding(template)) {
      name = encode(name);
    }
    String remoteNameStyle = config.getRemoteNameStyle();
    if (remoteNameStyle.equals("dash")) {
      name = name.replace("/", "-");
    } else if (remoteNameStyle.equals("underscore")) {
      name = name.replace("/", "_");
    } else if (remoteNameStyle.equals("basenameOnly")) {
      name = FilenameUtils.getBaseName(name);
    } else if (!remoteNameStyle.equals("slash")) {
      logger.atFine().log("Unknown remoteNameStyle: %s, falling back to slash", remoteNameStyle);
    }
    String replacedPath = replaceName(template.getPath(), name, config.isSingleProjectMatch());
    return (replacedPath != null) ? template.setPath(replacedPath) : template;
  }

  /**
   * This method was copied from replication plugin where the method is in protected scope {@link
   * com.googlesource.gerrit.plugins.replication.Destination#needsUrlEncoding(URIish)}
   */
  @SuppressWarnings("javadoc")
  private static boolean needsUrlEncoding(URIish uri) {
    return "http".equalsIgnoreCase(uri.getScheme())
        || "https".equalsIgnoreCase(uri.getScheme())
        || "amazon-s3".equalsIgnoreCase(uri.getScheme());
  }

  /**
   * This method was copied from replication plugin where the method is in protected scope {@link
   * com.googlesource.gerrit.plugins.replication.Destination#encode(String)}
   */
  @SuppressWarnings("javadoc")
  private static String encode(String str) {
    try {
      // Some cleanup is required. The '/' character is always encoded as %2F
      // however remote servers will expect it to be not encoded as part of the
      // path used to the repository. Space is incorrectly encoded as '+' for this
      // context. In the path part of a URI space should be %20, but in form data
      // space is '+'. Our cleanup replace fixes these two issues.
      return URLEncoder.encode(str, "UTF-8").replaceAll("%2[fF]", "/").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
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
