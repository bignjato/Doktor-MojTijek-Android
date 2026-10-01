package com.mojtijek.doktor.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mojtijek.doktor.R
import java.text.SimpleDateFormat
import java.util.*

class DoseNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "dose_reminders"
        private const val CHANNEL_NAME = "Podsjetnici za doze"
        private const val CHANNEL_DESCRIPTION = "Obavijesti za uzimanje lijekova"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun showDoseNotification(
        notificationId: Int,
        therapyName: String,
        dose: String,
        time: String
    ) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Vrijeme za lijek: $therapyName")
            .setContentText("$dose u $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
    
    fun scheduleDoseNotifications(
        therapyId: String,
        therapyName: String,
        dose: String,
        times: List<String>
    ) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            android.util.Log.e("DoseNotifications", "AlarmManager not available")
            return
        }
        
        val today = Calendar.getInstance()
        times.forEach { timeString ->
            try {
                val timeParts = timeString.split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }
                    
                    // Only schedule if time is in the future today
                    if (calendar.after(Calendar.getInstance())) {
                        val notificationId = (therapyId + timeString).hashCode()
                        android.util.Log.d(
                            "DoseNotifications", 
                            "Would schedule notification for $therapyName at $timeString (ID: $notificationId)"
                        )
                        // In a real implementation, would set up AlarmManager here
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DoseNotifications", "Failed to parse time: $timeString", e)
            }
        }
    }
    
    fun cancelDoseNotifications(therapyId: String, times: List<String>) {
        times.forEach { timeString ->
            val notificationId = (therapyId + timeString).hashCode()
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
