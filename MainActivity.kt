package com.example.catpwddecoder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let { handleUri(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        findViewById<Button>(R.id.btnPick).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            pickFile.launch(intent)
        }
    }

    private fun handleUri(uri: Uri) {
        try {
            val xml = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (xml.isNullOrBlank()) {
                tvResult.text = "Не удалось прочитать файл или он пуст."
                return
            }
            val pairs = PasswordDecoder.parsePasswords(xml)
            tvResult.text = if (pairs.isEmpty()) {
                "В файле не найдено поле пароля (RealValue)."
            } else {
                buildString {
                    appendLine("=== Результат расшифровки ===")
                    appendLine()
                    pairs.forEach { (label, pwd) ->
                        appendLine("$label: ${pwd.ifBlank { "(пусто)" }}")
                    }
                }
            }
        } catch (e: Exception) {
            tvResult.text = "Ошибка: ${e.message}"
        }
    }
}