# Config

The plugin itself has no specific configuration, however some Gerrit specific
settings are relevant.

## Cache

This plugin relies on a persistent cache to store replication status
information, the global cache configuration settings apply.

Please look at
the [gerrit cache documentation](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#cache)
for more information on this.

In particular, to define the lifespan of replication-status entries in the
cache, look at
the [maxAge](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#cache.name.maxAge)
documentation.

## Gerrit instanceId

[gerrit.instanceId](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#gerrit.instanceId)
is expected to be populated. This is needed to the plugin to discriminate
between events produced by the current instances versus events produced by
different instances.

Installing this plugin on Gerrit instances not having `gerrit.instanceId`
populated will prevent this plugin from loading.
