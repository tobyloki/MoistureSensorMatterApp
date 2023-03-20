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

package com.iotgroup2.matterapp.shared.matter.chip

import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipStructs
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import timber.log.Timber

/**
 * Encapsulates the information of interest when querying a Matter device just after it has been
 * commissioned.
 */
data class DeviceMatterInfo(
    val endpoint: Int,
    val types: List<Long>,
    val serverClusters: List<Any>,
    val clientClusters: List<Any>
)

/** Singleton to facilitate access to Clusters functionality. */
@Singleton
class ClustersHelper @Inject constructor(private val chipClient: ChipClient) {

  // -----------------------------------------------------------------------------------------------
  // Convenience functions

  /** Fetches MatterDeviceInfo for each endpoint supported by the device. */
  suspend fun fetchDeviceMatterInfo(nodeId: Long): List<DeviceMatterInfo> {
      Timber.d("fetchDeviceMatterInfo(): nodeId [${nodeId}]")
      val matterDeviceInfoList = arrayListOf<DeviceMatterInfo>()
      val connectedDevicePtr =
          try {
              chipClient.getConnectedDevicePointer(nodeId)
          } catch (e: IllegalStateException) {
              Timber.e("Can't get connectedDevicePointer.")
              return emptyList()
          }
      fetchDeviceMatterInfo(nodeId, connectedDevicePtr, 0, matterDeviceInfoList)
      return matterDeviceInfoList
  }

    /** Fetches MatterDeviceInfo for a specific endpoint. */
    suspend fun fetchDeviceMatterInfo(
        nodeId: Long,
        connectedDevicePtr: Long,
        endpointInt: Int,
        matterDeviceInfoList: ArrayList<DeviceMatterInfo>
    ) {
        Timber.d("fetchDeviceMatterInfo(): nodeId [${nodeId}] endpoint [$endpointInt]")

        val partsListAttribute =
            readDescriptorClusterPartsListAttribute(connectedDevicePtr, endpointInt)
        Timber.d("partsListAttribute [${partsListAttribute}]")

        // DeviceListAttribute
        val deviceListAttribute =
            readDescriptorClusterDeviceListAttribute(connectedDevicePtr, endpointInt)
        val types = arrayListOf<Long>()
        deviceListAttribute.forEach { types.add(it.deviceType) }

        // ServerListAttribute
        val serverListAttribute =
            readDescriptorClusterServerListAttribute(connectedDevicePtr, endpointInt)
        val serverClusters = arrayListOf<Any>()
        serverListAttribute.forEach { serverClusters.add(it) }

        // ClientListAttribute
        val clientListAttribute =
            readDescriptorClusterClientListAttribute(connectedDevicePtr, endpointInt)
        val clientClusters = arrayListOf<Any>()
        clientListAttribute.forEach { clientClusters.add(it) }

        // Build the DeviceMatterInfo
        val deviceMatterInfo = DeviceMatterInfo(endpointInt, types, serverClusters, clientClusters)
        matterDeviceInfoList.add(deviceMatterInfo)

        // Recursive call for the parts supported by the endpoint.
        // For each part (endpoint)
        partsListAttribute?.forEach { part ->
            Timber.d("part [$part] is [${part.javaClass}]")
            val endpointInt =
                when (part) {
                    is Int -> part.toInt()
                    else -> return@forEach
                }
            Timber.d("Processing part [$part]")
            fetchDeviceMatterInfo(nodeId, connectedDevicePtr, endpointInt, matterDeviceInfoList)
        }
    }

  // -----------------------------------------------------------------------------------------------
  // DescriptorCluster functions

