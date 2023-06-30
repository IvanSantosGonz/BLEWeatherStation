package com.example.weatherstation

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothService(context: Context) {
    private val context: Context
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
    private var advertisementCallback: AdvertisingSetCallback
    private lateinit var gattServer: BluetoothGattServer
    private lateinit var gattServerCallback: GattServerCallback

    private val LOG_TAG = "WEATHER STATION BLUETOOTH SERVICE"
    private val WEATHER_SERVICE_UUID = "2a3477de-19a7-4004-918b-a69261f0fdc7"
    private val TEMPERATURE_CHARACTERISTIC_UUID = "718e1d1a-8249-4e89-a3a2-eccf213607cb"
    private val HUMIDITY_CHARACTERISTIC_UUID = "b7d3bc72-de1a-4d82-8b7c-7f249b923358"

    private var temperatureValue = "0"
    private var humidityValue = "0"
    var isAdvertising = false

    init {
        this.bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.bluetoothAdapter = bluetoothManager.adapter
        this.bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        this.context = context
        this.advertisementCallback = createAdvertisementCallback()
    }

    private fun createAdvertisementParameters(): AdvertisingSetParameters {
        return AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setScannable(true)
            .setConnectable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()
    }

    private fun createAdvertisementData(): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid.fromString(WEATHER_SERVICE_UUID))
            //.addServiceData(ParcelUuid.fromString(WEATHER_SERVICE_UUID), "some data".toByteArray())
            .build()
    }

    private fun createAdvertisementCallback(): AdvertisingSetCallback {
        return object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet,
                txPower: Int,
                status: Int
            ) {
                Log.i(
                    LOG_TAG,
                    "onAdvertisingSetStarted(): txPower:$txPower , status: $status"
                )
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
                Log.i(LOG_TAG, "onAdvertisingDataSet() :status:$status")
            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
                Log.i(LOG_TAG, "onScanResponseDataSet(): status:$status")
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                Log.i(LOG_TAG, "onAdvertisingSetStopped():")
            }
        }
    }

    private fun createGattService(): BluetoothGattService {
        val service = BluetoothGattService(
            ParcelUuid.fromString(WEATHER_SERVICE_UUID).uuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val temperatureCharacteristic = BluetoothGattCharacteristic(
            ParcelUuid.fromString(TEMPERATURE_CHARACTERISTIC_UUID).uuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(temperatureCharacteristic)

        val humidityCharacteristic = BluetoothGattCharacteristic(
            ParcelUuid.fromString(HUMIDITY_CHARACTERISTIC_UUID).uuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(humidityCharacteristic)

        return service
    }

    private fun setupGattServer(context: Context) {
        gattServerCallback = GattServerCallback()
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        val service = createGattService()
        val serviceExists = gattServer.getService(service.uuid) != null
        if (!serviceExists) {
            gattServer.addService(service)
        }
    }

    fun startAdvertising() {
        setupGattServer(context)
        val advertisementParameters = createAdvertisementParameters()
        val advertisementData = createAdvertisementData()
        bluetoothLeAdvertiser.startAdvertisingSet(
            advertisementParameters,
            advertisementData,
            null,
            null,
            null,
            0,
            0,
            advertisementCallback
        )
        isAdvertising = true
        Log.i(LOG_TAG, "startAdvertising()")
    }


    fun stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertisingSet(advertisementCallback)
    }

    fun setOnTemperatureCharacteristicReadResponse(value: String) {
        temperatureValue = value
    }

    fun setOnHumidityCharacteristicReadResponse(value: String) {
        humidityValue = value
    }

    inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            if (characteristic!!.uuid.toString() == TEMPERATURE_CHARACTERISTIC_UUID) {
                Log.d(LOG_TAG, "Reading temperature characteristic")
                gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    temperatureValue.toByteArray()
                )
            }
            else if (characteristic.uuid.toString() == HUMIDITY_CHARACTERISTIC_UUID) {
                Log.d(LOG_TAG, "Reading humidity characteristic")
                gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    humidityValue.toByteArray()
                )
            }
        }
    }
}