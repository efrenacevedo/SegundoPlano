package mx.edu.segundoplano.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val supabase = SupabaseService(applicationContext)
            val dbFile = File(applicationContext.getExternalFilesDir("databases"), "biometric.db")
            val audiosDir = File(applicationContext.getExternalFilesDir(null), "audios")

            val result = supabase.syncDatabase(dbFile, audiosDir)

            if (result.success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                5, TimeUnit.MINUTES // Intervalo de 5 minutos
            ).setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync_biometric_data",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}