package com.example.catpwddecoder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class ActivationActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "catpwd_prefs"
        const val KEY_ACTIVATED = "activated"

        fun passwordForDate(calendar: Calendar): String {
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            return "CAT${day + month + year}"
        }

        fun passwordForToday(): String = passwordForDate(Calendar.getInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ACTIVATED, false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_activation)

        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnActivate = findViewById<MaterialButton>(R.id.btnActivate)

        btnActivate.setOnClickListener {
            val input = etPassword.text?.toString()?.trim() ?: ""
            if (input == passwordForToday()) {
                prefs.edit().putBoolean(KEY_ACTIVATED, true).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
