package com.example.weatherstation

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startAdvertisingIfPermissions()
    }

    private fun startAdvertisingIfPermissions() {
        PermissionX.init(this)
            .permissions(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    bluetoothService = BluetoothService(applicationContext)
                    bluetoothService.startAdvertising()
                    Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun updateValues(view: View?){
        val temperature = findViewById<TextInputEditText>(R.id.textInputEditTextTemperature).text.toString()
        bluetoothService.setOnTemperatureCharacteristicReadResponse("$temperature ºC")
        val humidity = findViewById<TextInputEditText>(R.id.textInputEditTextHumidity).text.toString()
        bluetoothService.setOnHumidityCharacteristicReadResponse("$humidity %")
        Toast.makeText(this, "Temperature and humidity values updated", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        if (bluetoothService.isAdvertising) {
            bluetoothService.stopAdvertising()
        }
        super.onDestroy()
    }
}