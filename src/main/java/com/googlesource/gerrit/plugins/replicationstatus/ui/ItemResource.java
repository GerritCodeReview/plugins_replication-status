package com.googlesource.gerrit.plugins.replicationstatus.ui;

import com.google.gerrit.extensions.restapi.RestResource;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.inject.TypeLiteral;

public class ItemResource implements RestResource {
  public static final TypeLiteral<RestView<ItemResource>> ITEM_KIND =
      new TypeLiteral<RestView<ItemResource>>() {};
}
