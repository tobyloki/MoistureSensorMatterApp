/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iotgroup2.matterapp.shared.matter.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.iotgroup2.matterapp.Devices
import java.io.InputStream
import java.io.OutputStream

object DevicesSerializer : Serializer<Devices> {

  override val defaultValue: Devices = Devices.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): Devices {
    try {
      return Devices.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override suspend fun writeTo(t: Devices, output: OutputStream) = t.writeTo(output)
}

val Context.devicesDataStore: DataStore<Devices> by
    dataStore(fileName = "devices_store.proto", serializer = DevicesSerializer)