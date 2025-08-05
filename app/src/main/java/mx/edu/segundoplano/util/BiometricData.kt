package mx.edu.segundoplano.util

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "biometric_data")
@Serializable
data class BiometricData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerialName("heart_rate")
    val heartRate: Int,
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
    @SerialName("compass")
    val compass: String,
    @SerialName("device_id")
    val deviceID: String ="M2101K7BL",
    @SerialName("audio_path")
    val audioPath: String = "",
    @SerialName("synced_at")
    val synced: Long? = null
)