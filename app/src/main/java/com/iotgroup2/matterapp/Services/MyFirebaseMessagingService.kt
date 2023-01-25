package com.iotgroup2.matterapp.Services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.iotgroup2.matterapp.MainActivity
import com.iotgroup2.matterapp.R
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("New refreshed token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Timber.i("From: ${message.from}")

        // Check if message contains a data payload.
        message.data.isNotEmpty().let {
            Timber.i("Message data payload: " + message.data)

            // get title and body from message.data
            val msg = message.data["default"]
            Timber.i("msg: $msg")

            if (msg != null) {
                sendNotification(msg)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var NOTIFY_ID = 0
    @SuppressLint("MissingPermission", "UnspecifiedImmutableFlag")
    private fun sendNotification(messageBody: String) {
        // init notification manager
        if (!::notificationManager.isInitialized) {
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }

        // create a channel
        var channel = notificationManager.getNotificationChannel("channelId")
        if (channel == null) {
            channel = NotificationChannel("channelId", "Alert", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // set notification tap to open app
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // make a push notification
        val builder = NotificationCompat.Builder(this, "channelId")
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alert")
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()


        // show the notification
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFY_ID, builder)
            NOTIFY_ID++
        }
    }
}