  /**
   * PartsListAttribute. These are the endpoints supported.
   *
   * ```
   * For example, on endpoint 0:
   *     sendReadPartsListAttribute part: [1]
   *     sendReadPartsListAttribute part: [2]
   * ```
   */
  suspend fun readDescriptorClusterPartsListAttribute(devicePtr: Long, endpoint: Int): List<Any>? {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
          .readPartsListAttribute(
              object : ChipClusters.DescriptorCluster.PartsListAttributeCallback {
                override fun onSuccess(values: MutableList<Int>?) {
                  continuation.resume(values)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  /**
   * DeviceListAttribute
   *
   * ```
   * For example, on endpoint 0:
   *   device: [long type: 22, int revision: 1] -> maps to Root node (0x0016) (utility device type)
   * on endpoint 1:
   *   device: [long type: 256, int revision: 1] -> maps to On/Off Light (0x0100)
   * ```
   */
  suspend fun readDescriptorClusterDeviceListAttribute(
      devicePtr: Long,
      endpoint: Int
  ): List<ChipStructs.DescriptorClusterDeviceTypeStruct> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
          .readDeviceTypeListAttribute(
              object : ChipClusters.DescriptorCluster.DeviceTypeListAttributeCallback {
                override fun onSuccess(
                    values: List<ChipStructs.DescriptorClusterDeviceTypeStruct>
                ) {
                  continuation.resume(values)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  /**
   * ServerListAttribute See
   * https://github.com/project-chip/connectedhomeip/blob/master/zzz_generated/app-common/app-common/zap-generated/ids/Clusters.h
   *
   * ```
   * For example: on endpoint 0
   *     sendReadServerListAttribute: [3]
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [29]
   *     ... and more ...
   * on endpoint 1:
   *     sendReadServerListAttribute: [3]
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [5]
   *     sendReadServerListAttribute: [6]
   *     sendReadServerListAttribute: [7]
   *     ... and more ...
   * on endpoint 2:
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [6]
   *     sendReadServerListAttribute: [29]
   *     sendReadServerListAttribute: [1030]
   *
   * Some mappings:
   *     namespace Groups = 0x00000004 (4)
   *     namespace OnOff = 0x00000006 (6)
   *     namespace TemperatureMeasurement = 0x00000402 (1026)
   *     namespace PressureMeasurement = 0x00000403 (1027)
   *     namespace HumidityMeasurement = 0x00000405 (1029)
   *     namespace Descriptor = 0x0000001D (29)
   *     namespace OccupancySensing = 0x00000406 (1030)
   * ```
   */
  suspend fun readDescriptorClusterServerListAttribute(devicePtr: Long, endpoint: Int): List<Long> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
          .readServerListAttribute(
              object : ChipClusters.DescriptorCluster.ServerListAttributeCallback {
                override fun onSuccess(values: MutableList<Long>) {
                  continuation.resume(values)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  /** ClientListAttribute */
  suspend fun readDescriptorClusterClientListAttribute(devicePtr: Long, endpoint: Int): List<Long> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
          .readClientListAttribute(
              object : ChipClusters.DescriptorCluster.ClientListAttributeCallback {
                override fun onSuccess(values: MutableList<Long>) {
                  continuation.resume(values)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  private fun getDescriptorClusterForDevice(
      devicePtr: Long,
      endpoint: Int
  ): ChipClusters.DescriptorCluster {
    return ChipClusters.DescriptorCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // ApplicationCluster functions

  suspend fun readApplicationBasicClusterAttributeList(deviceId: Long, endpoint: Int): List<Long> {
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return emptyList()
        }
    return suspendCoroutine { continuation ->
      getApplicationBasicClusterForDevice(connectedDevicePtr, endpoint)
          .readAttributeListAttribute(
              object : ChipClusters.ApplicationBasicCluster.AttributeListAttributeCallback {
                override fun onSuccess(value: MutableList<Long>) {
                  continuation.resume(value)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  private fun getApplicationBasicClusterForDevice(
      devicePtr: Long,
      endpoint: Int
  ): ChipClusters.ApplicationBasicCluster {
    return ChipClusters.ApplicationBasicCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // BasicCluster functions

  suspend fun readBasicClusterVendorIDAttribute(deviceId: Long, endpoint: Int): Int? {
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return null
        }
    return suspendCoroutine { continuation ->
      getBasicClusterForDevice(connectedDevicePtr, endpoint)
          .readVendorIDAttribute(
              object : ChipClusters.ApplicationBasicCluster.VendorIDAttributeCallback {
                override fun onSuccess(value: Int?) {
                  continuation.resume(value)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  suspend fun readBasicClusterAttributeList(deviceId: Long, endpoint: Int): List<Long> {
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return emptyList()
        }

    return suspendCoroutine { continuation ->
      getBasicClusterForDevice(connectedDevicePtr, endpoint)
          .readAttributeListAttribute(
              object : ChipClusters.ApplicationBasicCluster.AttributeListAttributeCallback {
                override fun onSuccess(values: MutableList<Long>) {
                  continuation.resume(values)
                }
                override fun onError(ex: Exception) {
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  private fun getBasicClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.ApplicationBasicCluster {
    return ChipClusters.ApplicationBasicCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // OnOffCluster functions

  // CODELAB FEATURED BEGIN
  suspend fun toggleDeviceStateOnOffCluster(deviceId: Long, endpoint: Int) {
    Timber.d("toggleDeviceStateOnOffCluster())")
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return
        }
    return suspendCoroutine { continuation ->
      getOnOffClusterForDevice(connectedDevicePtr, endpoint)
          .toggle(
              object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                  continuation.resume(Unit)
                }
                override fun onError(ex: Exception) {
                  Timber.e(ex, "readOnOffAttribute command failure")
                  continuation.resumeWithException(ex)
                }
              })
    }
  }
  // CODELAB FEATURED END

  suspend fun setOnOffDeviceStateOnOffCluster(deviceId: Long, isOn: Boolean, endpoint: Int) {
    Timber.d(
        "setOnOffDeviceStateOnOffCluster() [${deviceId}] isOn [${isOn}] endpoint [${endpoint}]")
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return
        }
    if (isOn) {
      // ON
      return suspendCoroutine { continuation ->
        getOnOffClusterForDevice(connectedDevicePtr, endpoint)
            .on(
                object : ChipClusters.DefaultClusterCallback {
                  override fun onSuccess() {
                    Timber.d("Success for setOnOffDeviceStateOnOffCluster")
                    continuation.resume(Unit)
                  }
                  override fun onError(ex: Exception) {
                    Timber.e(ex, "Failure for setOnOffDeviceStateOnOffCluster")
                    continuation.resumeWithException(ex)
                  }
                })
      }
    } else {
      // OFF
      return suspendCoroutine { continuation ->
        getOnOffClusterForDevice(connectedDevicePtr, endpoint)
            .off(
                object : ChipClusters.DefaultClusterCallback {
                  override fun onSuccess() {
                    Timber.d("Success for getOnOffDeviceStateOnOffCluster")
                    continuation.resume(Unit)
                  }
                  override fun onError(ex: Exception) {
                    Timber.e(ex, "Failure for getOnOffDeviceStateOnOffCluster")
                    continuation.resumeWithException(ex)
                  }
                })
      }
    }
  }

  suspend fun getDeviceStateOnOffCluster(deviceId: Long, endpoint: Int): Boolean? {
    Timber.d("getDeviceStateOnOffCluster())")
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e("Can't get connectedDevicePointer.")
          return null
        }
    return suspendCoroutine { continuation ->
      getOnOffClusterForDevice(connectedDevicePtr, endpoint)
          .readOnOffAttribute(
              object : ChipClusters.BooleanAttributeCallback {
                override fun onSuccess(value: Boolean) {
                  continuation.resume(value)
                }
                override fun onError(ex: Exception) {
                  Timber.e(ex, "readOnOffAttribute command failure")
                    ex.printStackTrace()
                  continuation.resumeWithException(ex)
                }
              })
    }
  }

  private fun getOnOffClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.OnOffCluster {
    return ChipClusters.OnOffCluster(devicePtr, endpoint)
  }

    suspend fun getDeviceStateTemperatureMeasurementCluster(deviceId: Long, endpoint: Int): Int? {
        Timber.d("getDeviceStateTemperatureMeasurementCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                Timber.e("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getTemperatureMeasurementClusterForDevice(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object : ChipClusters.TemperatureMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }
                        override fun onError(ex: Exception) {
                            Timber.e(ex, "getTemperatureMeasurementClusterForDevice command failure")
                            ex.printStackTrace()
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getTemperatureMeasurementClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.TemperatureMeasurementCluster {
        return ChipClusters.TemperatureMeasurementCluster(devicePtr, endpoint)
    }

    suspend fun getDeviceStatePressureMeasurementCluster(deviceId: Long, endpoint: Int): Int? {
        Timber.d("getDeviceStatePressureMeasurementCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                Timber.e("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getPressureMeasurementClusterForDevice(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object : ChipClusters.PressureMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }
                        override fun onError(ex: Exception) {
                            Timber.e(ex, "getPressureMeasurementClusterForDevice command failure")
                            ex.printStackTrace()
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getPressureMeasurementClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.PressureMeasurementCluster {
        return ChipClusters.PressureMeasurementCluster(devicePtr, endpoint)
    }

    suspend fun getDeviceStateHumidityMeasurementCluster(deviceId: Long, endpoint: Int): Int? {
        Timber.d("getDeviceStateHumidityMeasurementCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                Timber.e("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getHumidityMeasurementClusterForDevice(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object : ChipClusters.RelativeHumidityMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }
                        override fun onError(ex: Exception) {
                            Timber.e(ex, "getHumidityMeasurementClusterForDevice.readMeasuredValueAttribute command failure")
                            ex.printStackTrace()
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getHumidityMeasurementClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.RelativeHumidityMeasurementCluster {
        return ChipClusters.RelativeHumidityMeasurementCluster(devicePtr, endpoint)
    }

    suspend fun getDeviceStateHumidityMinMeasurementCluster(deviceId: Long, endpoint: Int): Int? {
        Timber.d("getDeviceStateHumidityMinMeasurementCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                Timber.e("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getHumidityMeasurementClusterForDevice(connectedDevicePtr, endpoint)
                .readMinMeasuredValueAttribute(
                    object : ChipClusters.RelativeHumidityMeasurementCluster.MinMeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }
                        override fun onError(ex: Exception) {
                            Timber.e(ex, "getHumidityMeasurementClusterForDevice.readMinMeasuredValueAttribute command failure")
                            ex.printStackTrace()
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    suspend fun getDeviceStateHumidityMaxMeasurementCluster(deviceId: Long, endpoint: Int): Int? {
        Timber.d("getDeviceStateHumidityMaxMeasurementCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                Timber.e("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getHumidityMeasurementClusterForDevice(connectedDevicePtr, endpoint)
                .readMaxMeasuredValueAttribute(
                    object : ChipClusters.RelativeHumidityMeasurementCluster.MaxMeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }
                        override fun onError(ex: Exception) {
                            Timber.e(ex, "getHumidityMeasurementClusterForDevice.readMaxMeasuredValueAttribute command failure")
                            ex.printStackTrace()
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

  // -----------------------------------------------------------------------------------------------
  // Administrator Commissioning Cluster (11.19)

  suspend fun openCommissioningWindowAdministratorCommissioningCluster(
      deviceId: Long,
      endpoint: Int,
      timeoutSeconds: Int,
      pakeVerifier: ByteArray,
      discriminator: Int,
      iterations: Long,
      salt: ByteArray,
      timedInvokeTimeoutMs: Int
  ) {
    Timber.d("openCommissioningWindowAdministratorCommissioningCluster())")
    val connectedDevicePtr =
        try {
          chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
          Timber.e(e, "Can't get connectedDevicePointer.")
          return
        }

    /*
    ChipClusters.DefaultClusterCallback var1, Integer var2, byte[] var3, Integer var4, Long var5, byte[] var6, int var7
     */
    return suspendCoroutine { continuation ->
      getAdministratorCommissioningClusterForDevice(connectedDevicePtr, endpoint)
          .openCommissioningWindow(
              object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                  continuation.resume(Unit)
                }
                override fun onError(ex: java.lang.Exception?) {
                  Timber.e(
                      ex,
                      "getAdministratorCommissioningClusterForDevice.openCommissioningWindow command failure")
                  continuation.resumeWithException(ex!!)
                }
              },
              timeoutSeconds,
              pakeVerifier,
              discriminator,
              iterations,
              salt,
              timedInvokeTimeoutMs)
    }
  }

  private fun getAdministratorCommissioningClusterForDevice(
      devicePtr: Long,
      endpoint: Int
  ): ChipClusters.AdministratorCommissioningCluster {
    return ChipClusters.AdministratorCommissioningCluster(devicePtr, endpoint)
  }
}
