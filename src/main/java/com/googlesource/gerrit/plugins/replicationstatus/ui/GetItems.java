package com.googlesource.gerrit.plugins.replicationstatus.ui;

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.plugins.PluginResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GetItems implements ChildCollection<PluginResource, ItemResource> {
  private final Provider<ListItems> list;
  private final DynamicMap<RestView<ItemResource>> views;

  @Inject
  public GetItems(Provider<ListItems> list, DynamicMap<RestView<ItemResource>> views) {
    this.list = list;
    this.views = views;
  }

  @Override
  public RestView<PluginResource> list() throws RestApiException {
    return list.get();
  }

  @Override
  public ItemResource parse(PluginResource parent, IdString id) throws ResourceNotFoundException, Exception {
    throw new ResourceNotFoundException(id);
  }


  @Override
  public DynamicMap<RestView<ItemResource>> views() {
    return views;
  }
}
