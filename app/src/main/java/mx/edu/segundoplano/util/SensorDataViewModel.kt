package mx.edu.segundoplano.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class AggregatedData(
    var heartRate: Int? = null,
    var compass: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null
)

class SensorDataViewModel(
    private val repository: LocalDataRepository
) : ViewModel() {

    private val latestData = AggregatedData()

    fun handleBLEData(type: String, value: String) {
        when (type) {
            "bpm" -> latestData.heartRate = value.toIntOrNull()
            "compass" -> latestData.compass = value
            "location" -> {
                val parts = value.split(",")
                if (parts.size == 2) {
                    latestData.latitude = parts[0].toDoubleOrNull()
                    latestData.longitude = parts[1].toDoubleOrNull()
                }
            }
        }
    }

    init {
        startPeriodicSave()
    }

    private fun startPeriodicSave() {
        viewModelScope.launch {
            while (true) {
               // delay(30_000L) // cada 30 segundos
                delay(5_000L)
                val data = latestData
                if (data.heartRate != null && data.latitude != null && data.longitude != null) {
                    val biometric = BiometricData(
                        heartRate = data.heartRate!!,
                        compass = data.compass ?: "",
                        latitude = data.latitude!!,
                        longitude = data.longitude!!
                    )
                    repository.insertBiometricData(biometric)
                    Log.d("SensorDataVM", "📦 Datos guardados localmente: $biometric")

                } else {
                    Log.d("SensorDataVM", "⚠️ Datos incompletos, no se guardó")
                }
            }
        }
    }
    fun syncJsonDataToSupabase(supabaseService: SupabaseService) {
        viewModelScope.launch {
            val unsyncedJsonData = BiometricJsonManager.readAll(supabaseService.context)

            if (unsyncedJsonData.isNotEmpty()) {
                try {
                    supabaseService.syncBiometricData(unsyncedJsonData)
                    BiometricJsonManager.clear(supabaseService.context)
                    Log.d("SYNC", "✅ Datos sincronizados a Supabase desde JSON (${unsyncedJsonData.size})")
                } catch (e: Exception) {
                    Log.e("SYNC", "❌ Error al sincronizar: ${e.message}")
                }
            } else {
                Log.d("SYNC", "📭 No hay datos para sincronizar")
            }
        }
    }

}

