package br.com.jadson.escalalouvor2k26

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import br.com.jadson.escalalouvor2k26.ui.screens.MainScreen
import br.com.jadson.escalalouvor2k26.ui.theme.EscalaLouvor2k26Theme

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled by system
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        setContent {
            val type = intent?.getStringExtra(br.com.jadson.escalalouvor2k26.util.NotificationHelper.EXTRA_TYPE)
            val refId = intent?.getStringExtra(br.com.jadson.escalalouvor2k26.util.NotificationHelper.EXTRA_REF_ID)
            
            EscalaLouvor2k26Theme {
                MainScreen(initialNotifType = type, initialRefId = refId)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
