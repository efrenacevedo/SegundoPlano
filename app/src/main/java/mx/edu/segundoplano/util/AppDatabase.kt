package mx.edu.segundoplano.util

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [BiometricData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun biometricDataDao(): BiometricDataDao
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase?= null
        fun getDatabase(context: Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = createDatabase(context)
                INSTANCE = instance
                instance
            }
        }
        fun createDatabase(context: Context): AppDatabase{
            val dbDir = File(context.getExternalFilesDir(null),"databases")
            if(!dbDir.exists()){
                dbDir.mkdirs()
            }
            val dbFile = File(dbDir,"biometric.db")
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                dbFile.absolutePath
            ).build()
        }
    }
}