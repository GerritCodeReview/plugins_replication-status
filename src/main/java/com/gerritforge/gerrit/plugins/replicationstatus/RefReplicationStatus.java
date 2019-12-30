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

package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.auto.value.AutoValue;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

@AutoValue
public abstract class RefReplicationStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  static final String CACHE_NAME = "replication_status";

  static RefReplicationStatus create(Map<String, RemoteReplicationStatus> remoteStatus) {
    return new AutoValue_RefReplicationStatus(remoteStatus);
  }

  public abstract Map<String, RemoteReplicationStatus> remoteStatus();

  public boolean hasFailures() {
    return remoteStatus().values().stream().anyMatch(r -> r.status().isFailure());
  }

  enum Serializer implements CacheSerializer<RefReplicationStatus> {
    INSTANCE;

    @Override
    public byte[] serialize(RefReplicationStatus object) {
      try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(stream)) {
        oos.writeObject(object);
        oos.flush();
        return stream.toByteArray();
      } catch (IOException e) {
        throw new IllegalArgumentException(
            String.format("Failed to serialize cache value: %s", object), e);
      }
    }

    @Override
    public RefReplicationStatus deserialize(byte[] in) {
      try (ByteArrayInputStream stream = new ByteArrayInputStream(in);
          ObjectInputStream oin = new ObjectInputStream(stream)) {
        return (RefReplicationStatus) oin.readObject();
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to deserialize cache value", e);
      }
    }
  }

  @AutoValue
  public abstract static class Key implements Serializable {
    private static final long serialVersionUID = 1L;

    static Key create(Project.NameKey projectName, String ref) {
      return new AutoValue_RefReplicationStatus_Key(projectName, ref);
    }

    abstract Project.NameKey projectName();

    abstract String ref();

    enum Serializer implements CacheSerializer<Key> {
      INSTANCE;

      @Override
      public byte[] serialize(Key object) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(stream)) {
          oos.writeObject(object);
          oos.flush();
          return stream.toByteArray();
        } catch (IOException e) {
          throw new IllegalArgumentException(
              String.format("Failed to serialize cache key: %s", object), e);
        }
      }

      @Override
      public Key deserialize(byte[] in) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(in);
            ObjectInputStream oin = new ObjectInputStream(stream)) {
          return (Key) oin.readObject();
        } catch (Exception e) {
          throw new IllegalArgumentException("Failed to deserialize cache key", e);
        }
      }
    }
  }
}
