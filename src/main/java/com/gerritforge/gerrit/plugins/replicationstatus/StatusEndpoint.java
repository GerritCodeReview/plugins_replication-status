// Copyright (C) 2019 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class StatusEndpoint implements RestReadView<ConfigResource> {

  @Inject
  public StatusEndpoint() {}

  @Override
  public Response<Map<String, Object>> apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    long ts = System.currentTimeMillis();
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("ts", new Long(ts));
    return Response.withStatusCode(HttpServletResponse.SC_OK, result);
  }
}
