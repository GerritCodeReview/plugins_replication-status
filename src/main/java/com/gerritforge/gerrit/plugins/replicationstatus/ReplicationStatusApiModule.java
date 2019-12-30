// Copyright (C) 2014 The Android Open Source Project
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

import static com.google.gerrit.server.config.ConfigResource.CONFIG_KIND;

import com.google.gerrit.extensions.restapi.RestApiModule;
import com.gerritforge.gerrit.plugins.replicationstatus.StatusEndpoint;

class ReplicationStatusApiModule extends RestApiModule {
  //XXX Not strictly necessary: returns the same metrics returned when hitting the prometheus endpoint, but in JSON format
  // The logic still need to be implemented. Is it worth doing it?
  @Override
  protected void configure() {
    get(CONFIG_KIND, "status").to(StatusEndpoint.class);
  }
}
