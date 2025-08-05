package mx.edu.segundoplano.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Room
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SyncResult(
    val success: Boolean,
    val recordsSynced: Int = 0,
    val message: String? = null
)

class SupabaseService(val context: Context) {
    private val bucket = "audios"
    private val client: SupabaseClient = createSupabaseClient(
      supabaseUrl = "https://gyitfkcnhwslnbhfkomx.supabase.co",
      supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5aXRma2NuaHdzbG5iaGZrb214Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4MjY1NTcsImV4cCI6MjA2OTQwMjU1N30.cPHyYrurNhS78BPTvGyLrsCcfSdelImTip6Xqk-WmEQ"

    ) {
        install(Postgrest)
        install(Storage)

        install(GoTrue)
    }
    @OptIn(SupabaseExperimental::class)
    suspend fun syncWithSupabase(): SyncResult {
        return try {
            // Archivo interno real donde Room guarda la base de datos
            val internalDbFile = context.getDatabasePath("biometric.db")

            // Ruta externa donde guardaremos la copia para subir
            val externalDbDir = File(context.getExternalFilesDir(null), "databases")
            if (!externalDbDir.exists()) {
                externalDbDir.mkdirs()
                Log.d("SupabaseService", "Carpeta externa creada: ${externalDbDir.absolutePath}")
            }
            val externalDbFile = File(externalDbDir, "biometric.db")

            Log.d("SupabaseService", "Archivo DB interno: ${internalDbFile.absolutePath} existe=${internalDbFile.exists()} tamaño=${internalDbFile.length()} bytes")

            if (internalDbFile.exists()) {
                // Copiar la base de datos interna a externa
                internalDbFile.copyTo(externalDbFile, overwrite = true)
                Log.d("SupabaseService", "Archivo DB copiado a externo: ${externalDbFile.absolutePath}")
            } else {
                Log.w("SupabaseService", "Archivo DB interno NO existe, no se puede copiar")
            }

            var filesSynced = 0

            // Subir la copia externa de la base de datos
            if (externalDbFile.exists()) {
                val dbUri = Uri.fromFile(externalDbFile)
                client.storage.from(bucket).upload("databases/biometric.db", dbUri, upsert = true)
                Log.d("SupabaseService", "Archivo DB subido correctamente")
                filesSynced++
            } else {
                Log.w("SupabaseService", "Archivo DB externo no existe, no se subió")
            }

            // Subir audios
            val audiosDir = File(context.getExternalFilesDir(null), "audios")
            if (audiosDir.exists() && audiosDir.isDirectory) {
                val audioFiles = audiosDir.listFiles { file -> file.extension == "3gp" } ?: emptyArray()

                Log.d("SupabaseService", "Archivos de audio encontrados: ${audioFiles.size}")

                for (audio in audioFiles) {
                    val audioUri = Uri.fromFile(audio)
                    client.storage.from(bucket).upload("audios/${audio.name}", audioUri, upsert = true)
                    Log.d("SupabaseService", "Audio subido: ${audio.name}")
                    filesSynced++
                }
            } else {
                Log.w("SupabaseService", "Carpeta de audios no existe")
            }

            SyncResult(success = true, recordsSynced = filesSynced)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error al sincronizar", e)
            SyncResult(success = false, message = e.localizedMessage ?: "Error desconocido")
        }
    }

    suspend fun uploadAudio(file: File): String = withContext(Dispatchers.IO) {
        val fileName = "audios/${System.currentTimeMillis()}_${file.name}"
        client.storage.from(bucket).upload(
            path = fileName,
            file = file
        )
        Log.d("SupabaseService", "Audio subido en uploadAudio: $fileName")
        return@withContext fileName
    }

    suspend fun syncBiometricData(data: List<BiometricData>) = withContext(Dispatchers.IO) {
        val records = data.map {
            BiometricData(
                heartRate = it.heartRate,
                latitude = it.latitude,
                longitude = it.longitude,
                compass = it.compass,
                audioPath = it.audioPath,
                deviceID = it.deviceID,
                synced = System.currentTimeMillis()
            )
        }
        Log.d("SupabaseService", "Subiendo datos a supabase: ${records.size} registros")
        client.postgrest["biometric_data"].insert(records)
    }

    suspend fun syncDatabase(dbFile: File, audiosDir: File): SyncResult {
        return try {
            Log.d("SupabaseService", "Iniciando sincronización de base de datos")

            val localDb = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "biometric.db"  // Sólo nombre, no ruta completa
            ).build()

            val unsyncedData = localDb.biometricDataDao().getUnsynced()
            Log.d("SupabaseService", "Registros no sincronizados: ${unsyncedData.size}")

            val updatedData = unsyncedData.mapNotNull { record ->
                if (record.audioPath.isNotEmpty()) {
                    val audioFile = File(audiosDir, record.audioPath)
                    if (audioFile.exists()) {
                        val remotePath = uploadAudio(audioFile)
                        Log.d("SupabaseService", "Audio sincronizado para record ${record.id}")
                        record.copy(audioPath = remotePath)
                    } else {
                        Log.w("SupabaseService", "Audio no existe localmente para record ${record.id}")
                        record
                    }
                } else {
                    record
                }
            }

            syncBiometricData(updatedData)

            localDb.biometricDataDao().markAsSynced(updatedData.map { it.id })
            Log.d("SupabaseService", "Registros marcados como sincronizados")

            SyncResult(true, updatedData.size, "Sincronización exitosa")
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error en syncDatabase", e)
            SyncResult(false, 0, "Error: ${e.message}")
        }
    }

    //data class SyncResult(val success: Boolean, val recordsSynced: Int, val message: String)

}