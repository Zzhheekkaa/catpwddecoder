package com.example.catpwddecoder

data class CompareRow(
    val section: String,
    val description: String,
    val valueLeft: String?,
    val valueRight: String?,
    val isMatch: Boolean
)
