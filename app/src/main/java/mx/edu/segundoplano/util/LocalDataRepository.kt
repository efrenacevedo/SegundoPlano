package mx.edu.segundoplano.util

import android.content.Context

class LocalDataRepository(private val dao: BiometricDataDao, private val context: Context) {
    suspend fun insertBiometricData(data: BiometricData){
        dao.insert(data)
        BiometricJsonManager.appendData(context, data)
    }
    suspend fun getAllBiometricData(): List<BiometricData>{
        return dao.getAll()
    }


}