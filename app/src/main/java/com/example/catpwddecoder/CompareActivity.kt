package com.example.catpwddecoder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

class CompareActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var cbHide: MaterialCheckBox
    private lateinit var tvLeftName: TextView
    private lateinit var tvRightName: TextView

    private val adapter = CompareAdapter()

    private var leftItems: List<ConfigItem> = emptyList()
    private var rightItems: List<ConfigItem> = emptyList()

    private val pickLeft = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let {
                tvLeftName.text = fileNameOf(it)
                leftItems = loadItems(it)
                rebuild()
            }
        }
    }

    private val pickRight = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let {
                tvRightName.text = fileNameOf(it)
                rightItems = loadItems(it)
                rebuild()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        recycler = findViewById(R.id.recyclerCompare)
        cbHide = findViewById(R.id.cbHideMatches)
        tvLeftName = findViewById(R.id.tvLeftFileName)
        tvRightName = findViewById(R.id.tvRightFileName)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        cbHide.setOnCheckedChangeListener { _, checked ->
            adapter.setHideMatches(checked)
        }

        findViewById<MaterialButton>(R.id.btnPickLeft).setOnClickListener {
            launchPicker(pickLeft)
        }
        findViewById<MaterialButton>(R.id.btnPickRight).setOnClickListener {
            launchPicker(pickRight)
        }
    }

    private fun launchPicker(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        launcher.launch(intent)
    }

    private fun fileNameOf(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    private fun loadItems(uri: Uri): List<ConfigItem> {
        return try {
            val xml = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (xml.isNullOrBlank()) emptyList() else EcmParser.parseAll(xml)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun rebuild() {
        if (leftItems.isEmpty() && rightItems.isEmpty()) {
            adapter.submit(emptyList())
            return
        }
        adapter.submit(buildRows(leftItems, rightItems))
    }

    private fun buildRows(left: List<ConfigItem>, right: List<ConfigItem>): List<CompareRow> {
        val sectionOrder = linkedSetOf<String>()
        sectionOrder += "ECM Info"
        sectionOrder += "Configuration"
        sectionOrder += "Totals"
        (left + right).forEach { sectionOrder += it.section }

        val rows = mutableListOf<CompareRow>()
        for (section in sectionOrder) {
            val leftSection = left.filter { it.section == section }
            val rightSection = right.filter { it.section == section }
            val descOrder = linkedSetOf<String>()
            leftSection.forEach { descOrder += it.description }
            rightSection.forEach { descOrder += it.description }

            for (desc in descOrder) {
                val l = leftSection.firstOrNull { it.description == desc }?.value
                val r = rightSection.firstOrNull { it.description == desc }?.value
                val match = l != null && r != null && l == r
                rows += CompareRow(section, desc, l, r, match)
            }
        }
        return rows
    }
}
