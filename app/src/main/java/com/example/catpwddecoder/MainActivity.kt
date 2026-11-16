package com.example.catpwddecoder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvEcmName: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var btnShare: MaterialButton
    private val adapter = ConfigAdapter()

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

        tvEcmName = findViewById(R.id.tvEcmName)
        recycler = findViewById(R.id.recyclerConfig)
        btnShare = findViewById(R.id.btnShare)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnPick).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            pickFile.launch(intent)
        }

        findViewById<MaterialButton>(R.id.btnCompare).setOnClickListener {
            startActivity(Intent(this, CompareActivity::class.java))
        }

        btnShare.setOnClickListener { shareResult() }
    }

    private fun handleUri(uri: Uri) {
        try {
            val xml = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (xml.isNullOrBlank()) {
                tvEcmName.text = ""
                adapter.submit(emptyList())
                btnShare.visibility = View.GONE
                btnShare.isEnabled = false
                return
            }

            val ecmName = EcmParser.parseEcmName(xml) ?: "—"
            tvEcmName.text = if (ecmName == "—") "" else "Модель: $ecmName"

            lastEngineSerial = EcmParser.parseEngineSerial(xml) ?: "—"
            lastPasswords = EcmParser.parsePasswords(xml)
            val allItems = EcmParser.parseAll(xml)

            val displayList = mutableListOf<ConfigItem>()
            val grouped = allItems.groupBy { it.section }
            for (section in listOf("ECM Info", "Configuration", "Totals")) {
                val sectionItems = grouped[section] ?: continue
                if (section != "ECM Info") {
                    displayList += ConfigItem(section, "", "")
                }
                displayList += sectionItems
            }

            adapter.submit(displayList)

            if (lastPasswords.isNotEmpty()) {
                btnShare.visibility = View.VISIBLE
                btnShare.isEnabled = true
            } else {
                btnShare.visibility = View.GONE
                btnShare.isEnabled = false
            }
        } catch (e: Exception) {
            tvEcmName.text = ""
            adapter.submit(emptyList())
            btnShare.visibility = View.GONE
            btnShare.isEnabled = false
        }
    }

    private fun shareResult() {
        if (lastPasswords.isEmpty() && lastEngineSerial == "—") return
        val text = buildString {
            appendLine("Engine Serial Number: $lastEngineSerial")
            appendLine()
            lastPasswords.forEach { (label, pwd) ->
                appendLine("$label: ${pwd.ifBlank { "(пусто)" }}")
            }
        }.trimEnd()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CAT ECM ($lastEngineSerial)")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться результатом"))
    }
}
