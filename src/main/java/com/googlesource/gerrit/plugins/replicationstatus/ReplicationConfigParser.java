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

import static com.googlesource.gerrit.plugins.replication.ReplicationFileBasedConfig.replaceName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.gerrit.entities.Project;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.ReplicationFileBasedConfig;
import com.googlesource.gerrit.plugins.replication.ReplicationFilter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public class ReplicationConfigParser {
  private final Config replicationConfig;

  @Inject
  ReplicationConfigParser(ReplicationFileBasedConfig replicationConfig) {
    this.replicationConfig = replicationConfig.getConfig();
  }

  @VisibleForTesting
  ReplicationConfigParser(Config replicationConfig) {
    this.replicationConfig = replicationConfig;
  }

  Set<String> remoteUrlsForStanza(String remoteStanza, Project.NameKey projectNameKey)
      throws URISyntaxException {
    List<String> urls =
        Arrays.asList(replicationConfig.getStringList("remote", remoteStanza, "url"));
    List<String> projects =
        Arrays.asList(replicationConfig.getStringList("remote", remoteStanza, "projects"));
    String remoteNameStyle =
        MoreObjects.firstNonNull(
            replicationConfig.getString("remote", remoteStanza, "remoteNameStyle"), "slash");

    List<URIish> urIs = (new RemoteConfig(replicationConfig, remoteStanza)).getURIs();

    if (!urls.isEmpty() && (new ReplicationFilter(projects).matches(projectNameKey))) {
      return urIs.stream()
          .map(
              template ->
                  resolveNodeName(getURI(remoteNameStyle, template, projectNameKey, projects)))
          .collect(Collectors.toSet());
    }

    return Collections.emptySet();
  }

  private URIish getURI(
      String remoteNameStyle, URIish template, Project.NameKey project, List<String> projects) {
    String name = project.get();
    if (needsUrlEncoding(template)) {
      name = encode(name);
    }
    if (remoteNameStyle.equals("dash")) {
      name = name.replace("/", "-");
    } else if (remoteNameStyle.equals("underscore")) {
      name = name.replace("/", "_");
    } else if (remoteNameStyle.equals("basenameOnly")) {
      name = FilenameUtils.getBaseName(name);
    } else if (!remoteNameStyle.equals("slash")) {
    }
    String replacedPath = replaceName(template.getPath(), name, isSingleProjectMatch(projects));
    return (replacedPath != null) ? template.setPath(replacedPath) : template;
  }

  private static String resolveNodeName(URIish uri) {
    StringBuilder sb = new StringBuilder();
    if (uri.isRemote()) {
      sb.append(uri.getHost());
      if (uri.getPort() != -1) {
        sb.append(":");
        sb.append(uri.getPort());
      }
    } else {
      sb.append(uri.getPath());
    }
    return sb.toString();
  }

  private static boolean needsUrlEncoding(URIish uri) {
    return "http".equalsIgnoreCase(uri.getScheme())
        || "https".equalsIgnoreCase(uri.getScheme())
        || "amazon-s3".equalsIgnoreCase(uri.getScheme());
  }

  private static String encode(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8").replaceAll("%2[fF]", "/").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isSingleProjectMatch(List<String> projects) {
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
}
