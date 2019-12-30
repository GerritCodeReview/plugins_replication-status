load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)
load("//tools/bzl:junit.bzl", "junit_tests")

gerrit_plugin(
    name = "replication-status",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    deps = [],
    manifest_entries = [
        "Gerrit-PluginName: replication-status",
        "Gerrit-Module: com.gerritforge.gerrit.plugins.replicationstatus.Module",
        "Implementation-Title: Replication Status",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/plugins_replication-status",
    ],
)

junit_tests(
    name = "replicationstatus_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        ":replicationstatus__plugin_test_deps",
    ],
)

java_library(
    name = "replicationstatus__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":replication-status__plugin",
    ],
)
