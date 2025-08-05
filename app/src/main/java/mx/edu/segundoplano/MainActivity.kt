
package mx.edu.segundoplano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import mx.edu.segundoplano.ui.theme.MainScreen
import mx.edu.segundoplano.ui.theme.SegundoPlanoTheme
import mx.edu.segundoplano.util.SyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)
        setContent {
            SegundoPlanoTheme {
                MainScreen()
            }
        }
    }
}


