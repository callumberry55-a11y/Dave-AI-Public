package com.local.daveai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DaveSystemActionsManager(private val context: Context) {

    companion object {
        private const val TAG = "DaveSystemActionsManager"
        private const val CHANNEL_ID = "dave_system_channel"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dave System Notifications"
            val descriptionText = "Notifications pushed by Dave AI"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun pushNotification(title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notify(NOTIFICATION_ID, builder.build())
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push notification", e)
        }
    }

    fun getUpcomingCalendarEvents(maxEvents: Int = 5): List<String> {
        val result = mutableListOf<String>()
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_CALENDAR permission not granted")
                return emptyList()
            }

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf(System.currentTimeMillis().toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

                while (cursor.moveToNext() && result.size < maxEvents) {
                    val title = if (titleIdx != -1) cursor.getString(titleIdx) else "Event"
                    val start = if (startIdx != -1) cursor.getLong(startIdx) else 0L
                    val formattedTime = if (start > 0) dateFormat.format(Date(start)) else "Upcoming"
                    result.add("$title at $formattedTime")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query calendar events", e)
        }
        return result
    }

    fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm", e)
            false
        }
    }

    fun setTimer(durationSeconds: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer", e)
            false
        }
    }

    fun toggleFlashlight(enable: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            false
        }
    }

    fun setAudioRingerMode(mode: Int): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                audioManager.ringerMode = mode
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set audio ringer mode", e)
            false
        }
    }

    fun openAppByName(appName: String): Boolean {
        val trimmed = appName.trim().lowercase()
        if (trimmed.isBlank()) return false

        return try {
            val pm = context.packageManager

            // 1. Check common system action aliases
            when (trimmed) {
                "camera" -> {
                    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                }
                "settings" -> {
                    return openSettingsScreen("")
                }
                "clock", "alarm", "alarms" -> {
                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                }
            }

            // 2. Query all launchable activities across installed packages
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = pm.queryIntentActivities(mainIntent, 0)

            // Exact or substring match on app label or package name
            val matchedApp = launchableApps.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                val pkgName = resolveInfo.activityInfo.packageName.lowercase()
                label == trimmed || label.contains(trimmed) || pkgName.contains(trimmed)
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }

            // 3. Fallback: check all installed packages metadata
            val installedPkgs = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val fallbackPkg = installedPkgs.firstOrNull { pkg ->
                val label = pm.getApplicationLabel(pkg).toString().lowercase()
                label.contains(trimmed) || pkg.packageName.lowercase().contains(trimmed)
            }

            if (fallbackPkg != null) {
                val launchIntent = pm.getLaunchIntentForPackage(fallbackPkg.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app by name: $appName", e)
            false
        }
    }

    fun addCalendarEvent(title: String, description: String, startTimeMillis: Long, endTimeMillis: Long): Boolean {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "WRITE_CALENDAR permission not granted")
                return false
            }

            val values = android.content.ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, endTimeMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add calendar event", e)
            return false
        }
    }

    fun sendSms(phoneNumber: String, message: String): Boolean {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "SEND_SMS permission not granted")
                return false
            }

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            return false
        }
    }

    fun makePhoneCall(phoneNumber: String): Boolean {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "CALL_PHONE permission not granted")
                return false
            }

            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make phone call", e)
            return false
        }
    }

    fun openSettingsScreen(settingType: String = ""): Boolean {
        return try {
            val action = when (settingType.lowercase()) {
                "wifi" -> android.provider.Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
                "display" -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
                else -> android.provider.Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings screen", e)
            false
        }
    }
}
