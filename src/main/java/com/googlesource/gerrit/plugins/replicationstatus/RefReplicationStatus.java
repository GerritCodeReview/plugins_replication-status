// Copyright (C) 2021 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.replicationstatus;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.gerrit.entities.Project;
import com.google.gerrit.proto.Protos;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.googlesource.gerrit.plugins.replicationstatus.proto.Cache;
import com.googlesource.gerrit.plugins.replicationstatus.proto.Cache.RefReplicationStatusProto;
import com.googlesource.gerrit.plugins.replicationstatus.proto.Cache.ReplicationStatusKeyProto;
import java.util.Map;
import java.util.stream.Collectors;

@AutoValue
public abstract class RefReplicationStatus {
  static final String CACHE_NAME = "replication_status";

  static RefReplicationStatus create(Map<String, RemoteReplicationStatus> remoteStatus) {
    return new AutoValue_RefReplicationStatus(remoteStatus);
  }

  public abstract Map<String, RemoteReplicationStatus> remoteStatus();

  @Memoized
  public boolean hasFailures() {
    return remoteStatus().values().stream().anyMatch(r -> r.status().isFailure());
  }

  @AutoValue
  public abstract static class Key {
    static Key create(Project.NameKey projectName, String ref) {
      return new AutoValue_RefReplicationStatus_Key(projectName, ref);
    }

    abstract Project.NameKey projectName();

    abstract String ref();

    enum Serializer implements CacheSerializer<RefReplicationStatus.Key> {
      INSTANCE;

      @Override
      public byte[] serialize(RefReplicationStatus.Key object) {
        return Protos.toByteArray(
            ReplicationStatusKeyProto.newBuilder()
                .setProject(object.projectName().get())
                .setRef(object.ref())
                .build());
      }

      @Override
      public RefReplicationStatus.Key deserialize(byte[] in) {
        ReplicationStatusKeyProto proto =
            Protos.parseUnchecked(ReplicationStatusKeyProto.parser(), in);
        return RefReplicationStatus.Key.create(Project.nameKey(proto.getProject()), proto.getRef());
      }
    }
  }

  enum Serializer implements CacheSerializer<RefReplicationStatus> {
    INSTANCE;

    @Override
    public byte[] serialize(RefReplicationStatus object) {
      RefReplicationStatusProto.Builder protoBuilder = RefReplicationStatusProto.newBuilder();
      for (Map.Entry<String, RemoteReplicationStatus> remote : object.remoteStatus().entrySet()) {
        RemoteReplicationStatus remoteValue = remote.getValue();
        protoBuilder.putRemoteStatus(
            remote.getKey(),
            Cache.RemoteReplicationStatusProto.newBuilder()
                .setWhen(remoteValue.when())
                .setStatus(remoteValue.status().name())
                .build());
      }
      return Protos.toByteArray(protoBuilder.build());
    }

    @Override
    public RefReplicationStatus deserialize(byte[] in) {
      RefReplicationStatusProto proto =
          Protos.parseUnchecked(RefReplicationStatusProto.parser(), in);

      final Map<String, RemoteReplicationStatus> statusMap =
          proto.getRemoteStatusMap().entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      e -> {
                        Cache.RemoteReplicationStatusProto remoteStatusProto = e.getValue();
                        return RemoteReplicationStatus.create(
                            RemoteReplicationStatus.ReplicationStatusResult.valueOf(
                                remoteStatusProto.getStatus()),
                            remoteStatusProto.getWhen());
                      }));

      return RefReplicationStatus.create(statusMap);
    }
  }
}
