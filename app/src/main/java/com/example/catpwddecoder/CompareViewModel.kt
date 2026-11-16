package com.example.catpwddecoder

import androidx.lifecycle.ViewModel

class CompareViewModel : ViewModel() {
    var leftName: String = ""
    var rightName: String = ""
    var leftItems: List<ConfigItem> = emptyList()
    var rightItems: List<ConfigItem> = emptyList()
    var hideMatches: Boolean = false
}
