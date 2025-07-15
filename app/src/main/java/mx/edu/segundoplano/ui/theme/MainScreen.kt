package mx.edu.segundoplano.ui.theme

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

import java.util.*
import androidx.compose.material3.Text
import java.io.File
import java.text.SimpleDateFormat


@Composable
fun MainScreen() {
    val context = LocalContext.current
    val bleManager = remember { BLEClientManager(context) }
    val audioPlayer = remember { AudioPlayer(context) }

    var bpm by remember { mutableStateOf("-") }
    var compass by remember { mutableStateOf("-") }
    val audios = remember { mutableStateListOf<String>() }
    var connected by remember { mutableStateOf(false) }
    var serverName by remember { mutableStateOf("Desconocido") }

    // Carga inicial de audios
    LaunchedEffect(Unit) {
        loadAudioFilesDebug(context, audios)
    }

    // Actualización del nombre del servidor al conectar
    val onData: (String, String) -> Unit = { type, value ->
        when (type) {
            "bpm" -> bpm = value
            "compass" -> compass = value
            "audio_saved" -> {
                loadAudioFilesDebug(context, audios)
            }
            "connected" -> {
                connected = value.toBoolean()
                if (!connected) {
                    bpm = "-"
                    compass = "-"
                    serverName = "Desconocido"
                }
            }
            "device_name" -> {
                serverName = value
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
                    text = "Servidor: $serverName",
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Pulso", style = MaterialTheme.typography.bodyMedium)
                }
                // Brújula
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Face, // puedes cambiar a otro icono si quieres
                        contentDescription = "Brújula",
                        tint = Color.Blue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = compass,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Brújula", style = MaterialTheme.typography.bodyMedium)
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

