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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

class ConfigParser {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  List<RemoteURLConfiguration> parseRemotes(Config config) throws ConfigInvalidException {

    if (config.getSections().isEmpty()) {
      logger.atWarning().log("Replication config does not exist or it's empty");
      return Collections.emptyList();
    }

    ImmutableList.Builder<RemoteURLConfiguration> confs = ImmutableList.builder();
    for (RemoteConfig c : allRemotes(config)) {
      if (c.getURIs().isEmpty()) {
        continue;
      }

      RemoteURLConfiguration remoteURLConfiguration = new RemoteURLConfiguration(c, config);

      if (!remoteURLConfiguration.isSingleProjectMatch()) {
        for (URIish u : c.getURIs()) {
          if (u.getPath() == null || !u.getPath().contains("${name}")) {
            throw new ConfigInvalidException(
                String.format(
                    "remote.%s.url \"%s\" lacks ${name} placeholder in %s",
                    c.getName(), u, config));
          }
        }
      }

      confs.add(remoteURLConfiguration);
    }

    return confs.build();
  }

  private static List<RemoteConfig> allRemotes(Config cfg) throws ConfigInvalidException {
    Set<String> names = cfg.getSubsections("remote");
    List<RemoteConfig> result = Lists.newArrayListWithCapacity(names.size());
    for (String name : names) {
      try {
        result.add(new RemoteConfig(cfg, name));
      } catch (URISyntaxException e) {
        throw new ConfigInvalidException(
            String.format("remote %s has invalid URL in %s", name, cfg), e);
      }
    }
    return result;
  }
}
