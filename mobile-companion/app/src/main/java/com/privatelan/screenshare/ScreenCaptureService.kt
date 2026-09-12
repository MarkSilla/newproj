package com.privatelan.screenshare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        createChannel()

        startForeground(
            20,
            notification()
        )

        val resultCode =
            intent?.getIntExtra(
                "resultCode",
                0
            ) ?: 0

        val data =
            intent?.getParcelableExtra<Intent>(
                "data"
            )

        if (resultCode != 0 && data != null) {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as android.media.projection.MediaProjectionManager

            projection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

            projection?.registerCallback(
                object : MediaProjection.Callback() {},
                null
            )
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {

        projection?.stop()
        projection = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                NotificationChannel(
                    "screen_share",
                    "Screen sharing",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notification(): Notification {

        return NotificationCompat.Builder(
            this,
            "screen_share"
        )
            .setContentTitle(
                "Private LAN Screen Share"
            )
            .setContentText(
                "Your screen is being shared on the local network"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .setOngoing(true)
            .build()
    }
}
