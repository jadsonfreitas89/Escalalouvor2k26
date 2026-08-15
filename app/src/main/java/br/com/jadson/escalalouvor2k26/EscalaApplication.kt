package br.com.jadson.escalalouvor2k26

import android.app.Application
import android.util.Log
import androidx.work.*
import br.com.jadson.escalalouvor2k26.util.NotificationHelper
import br.com.jadson.escalalouvor2k26.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class EscalaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NOTIF_BACKGROUND_TEST", "EscalaApplication: onCreate() - PROCESSO INICIADO")
        NotificationHelper.createNotificationChannel(this)
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EscalaNotificationWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            repeatingRequest
        )
    }
}
