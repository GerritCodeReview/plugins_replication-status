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

import com.google.gerrit.server.cache.serialize.CacheSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class JavaCacheSerializer<T> implements CacheSerializer<T> {

  @Override
  public byte[] serialize(T object) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(stream)) {
      oos.writeObject(object);
      oos.flush();
      return stream.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Serialization error: %s", object), e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T deserialize(byte[] in) {
    try (ByteArrayInputStream stream = new ByteArrayInputStream(in);
        ObjectInputStream oin = new ObjectInputStream(stream)) {
      return (T) oin.readObject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Deserialization error", e);
    }
  }
}
