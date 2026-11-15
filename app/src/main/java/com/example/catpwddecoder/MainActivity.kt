package com.example.catpwddecoder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvEngineSerial: TextView
    private lateinit var btnShare: MaterialButton

    private var lastEngineSerial: String = "—"
    private var lastPasswords: List<Pair<String, String>> = emptyList()

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
        tvEngineSerial = findViewById(R.id.tvEngineSerial)
        btnShare = findViewById(R.id.btnShare)

        findViewById<MaterialButton>(R.id.btnPick).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            pickFile.launch(intent)
        }

        btnShare.setOnClickListener { shareResult() }
    }

    private fun handleUri(uri: Uri) {
        try {
            val xml = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (xml.isNullOrBlank()) {
                tvResult.text = "Не удалось прочитать файл или он пуст."
                btnShare.isEnabled = false
                return
            }

            lastEngineSerial = PasswordDecoder.parseEngineSerial(xml) ?: "—"
            lastPasswords = PasswordDecoder.parsePasswords(xml)

            tvEngineSerial.text = lastEngineSerial

            if (lastPasswords.isEmpty()) {
                tvResult.text = "В файле не найдено поле пароля (RealValue)."
                btnShare.isEnabled = false
            } else {
                tvResult.text = buildString {
                    lastPasswords.forEach { (label, pwd) ->
                        appendLine("$label: ${pwd.ifBlank { "(пусто)" }}")
                    }
                }.trimEnd()
                btnShare.isEnabled = true
            }
        } catch (e: Exception) {
            tvResult.text = "Ошибка: ${e.message}"
            btnShare.isEnabled = false
        }
    }

        private fun shareResult() {
        if (lastPasswords.isEmpty()) return
        val text = buildString {
            appendLine("Engine Serial Number: $lastEngineSerial")
            appendLine()
            lastPasswords.forEach { (label, pwd) ->
                appendLine("$label: ${pwd.ifBlank { "(пусто)" }}")
            }
        }.trimEnd()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CAT ECM passwords ($lastEngineSerial)")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться результатом"))
    }
}
