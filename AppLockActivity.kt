package com.example.safepulsemainproject

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class AppLockActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var failedAttempts = 0
    private val MAX_ATTEMPTS = 3
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var imageFile: File
    private lateinit var imageReader: ImageReader
    private var cameraDevice: CameraDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        checkAndRequestPermissions()

        val etPin = findViewById<EditText>(R.id.et_pin)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        sharedPreferences = getSharedPreferences("SafePulsePrefs", Context.MODE_PRIVATE)
        val storedPin = sharedPreferences.getString("appLockPin", "1234")
        val isNewUser = sharedPreferences.getBoolean("isNewUser", true) // Default: true (New user)

        btnUnlock.setOnClickListener {
            val enteredPin = etPin.text.toString()

            if (enteredPin == storedPin) {
                sharedPreferences.edit().putBoolean("isUnlocked", true).apply()

                if (isNewUser) {
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }

                finish()
            } else {
                failedAttempts++
                Toast.makeText(this, "Incorrect PIN. Attempt: $failedAttempts", Toast.LENGTH_SHORT).show()

                if (failedAttempts >= MAX_ATTEMPTS) {
                    captureIntruderPhoto()
                    failedAttempts = 0
                }
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: ""
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 101)
        }
    }

    private fun captureIntruderPhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return
        }

        try {
            val handler = Handler(Looper.getMainLooper())

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    setupImageReaderAndCapture()
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                }
            }, handler)
        } catch (e: Exception) {
            Log.e("CapturePhoto", "Error capturing photo: ${e.message}")
        }
    }

    private fun setupImageReaderAndCapture() {
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        imageFile = File(outputDir, "Intruder_$timeStamp.jpg")

        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
        }, Handler(Looper.getMainLooper()))

        val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)

        cameraDevice!!.createCaptureSession(
            listOf(imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureRequestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        Log.e("CapturePhoto", "Error capturing photo: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(applicationContext, "Camera configuration failed", Toast.LENGTH_SHORT).show()
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    private fun saveImage(image: Image) {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        image.close()

        try {
            val outputStream: OutputStream = contentResolver.openOutputStream(saveImageToGallery())!!
            outputStream.write(bytes)
            outputStream.close()

            val emergencyContacts = getEmergencyContacts()

            // ✅ Send SMS Alert
            sendSMS(emergencyContacts)

            // ✅ Send MMS with Image
            sendWhatsAppMessage(emergencyContacts, imageFile)


        } catch (e: Exception) {
            Log.e("SaveImage", "Failed to save image: ${e.message}")
        }
    }

    private fun saveImageToGallery(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Intruder_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SafePulse/")
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to save image")
    }

    private fun sendSMS(contacts: List<String>) {
        val message = "🚨 Unauthorized access detected! See attached image."
        val smsManager = SmsManager.getDefault()

        for (contact in contacts) {
            try {
                smsManager.sendTextMessage(contact, null, message, null, null)
            } catch (e: Exception) {
                Log.e("SendSMS", "Failed to send SMS: ${e.message}")
            }
        }
    }

    private fun sendWhatsAppMessage(contacts: List<String>, imageFile: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)

        for (contact in contacts) {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.putExtra(Intent.EXTRA_TEXT, "🚨 Unauthorized access detected! See attached image.")
                intent.setPackage("com.whatsapp") // Ensure it only opens WhatsApp
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                startActivity(intent)
            } catch (e: Exception) {
                Log.e("SendWhatsApp", "Failed to send image via WhatsApp: ${e.message}")
                Toast.makeText(this, "WhatsApp is not installed or the file format is not supported.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun getEmergencyContacts(): List<String> {
        val sharedPreferences = getSharedPreferences("SafePulsePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("emergency_contacts", emptySet())?.toList() ?: emptyList()
    }
}
