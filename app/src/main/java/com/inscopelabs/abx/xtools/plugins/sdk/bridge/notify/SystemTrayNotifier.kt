package com.inscopelabs.abx.xtools.plugins.sdk.bridge.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Default [Notifier] — posts to the system tray. Requires the
 * `POST_NOTIFICATIONS` runtime permission on Android 13+; the call is a
 * silent no-op when the permission is missing rather than throwing,
 * because a denied notification is not a plugin-fatal condition.
 */
class SystemTrayNotifier(private val context: Context) : Notifier {

    init {
        ensureChannel()
    }

    override fun show(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NEXT_ID++, n)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "XTools Plugins",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    companion object {
        private const val CHANNEL_ID = "xtools.plugins"
        private var NEXT_ID: Int = 1
    }
}
