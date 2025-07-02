package com.example.safepulsemainproject
import android.view.View

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager

class SetSafeZoneActivity : AppCompatActivity(), LocationListener {

    private var safeZoneLat: Double = 0.0
    private var safeZoneLng: Double = 0.0
    private val safeZoneRadius: Float = 3000f // 3 km radius
    private lateinit var locationManager: LocationManager
    private lateinit var startRideButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var trackingStarted = false
    private var startRideLat: Double = 0.0
    private var startRideLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_safe_zone)

        val latitudeInput = findViewById<EditText>(R.id.edit_text_latitude)
        val longitudeInput = findViewById<EditText>(R.id.edit_text_longitude)
        val btnSubmitSafeZone = findViewById<Button>(R.id.btn_submit_safe_zone)
        startRideButton = findViewById<Button>(R.id.btn_start_ride)

        startRideButton.visibility = View.GONE

        sharedPreferences = getSharedPreferences("SafePulsePrefs", MODE_PRIVATE)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        btnSubmitSafeZone.setOnClickListener {
            val latitude = latitudeInput.text.toString().toDoubleOrNull()
            val longitude = longitudeInput.text.toString().toDoubleOrNull()

            if (latitude != null && longitude != null) {
                safeZoneLat = latitude
                safeZoneLng = longitude

                val editor = sharedPreferences.edit()
                editor.putFloat("safeZoneLat", safeZoneLat.toFloat())
                editor.putFloat("safeZoneLng", safeZoneLng.toFloat())
                editor.putFloat("safeZoneRadius", safeZoneRadius)
                editor.apply()

                startRideButton.visibility = View.VISIBLE
                Toast.makeText(this, "Safe Zone Added Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter valid latitude and longitude.", Toast.LENGTH_SHORT).show()
            }
        }

        startRideButton.setOnClickListener {
            startLocationTracking()
        }
    }

    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1800000L, // 30 minutes interval
            10f,
            this,
            Looper.getMainLooper()
        )

        trackingStarted = true
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            startRideLat = it.latitude
            startRideLng = it.longitude
            sendJourneyStartAlert()
        }
        Toast.makeText(this, "Tracking started...  Wish You a Happy Safe Journey", Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
        val userLat = location.latitude
        val userLng = location.longitude

        val userLocation = Location("")
        userLocation.latitude = userLat
        userLocation.longitude = userLng

        val safeZoneLocation = Location("")
        safeZoneLocation.latitude = safeZoneLat
        safeZoneLocation.longitude = safeZoneLng

        val distance = userLocation.distanceTo(safeZoneLocation) / 1000 // Convert to km
        showNotification("Distance Update", "Distance to Safe Zone: $distance km")

        if (distance <= safeZoneRadius / 1000) {
            sendSafeZoneAlert()
            locationManager.removeUpdates(this)
        }
    }

    private fun sendJourneyStartAlert() {
        val message = "Journey has started. Please wait patiently for arrival at the safe zone. Live location: https://www.google.com/maps?q=$startRideLat,$startRideLng"
        sendSmsToEmergencyContacts(message)
    }

    private fun sendSafeZoneAlert() {
        val message = "User has entered the Safe Zone!"
        sendSmsToEmergencyContacts(message)
        showNotification("Safe Zone Alert", message)
    }

    private fun sendSmsToEmergencyContacts(message: String) {
        val contacts = sharedPreferences.getStringSet("emergency_contacts", emptySet()) ?: emptySet()
        val smsManager = SmsManager.getDefault()

        for (contact in contacts) {
            smsManager.sendTextMessage(contact, null, message, null, null)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "SafePulseChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SafePulse Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }
}
