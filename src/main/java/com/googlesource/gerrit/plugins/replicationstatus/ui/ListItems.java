package com.googlesource.gerrit.plugins.replicationstatus.ui;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.plugins.PluginResource;
import java.util.List;
import javax.inject.Inject;

public class ListItems implements RestReadView<PluginResource> {

  @Inject
  public ListItems() {}

  @Override
  public Response<List<ItemInfo>> apply(PluginResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    return Response.ok(List.of(new ItemInfo(1, "one")));
  }

  public static class ItemInfo {
    public long id;
    public String item;

    public ItemInfo(long id, String item) {
      this.id = id;
      this.item = item;
    }
  }
}
