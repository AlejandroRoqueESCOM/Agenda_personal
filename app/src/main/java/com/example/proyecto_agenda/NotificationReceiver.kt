package com.example.proyecto_agenda

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Recuperar datos del intent
        val titulo = intent.getStringExtra("titulo") ?: "Evento Pendiente"
        val mensaje = intent.getStringExtra("mensaje") ?: "Tienes una tarea programada."
        val idEvento = intent.getIntExtra("idEvento", 0)

        // Verificar si es un evento atrasado o a tiempo (opcional, para cambiar el texto)
        val timestampProgramado = intent.getLongExtra("timestamp", 0L)
        val diferencia = System.currentTimeMillis() - timestampProgramado

        // Si la diferencia es mayor a 5 minutos (300,000 ms), podríamos agregar "(Atrasado)" al título
        val tituloFinal = if (diferencia > 300000) "$titulo (Atrasado)" else titulo

        crearYMostrarNotificacion(context, tituloFinal, mensaje, idEvento)
    }

    private fun crearYMostrarNotificacion(context: Context, titulo: String, mensaje: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "agenda_alta_prioridad"

        // 1. Configuración del CANAL (Fundamental para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Agenda",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH hace que sea EMERGENTE (Heads-up)
            ).apply {
                description = "Notificaciones para eventos y recordatorios"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500) // Patrón de vibración
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC // Visible en BLOQUEO
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Intent para abrir la aplicación al tocar la notificación
        val intentApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("idEventoAbierto", id) // Por si quieres abrir el evento directo
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id, // ID único para que no se sobrescriban los intents
            intentApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Sonido de notificación predeterminado
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 4. Construcción de la Notificación
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar) // ÍCONO OBLIGATORIO
            .setContentTitle(titulo)
            .setContentText(mensaje) // Texto corto
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje)) // NOTIFICACIÓN EXPANDIBLE (Texto largo)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Para versiones anteriores a Android 8 (Emergente)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent) // Acción al hacer click
            .setAutoCancel(true) // Se borra al tocarla
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible en pantalla de bloqueo

        // Mostrar
        notificationManager.notify(id, builder.build())
    }
}