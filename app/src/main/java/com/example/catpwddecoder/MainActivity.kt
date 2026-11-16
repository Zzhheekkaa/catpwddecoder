package com.example.catpwddecoder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var tvEcmName: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var btnShare: MaterialButton
    private val adapter = ConfigAdapter()

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

        // Восстановление из ViewModel (переживает поворот)
        restoreFromViewModel()
    }

    private fun restoreFromViewModel() {
        tvEcmName.text = viewModel.ecmName

        if (viewModel.items.isNotEmpty()) {
            adapter.submit(buildDisplayList(viewModel.items))
        }

        if (viewModel.passwords.isNotEmpty()) {
            btnShare.visibility = View.VISIBLE
            btnShare.isEnabled = true
        } else {
            btnShare.visibility = View.GONE
            btnShare.isEnabled = false
        }
    }

    private fun handleUri(uri: Uri) {
        try {
            val xml = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (xml.isNullOrBlank()) {
                viewModel.items = emptyList()
                viewModel.ecmName = ""
                viewModel.passwords = emptyList()
                viewModel.engineSerial = "—"
                restoreFromViewModel()
                return
            }

            val ecmName = EcmParser.parseEcmName(xml) ?: "—"
            val displayName = if (ecmName == "—") "" else "Модель: $ecmName"

            val allItems = EcmParser.parseAll(xml)
            val passwords = EcmParser.parsePasswords(xml)
            val engineSerial = EcmParser.parseEngineSerial(xml) ?: "—"

            viewModel.ecmName = displayName
            viewModel.items = allItems
            viewModel.passwords = passwords
            viewModel.engineSerial = engineSerial

            tvEcmName.text = displayName
            adapter.submit(buildDisplayList(allItems))

            if (passwords.isNotEmpty()) {
                btnShare.visibility = View.VISIBLE
                btnShare.isEnabled = true
            } else {
                btnShare.visibility = View.GONE
                btnShare.isEnabled = false
            }
        } catch (e: Exception) {
            viewModel.items = emptyList()
            viewModel.ecmName = ""
            viewModel.passwords = emptyList()
            viewModel.engineSerial = "—"
            restoreFromViewModel()
        }
    }

    private fun buildDisplayList(allItems: List<ConfigItem>): List<ConfigItem> {
        val displayList = mutableListOf<ConfigItem>()
        val grouped = allItems.groupBy { it.section }
        for (section in listOf("ECM Info", "Configuration", "Totals")) {
            val sectionItems = grouped[section] ?: continue
            if (section != "ECM Info") {
                displayList += ConfigItem(section, "", "")
            }
            displayList += sectionItems
        }
        return displayList
    }

    private fun shareResult() {
        if (viewModel.passwords.isEmpty() && viewModel.engineSerial == "—") return
        val text = buildString {
            appendLine("Engine Serial Number: ${viewModel.engineSerial}")
            appendLine()
            viewModel.passwords.forEach { (label, pwd) ->
                appendLine("$label: ${pwd.ifBlank { "(пусто)" }}")
            }
        }.trimEnd()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CAT ECM (${viewModel.engineSerial})")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться результатом"))
    }
}
