package com.example.safepulsemainproject

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        val sharedPreferences = context.getSharedPreferences("SafePulsePrefs", Context.MODE_PRIVATE)
        val transitionType = geofencingEvent.geofenceTransition

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                sharedPreferences.edit().putLong("last_enter_time", System.currentTimeMillis()).apply()
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                val predefinedTime = sharedPreferences.getLong("safe_zone_time", 0L)

                Handler(Looper.getMainLooper()).postDelayed({
                    val lastEnterTime = sharedPreferences.getLong("last_enter_time", 0L)
                    if (System.currentTimeMillis() - lastEnterTime > predefinedTime) {
                        sendEmergencyAlert(context)
                    }
                }, predefinedTime)
            }
        }
    }

    private fun sendEmergencyAlert(context: Context) {
        val sharedPreferences = context.getSharedPreferences("SafePulsePrefs", Context.MODE_PRIVATE)
        val emergencyContacts = sharedPreferences.getStringSet("emergency_contacts", emptySet())?.toList() ?: emptyList()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val message = "ALERT! The user has not returned to the Safe Zone in time."
        val smsManager = SmsManager.getDefault()
        emergencyContacts.forEach { smsManager.sendTextMessage(it, null, message, null, null) }
    }
}
