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

        val pre
