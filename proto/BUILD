load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "replication_status_cache_proto",
    srcs = ["cache.proto"],
)

java_proto_library(
    name = "replication_status_cache_java_proto",
    visibility = ["//visibility:public"],
    deps = [":replication_status_cache_proto"],
)
