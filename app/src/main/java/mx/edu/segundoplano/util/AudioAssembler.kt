package mx.edu.segundoplano.util


import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object AudioAssembler {
    private val buffer = ByteArrayOutputStream()

    fun appendChunk(chunk: ByteArray) {
        buffer.write(chunk)
    }
    fun finalizeAudio(context: Context): String {
        val audioDir = context.getExternalFilesDir("audios")?: context.filesDir
        if (!audioDir.exists()) audioDir.mkdirs()
        val fileName = "audio_${System.currentTimeMillis()}.3gp"
        val file = File(audioDir, fileName)
        file.writeBytes(buffer.toByteArray())
        buffer.reset()
        return file.absolutePath
//        try{
//            val dir = File(Environment.getExternalStorageDirectory(),"audios_recibidos")
//            if (!dir.exists()) {
//                val created = dir.mkdirs()
//                Log.d("AudioAssembler","Directorio Creado: $created")
//            }
//            val outputFile =  File(dir, fileName)
//            FileOutputStream(outputFile).use { output ->
//                for (chunk in buffer){
//                    output.write(chunk)
//                }
//
//            }
//            buffer.clear()
//            Log.d("AudioAssembler","Audio guardado en: ${outputFile.absolutePath}")
//            return outputFile.name
//        } catch (e: Exception){
//            Log.e("AudioAssembler","Error al guardar el audio: ${e.message}",e)
//            return "Error al guardar audio"
//        }
    }

//    fun finalizeAudio(): String {
//        val dir = File(Environment.getExternalStorageDirectory(), "audios_recibidos")
//        if (!dir.exists()) dir.mkdirs()
//
//        val file = File(dir, "audio_${System.currentTimeMillis()}.3gp")
//        val fos = FileOutputStream(file)
//        buffer.forEach { fos.write(it) }
//        fos.close()
//        buffer.clear()
//        return file.name
//    }
}


