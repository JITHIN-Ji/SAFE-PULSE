package com.example.safepulsemainproject
import android.content.Context

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddEmergencyContactsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val MAX_CONTACTS = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_emergency_contacts)

        sharedPreferences = getSharedPreferences("SafePulsePrefs", MODE_PRIVATE)

        val editTexts = arrayOf(
            findViewById<EditText>(R.id.et_contact_1),
            findViewById<EditText>(R.id.et_contact_2),
            findViewById<EditText>(R.id.et_contact_3),
            findViewById<EditText>(R.id.et_contact_4),
            findViewById<EditText>(R.id.et_contact_5)
        )

        val btnSaveContacts = findViewById<Button>(R.id.btn_save_contacts)

        // Load previously saved contacts (if any)
        loadSavedContacts(editTexts)

        btnSaveContacts.setOnClickListener {
            saveContacts(editTexts)
        }
    }
    private fun getEmergencyContacts(): List<String> {
        val sharedPreferences = getSharedPreferences("SafePulsePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("emergencyContacts", emptySet())?.toList() ?: emptyList()
    }


    private fun saveContacts(editTexts: Array<EditText>) {
        val editor = sharedPreferences.edit()
        val contacts = mutableListOf<String>()

        for (i in editTexts.indices) {
            val contact = editTexts[i].text.toString().trim()
            if (contact.isNotEmpty()) {
                contacts.add(contact)
            }
        }

        if (contacts.size in 1..MAX_CONTACTS) {
            editor.putStringSet("emergency_contacts", contacts.toSet())
            editor.apply()
            Toast.makeText(this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Please enter at least 1 and at most 5 contacts.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedContacts(editTexts: Array<EditText>) {
        val savedContacts = sharedPreferences.getStringSet("emergency_contacts", emptySet()) ?: emptySet()
        val contactList = savedContacts.toList()

        for (i in contactList.indices) {
            if (i < editTexts.size) {
                editTexts[i].setText(contactList[i])
            }
        }
    }
}
