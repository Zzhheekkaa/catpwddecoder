package com.example.catpwddecoder

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var engineSerial: String = "—"
    var ecmName: String = ""
    var passwords: List<Pair<String, String>> = emptyList()
    var items: List<ConfigItem> = emptyList()
}
