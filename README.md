🛡️ SafePulse – Smart Emergency & Fall Detection App
SafePulse is an Android application built to enhance user safety by combining fall detection, SOS flashlight, emergency contacts, and safe zone alerts. It also uses an app lock mechanism for added privacy.

🚀 Features
📉 Fall Detection: Detects user falls and alerts emergency contacts.

🔦 SOS Flashlight: Flashlight blinks in SOS pattern for visual signaling.

📇 Emergency Contacts: Users can add trusted contacts.

🗺️ Safe Zone: Define geographic safe zones to prevent wandering.

🔐 App Lock: App locks in the background for enhanced privacy.

📍 Location Access: For safe zone tracking and emergency use.

📱 App Structure (Main Components)
MainActivity.kt: Central hub with buttons for fall detection, SOS, contacts, safe zone, and logout.

AppLockActivity.kt: Handles unlocking mechanism on launch.

FallDetectionActivity.kt: Detects and processes fall events using sensors.

SOSTorch.kt: Manages blinking flashlight in SOS pattern.

AddEmergencyContactsActivity.kt: Lets users manage emergency contacts.

SafeZoneActivity.kt: Lets users define geofenced safe zones.

LoginActivity.kt: Handles login/logout sessions using SharedPreferences.

🧾 Permissions Used
Permission	Purpose
ACCESS_FINE_LOCATION	Used for Safe Zone and location tracking
SEND_SMS	To alert emergency contacts during a fall
CAMERA	Required to control the flashlight
ACCESS_COARSE_LOCATION	Backup for location if fine accuracy isn't available

All permissions are requested at runtime and handled in MainActivity.

🔐 App Lock Logic
The app stores a flag isUnlocked in SharedPreferences.

On resume, the flag is reset to false.

The user is redirected to AppLockActivity unless isUnlocked == true.

🧰 How to Build
Clone the project:

bash
Copy
Edit
git clone https://github.com/yourusername/safepulse.git
Open the project in Android Studio

Add permissions in AndroidManifest.xml:

xml
Copy
Edit
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CAMERA" />
Build and run the app on an Android device with the required sensors and camera.

🛠 Technologies Used
Kotlin

Android SDK

Jetpack Components

SensorManager (for fall detection)

SharedPreferences (for session/app lock)

Location & SMS APIs

💡 Future Improvements
Add background service for real-time fall detection.

Use biometric authentication instead of a basic lock.

Integrate cloud backup for contact data.

📬 Contact
Author: Jithin T
Gmail: jithinjithuedpl922@gmail.com
