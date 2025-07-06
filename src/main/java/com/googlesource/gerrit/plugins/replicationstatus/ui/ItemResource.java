package com.googlesource.gerrit.plugins.replicationstatus.ui;

import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.TypeLiteral;

public class ItemResource extends ConfigResource {
  public static final TypeLiteral<RestView<ItemResource>> ITEM_KIND =
      new TypeLiteral<RestView<ItemResource>>() {};
}
