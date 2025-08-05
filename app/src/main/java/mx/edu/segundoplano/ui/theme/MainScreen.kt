package mx.edu.segundoplano.ui.theme

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import mx.edu.segundoplano.util.AudioPlayer
import mx.edu.segundoplano.util.BLEClientManager
import android.util.Log
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn

import java.util.*
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import mx.edu.segundoplano.util.AppDatabase
import mx.edu.segundoplano.util.LocalDataRepository
import mx.edu.segundoplano.util.SensorDataViewModel
import mx.edu.segundoplano.util.SensorDataViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mx.edu.segundoplano.util.SupabaseService


@Composable
fun MainScreen() {
    val context = LocalContext.current

    val dbs = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        LocalDataRepository(
            dbs.biometricDataDao(),
            context = context
        )
    }
    val viewModel: SensorDataViewModel = viewModel(
        factory = SensorDataViewModelFactory(repository)
    )
    val bleClientManager = remember {
        BLEClientManager(context)
    }
    val bleManager = remember { BLEClientManager(context) }
    val audioPlayer = remember { AudioPlayer(context) }

    val scope = rememberCoroutineScope()
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    var bpm by remember { mutableStateOf("-") }
    var compass by remember { mutableStateOf("-") }
    var longitude by remember { mutableStateOf("-") }
    var latitude by remember { mutableStateOf("-") }
    val audios = remember { mutableStateListOf<String>() }
    var connected by remember { mutableStateOf(false) }
    var serverName by remember { mutableStateOf("Desconocido") }



    // Carga inicial de audios
    LaunchedEffect(Unit) {

        loadAudioFilesDebug(context, audios)
        bleClientManager.startScan { type, value ->
            viewModel.handleBLEData(type, value)
        }

    }

    // Actualización del nombre del servidor al conectar
    val onData: (String, String) -> Unit = { type, value ->
        when (type) {
            "bpm" -> bpm = value
            "compass" -> compass = value
            "location" ->{
                val parts = value.split(",")
                if (parts.size == 2){
                    latitude = parts[0]
                    longitude = parts[1]
                }
            }
            "audio_saved" -> {
                loadAudioFilesDebug(context, audios)
            }
            "connected" -> {
                connected = value.toBoolean()
                if (!connected) {
                    bpm = "-"
                    compass = "-"
                    latitude = "-"
                    longitude = "-"
                    serverName = "Desconocido"
                }
            }
            "device_name" -> {
                serverName = serverName
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón conexión
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { bleManager.startScan(onData) }
        ) {
            Text(text = if (connected) "Reconectar" else "Escanear y Conectar")
        }

        // Nombre del servidor + estado conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (connected) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (connected) Color.Green else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Servidor: ${serverName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (connected) "Conectado" else "Desconectado",
                    color = if (connected) Color.Green else Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Datos principales (Pulso y Brújula)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulso
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = "Pulso",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$bpm bpm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Pulso", style = MaterialTheme.typography.bodyMedium)
                }
                // Brújula
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Info, // puedes cambiar a otro icono si quieres
                        contentDescription = "Brújula",
                        tint = Color.Blue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = compass,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Brújula", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn, // puedes cambiar a otro icono si quieres
                        contentDescription = "Localización",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = longitude,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = latitude,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Ubicación", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Lista audios recibidos
        Text(
            text = "Audios recibidos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (audios.isEmpty()) {
            Text("No se han recibido audios aún.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = {
                    audios.groupBy { classifyAudioDate(File(it)) }.forEach { (group, items) ->
                        item {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(items) { path ->
                            val file = File(path)
                            val duration = getAudioDuration(context, file)
                            val date =
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    .format(Date(file.lastModified()))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { audioPlayer.play(path) },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("Fecha: $date", style = MaterialTheme.typography.bodySmall)
                                        Text("Duración: $duration", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { audioPlayer.play(path) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir")
                                        }
                                        IconButton(onClick = {
                                            if (file.delete()) {
                                                audios.remove(path)
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        Button(
            onClick = {
                isSyncing = true
                scope.launch {
                    val supabase = SupabaseService(context)
                    val result = supabase.syncWithSupabase()

                    syncMessage = if (result.success) {
                        "Sincronizado ${result.recordsSynced} registros"
                    } else {
                        "Error: ${result.message}"
                    }
                    isSyncing = false
                }
            },
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sincronizando...")
            } else {
                Text("Sincronizar con Supabase")
            }
        }

        syncMessage?.let {
            Text(it, color = if (it.startsWith("Error")) Color.Red else Color.Green)
        }

    }

}


fun loadAudioFilesDebug(context: Context, audios: MutableList<String>) {
    val audiosDir = File(context.getExternalFilesDir(null), "audios")
    if (audiosDir.exists()) {
        val files = audiosDir.listFiles()
        val filtered = files?.filter { it.extension.equals("3gp", ignoreCase = true) } ?: emptyList()
        audios.clear()
        audios.addAll(filtered.map { it.absolutePath })
    } else {
        audios.clear()
    }
}

fun classifyAudioDate(file: File): String {
    val lastModified = file.lastModified()
    val today = Calendar.getInstance()
    val fileDate = Calendar.getInstance().apply { timeInMillis = lastModified }
    return when {
        today.get(Calendar.YEAR) == fileDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == fileDate.get(Calendar.DAY_OF_YEAR) -> "Subidos hoy"
        today.get(Calendar.YEAR) == fileDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) - fileDate.get(Calendar.DAY_OF_YEAR) == 1 -> "Subidos ayer"
        today.get(Calendar.WEEK_OF_YEAR) == fileDate.get(Calendar.WEEK_OF_YEAR) -> "Esta semana"
        else -> "Audios antiguos"
    }
}

fun getAudioDuration(context: Context, file: File): String {
    return try {
        val retriever = MediaMetadataRetriever().apply {
            setDataSource(context, Uri.fromFile(file))
        }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        retriever.release()
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        String.format("%02d:%02d", minutes, seconds)
    } catch (e: Exception) {
        "--:--"
    }
}

