package com.example.ui.screens

internal data class PdfPageSearchMatch(val pageNumber: Int)

internal data class PdfSearchSnapshot(
    val count: Int = 0,
    val truncated: Boolean = false,
    val matches: List<PdfPageSearchMatch> = emptyList()
)
