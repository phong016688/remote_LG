package com.mentos_koder.remote_lg_tv.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.adapter.AppAdapter
import com.mentos_koder.remote_lg_tv.util.Constants


object NotificationScheduler {
    const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "MyChannel"
    private val CHANNEL_NAME: CharSequence = "My Channel"
    fun scheduleNotification(context: Context, triggerAtMillis: Long) {
        val notificationIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelNotification(context: Context) {
        val notificationIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            showNotification(context)
        }

        private fun showNotification(context: Context) {
            createNotificationChannel(context)
            val sharedPref = context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
            var url = sharedPref.getString("nameDevice", "")
            if (url!!.isEmpty()) {
                url = "TVs"
            }
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo_app)
                    .setContentTitle("Remote")
                    .setContentText("Control$url are simple and easy to use,Open Channel now!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        private fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

