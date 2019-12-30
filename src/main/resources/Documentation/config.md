Broker WebSession Plugin Configuration
======================

@PLUGIN@ parameters can be configured using Gerrit config file: $site_dir/etc/gerrit.config.

Sample config
---------------------

```
[plugin "websession-broker"]
        webSessionTopic = gerrit_web_session
```

Configuration parameters
---------------------

`plugin.websession-broker.webSessionTopic`
:   Name of the topic to use for publishing web session events.
    Default: gerrit\_web\_session