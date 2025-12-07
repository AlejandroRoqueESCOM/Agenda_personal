package com.example.proyecto_agenda

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ALARM_CHECK", "¡Alarma recibida! Intentando mostrar notificación...")

        try {
            val titulo = intent.getStringExtra("titulo") ?: "Evento Pendiente"
            val mensaje = intent.getStringExtra("mensaje") ?: "Revisa tu agenda."
            val idEvento = intent.getIntExtra("idEvento", System.currentTimeMillis().toInt())

            crearYMostrarNotificacion(context, titulo, mensaje, idEvento)
        } catch (e: Exception) {
            Log.e("ALARM_CHECK", "Error al mostrar notificación: ${e.message}")
        }
    }

    private fun crearYMostrarNotificacion(context: Context, titulo: String, mensaje: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "agenda_channel_id"

        val sonidoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Configuración CRÍTICA del canal para que suene
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Agenda",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH = Sonido + Pop-up
            ).apply {
                description = "Alertas de eventos importantes"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(sonidoUri, audioAttributes) // Forzar sonido en el canal
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intentApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, id, intentApp, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Para Android 7 o inferior
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría Alarma para que sea insistente
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(sonidoUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(id, builder.build())
        Log.d("ALARM_CHECK", "Notificación enviada al sistema.")
    }
}