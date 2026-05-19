package ar.gov.entrerios.cge.concursos.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ar.gov.entrerios.cge.concursos.MainActivity
import ar.gov.entrerios.cge.concursos.R
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.util.Constants
import ar.gov.entrerios.cge.concursos.core.util.DateFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(true)
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun notifyNewConcursos(concursos: List<Concurso>) {
        if (concursos.isEmpty()) return
        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        concursos.forEach { concurso ->
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("concursoscge://detalle/${concurso.id}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                concurso.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val keyword = concurso.matches.firstOrNull()?.keyword ?: "—"
            val date = DateFormatter.formatDate(concurso.publishedAt ?: concurso.detectedAt)

            val notif = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(concurso.title.take(80))
                .setContentText("Coincide con \"$keyword\" · $date")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Coincide con \"$keyword\"\nFecha: $date\n\n${concurso.excerpt}")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(Constants.NOTIFICATION_GROUP_KEY)
                .build()

            notificationManager.notify(concurso.id.toInt(), notif)
        }

        // Summary cuando hay más de uno
        if (concursos.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
            concursos.take(5).forEach { inboxStyle.addLine(it.title) }
            inboxStyle.setSummaryText("${concursos.size} resultados")

            val summary = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Concursos CGE")
                .setContentText("${concursos.size} nuevos concursos relevantes")
                .setStyle(inboxStyle)
                .setGroup(Constants.NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(0, summary)
        }
    }
}
