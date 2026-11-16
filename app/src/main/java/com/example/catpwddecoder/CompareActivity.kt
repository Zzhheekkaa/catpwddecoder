package com.example.catpwddecoder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

class CompareActivity : AppCompatActivity() {

    private val viewModel: CompareViewModel by viewModels()

    private lateinit var recycler: RecyclerView
    private lateinit var cbHide: MaterialCheckBox
    private lateinit var tvLeftName: TextView
    private lateinit var tvRightName: TextView

    private val adapter = CompareAdapter()

    private val pickLeft = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            res.data?.data?.let {
                viewModel.leftName = fileNameOf(it)
                viewModel.leftItems = loadItems(it)
                restoreFromViewModel()
            }
        }
    }

    private val pickRight = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            res.data?.data?.let {
                viewModel.rightName = fileNameOf(it)
                viewModel.rightItems = loadItems(it)
                restoreFromViewModel()
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

        cbHide.isChecked = viewModel.hideMatches
        adapter.setHideMatches(viewModel.hideMatches)

        cbHide.setOnCheckedChangeListener { _, checked ->
            viewModel.hideMatches = checked
            adapter.setHideMatches(checked)
        }

        findViewById<MaterialButton>(R.id.btnPickLeft).setOnClickListener {
            launchPicker(pickLeft)
        }
        findViewById<MaterialButton>(R.id.btnPickRight).setOnClickListener {
            launchPicker(pickRight)
        }

        restoreFromViewModel()
    }

    private fun restoreFromViewModel() {
        tvLeftName.text = viewModel.leftName.ifBlank { getString(R.string.left_file) }
        tvRightName.text = viewModel.rightName.ifBlank { getString(R.string.right_file) }

        if (viewModel.leftItems.isEmpty() && viewModel.rightItems.isEmpty()) {
            adapter.submit(emptyList())
        } else {
            adapter.submit(buildRows(viewModel.leftItems, viewModel.rightItems))
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
