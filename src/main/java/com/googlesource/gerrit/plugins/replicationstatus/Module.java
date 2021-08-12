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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;

class Module extends LifecycleModule {

  private final SitePaths site;

  @Inject
  public Module(SitePaths site) {
    this.site = site;
  }

  @Override
  protected void configure() {
    DynamicSet.bind(binder(), EventListener.class).to(EventHandler.class);
    install(new ReplicationStatusApiModule(site));
    install(new ReplicationStatusCacheModule());
  }
}
