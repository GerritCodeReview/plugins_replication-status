# Build

This plugin is built with Bazel in-tree build.

## Build in Gerrit tree

Create a symbolic link of the repository source to the Gerrit source tree
/plugins/replication-status directory.

Example:

```shell
git clone https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/plugins/replication-status
cd gerrit/plugins ln -s ../../replication-status replication-status
```

From the Gerrit source tree issue the command

```shell
bazelsk build plugins/replication-status
```

The jar file is created
under `basel-bin/plugins/replication-status/replication-status.jar`

To execute the tests run

```shell
bazelisk test plugins/replication-status/...
```

from the Gerrit source tree.