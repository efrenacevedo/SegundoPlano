package mx.edu.segundoplano.util

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BiometricDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: BiometricData)

    @Query("SELECT * FROM biometric_data")
    suspend fun getAll(): List<BiometricData>

    @Query("DELETE FROM biometric_data")
    suspend fun deleteAll()

    @Query("SELECT * FROM biometric_data WHERE synced = 0")
    suspend fun getUnsynced(): List<BiometricData>

    @Query("UPDATE biometric_data SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)
}
