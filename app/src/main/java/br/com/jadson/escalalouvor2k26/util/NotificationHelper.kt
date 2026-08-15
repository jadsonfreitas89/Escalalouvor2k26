package br.com.jadson.escalalouvor2k26.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.jadson.escalalouvor2k26.MainActivity
import br.com.jadson.escalalouvor2k26.R

object NotificationHelper {
    private const val CHANNEL_ID = "escala_louvor_notifications"
    private const val CHANNEL_NAME = "Notificações da Escala"
    private const val CHANNEL_DESC = "Informa sobre escalas, louvores e solicitações"

    const val TYPE_RECADO = "RECADO"
    const val TYPE_ESCALA_ALTERADA = "ESCALA_ALTERADA"
    const val TYPE_SOLICITACAO_TROCA = "SOLICITACAO_TROCA"
    const val TYPE_TROCA_RECUSADA = "TROCA_RECUSADA"
    const val TYPE_LOUVORES = "LOUVORES"
    const val TYPE_BOAS_VINDAS = "BOAS_VINDAS"

    const val EXTRA_TYPE = "notif_type"
    const val EXTRA_REF_ID = "notif_ref_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context, 
        id: Int, 
        title: String, 
        message: String,
        type: String = TYPE_BOAS_VINDAS,
        referenceId: String? = null
    ): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_REF_ID, referenceId)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification) // App monochromatic icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            with(NotificationManagerCompat.from(context)) {
                Log.d("NOTIF_HELPER", "Exibindo notificação ID: $id, Tipo: $type")
                notify(id, builder.build())
                true
            }
        } catch (e: SecurityException) {
            Log.e("NOTIF_HELPER", "ERRO SECURITY = ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("NOTIF_HELPER", "ERRO NOTIFY = ${e.message}")
            false
        }
    }
}
