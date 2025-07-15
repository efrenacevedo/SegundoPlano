package mx.edu.segundoplano.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import mx.edu.segundoplano.util.AudioAssembler
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.thread

class BLEClientManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000aaaa-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_UUID: UUID = UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb")
        // val STRESS_UUID: UUID = UUID.fromString("0000aa02-0000-1000-8000-00805f9b34fb") // eliminado
        val AUDIO_UUID: UUID = UUID.fromString("0000aa03-0000-1000-8000-00805f9b34fb")
        val AUDIO_END_UUID: UUID = UUID.fromString("0000aa04-0000-1000-8000-00805f9b34fb")
        val COMPASS_UUID: UUID = UUID.fromString("0000aa05-0000-1000-8000-00805f9b34fb")
        val LOCATION_UUID: UUID = UUID.fromString("0000aa06-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun startScan(onData: (String, String) -> Unit) {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                scanner.stopScan(this)
                Log.d("BLEClient", "🔍 Dispositivo encontrado: ${device.name ?: device.address}")
                device.connectGatt(context, false, gattCallback(onData), BluetoothDevice.TRANSPORT_LE, 2000)
            }
        })
    }

    private fun gattCallback(onData: (String, String) -> Unit) = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLEClient", "✅ Conectado al servidor BLE, descubriendo servicios...")
                this@BLEClientManager.gatt = gatt
                gatt?.requestMtu(512)

                onData("connected", "true")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLEClient", "❌ Desconectado del servidor BLE")
                onData("connected", "false")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int){
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("BLEClient", "MTU cambiado exitosamente: $mtu bytes")
            }else{
                Log.w("BLEClient","Error al cambiar el valor del mtu, mantiene su valor por defecto: $mtu bytes")
            }
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e("BLEClient", "❌ Servicio no encontrado")
                return
            }
            Log.d("BLEClient","Servicios encontrados:")
            val characteristics = listOf(
                HEART_RATE_UUID,
                AUDIO_UUID,
                AUDIO_END_UUID,
                COMPASS_UUID,
                LOCATION_UUID
            )
            gatt.services.forEach{ service ->
                Log.d("BLEClient","-- UUID: ${service.uuid}")
            }
            thread {
                for (uuid in characteristics) {
                    val characteristic = service.getCharacteristic(uuid)
                    if (characteristic != null) {
                        val success = gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(CCCD_UUID)
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val result = gatt.writeDescriptor(descriptor)
                        Log.d("BLEClient", "🔔 Subscripción a $uuid → noti=$success, write=$result")
                        Thread.sleep(300) // pausa para no saturar el stack BLE
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                HEART_RATE_UUID -> {
                    val bpm = characteristic.value[0].toInt() and 0xFF
                    Log.d("BLEClient", "❤️ BPM recibido: $bpm")
                    onData("bpm", bpm.toString())
                }
                AUDIO_UUID -> {
                    val size = characteristic.value.size
                    Log.d("BLEClient", "🎧 Fragmento de audio recibido: $size bytes")
                    AudioAssembler.appendChunk(characteristic.value)
                }
                AUDIO_END_UUID -> {
                    val fileName = AudioAssembler.finalizeAudio(context)
                    Log.d("BLEClient", "📁 Audio reconstruido y guardado como: $fileName")
                    onData("audio_saved", fileName)
                }
                COMPASS_UUID -> {
                    val compassString = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("BLEClient", "🧭 Brújula recibida: $compassString")
                    onData("compass", compassString)
                }
                LOCATION_UUID -> {
                    val data = characteristic.value
                    if (data.size >= 16) {
                        val lat = ByteBuffer.wrap(data, 0, 8).double
                        val lon = ByteBuffer.wrap(data, 8, 8).double
                        Log.d("BLEClient", "📍 Ubicación recibida: lat=$lat, lon=$lon")
                        onData("location", "$lat,$lon")
                    } else {
                        Log.d("BLEClient", "📍 Datos de ubicación incompletos")
                    }
                }
            }
        }
    }
}
