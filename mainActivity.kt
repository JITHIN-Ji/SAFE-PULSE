package com.example.safepulsemainproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var sosTorch: SOSTorch
    private var isSOSActive = false
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the app is unlocked
        val sharedPreferences = getSharedPreferences("SafePulsePrefs", MODE_PRIVATE)
        val isUnlocked = sharedPreferences.getBoolean("isUnlocked", false)

        if (!isUnlocked) {
            startActivity(Intent(this, AppLockActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val btnFallDetection = findViewById<Button>(R.id.btn_fall_detection)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnAddContacts = findViewById<Button>(R.id.btn_add_contacts)
        val btnSetSafeZone = findViewById<Button>(R.id.btn_set_safe_zone)
        val btnSOSTorch = findViewById<Button>(R.id.btn_sos_flashlight) // Added button reference
        tvStatus = findViewById(R.id.tv_status)

        // Initialize SOS Torch
        sosTorch = SOSTorch(this)

        // Check and request necessary permissions
        checkPermissions()

        btnFallDetection.setOnClickListener {
            tvStatus.text = "Fall detection is active. Monitoring for falls..."
            val intent = Intent(this, FallDetectionActivity::class.java)
            startActivity(intent)
        }

        btnAddContacts.setOnClickListener {
            val intent = Intent(this, AddEmergencyContactsActivity::class.java)
            startActivity(intent)
        }

        btnSetSafeZone.setOnClickListener {
            val intent = Intent(this, SafeZoneActivity::class.java)
            startActivity(intent)
        }

        btnSOSTorch.setOnClickListener {
            if (isSOSActive) {
                sosTorch.stopSOS()
                btnSOSTorch.text = "Start SOS Flashlight"
            } else {
                sosTorch.startSOS()
                btnSOSTorch.text = "Stop SOS Flashlight"
            }
            isSOSActive = !isSOSActive
        }

        btnLogout.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        tvStatus.text = ""
    }

    override fun onPause() {
        super.onPause()
        // Lock the app when it goes to the background
        val sharedPreferences = getSharedPreferences("SafePulsePrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isUnlocked", false).apply()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA // Required for flashlight
        )

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
