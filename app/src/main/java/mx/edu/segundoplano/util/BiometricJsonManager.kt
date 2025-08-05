package mx.edu.segundoplano.util


import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object BiometricJsonManager {

    private const val FILE_NAME = "biometric_data_pending.json"

    fun appendData(context: Context, data: BiometricData) {
        val file = File(context.filesDir, FILE_NAME)

        val currentList = if (file.exists()) {
            val content = file.readText()
            if (content.isNotBlank()) {
                Json.decodeFromString<List<BiometricData>>(content)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        val updatedList = currentList + data
        file.writeText(Json.encodeToString(updatedList))
    }

    fun readAll(context: Context): List<BiometricData> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists() || file.readText().isBlank()) return emptyList()
        return Json.decodeFromString(file.readText())
    }

    fun clear(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.writeText("")
    }
}