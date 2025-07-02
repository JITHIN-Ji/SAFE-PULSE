package com.example.safepulsemainproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FallDetectionActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val fallThreshold = 25.0  // Adjust based on testing
    private var fallDetected = false  // Declare as 'var' to allow reassignment
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fall_detection)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // Initialize sensor manager and accelerometer sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Check if the accelerometer is available
        if (accelerometer != null) {
            // Register the listener for accelerometer events
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the magnitude of the acceleration vector
            val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())

            // If the magnitude exceeds the threshold, we consider it a fall
            if (magnitude > fallThreshold && !fallDetected) {
                fallDetected = true  // Reassigning the fallDetected value
                triggerFallAlert()

                // Optional: Wait a short period before allowing another fall detection
                GlobalScope.launch {
                    kotlinx.coroutines.delay(5000)  // 5-second delay
                    fallDetected = false  // Reset the fall detection flag
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this case
    }

    private fun triggerFallAlert() {
        // Show a toast indicating fall detection
        Toast.makeText(this, "Fall detected! Sending SMS alert...", Toast.LENGTH_SHORT).show()

        // Get the location and send SMS
        getLocationAndSendSMS()
    }

    private fun getLocationAndSendSMS() {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val sharedPreferences = getSharedPreferences("SafePulsePrefs", MODE_PRIVATE)
                    val contacts = sharedPreferences.getStringSet("emergency_contacts", emptySet()) ?: emptySet()

                    if (contacts.isEmpty()) {
                        Toast.makeText(this, "No emergency contacts saved!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val message = "ALERT: EMERGENCY!!! URGENT: The user needs immediate help! Please respond now! Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"

                    for (contact in contacts) {
                        sendSMS(contact, message)
                    }
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendSMS(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show()
        } else {
            // Request SMS permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 0)
        }
    }

    // Handle the permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permissions denied
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle SMS permission result
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the listener again when the activity is resumed
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener when the activity is paused
        sensorManager.unregisterListener(this)
    }
}
