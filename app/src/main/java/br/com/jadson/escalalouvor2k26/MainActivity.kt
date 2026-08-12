package br.com.jadson.escalalouvor2k26

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.jadson.escalalouvor2k26.ui.screens.MainScreen
import br.com.jadson.escalalouvor2k26.ui.theme.EscalaLouvor2k26Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EscalaLouvor2k26Theme {
                MainScreen()
            }
        }
    }
}
