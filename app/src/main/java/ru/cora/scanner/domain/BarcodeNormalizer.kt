package ru.cora.scanner.domain

object BarcodeNormalizer {
    fun normalize(raw: String): String = raw.trim().replace("\u0000", "")
}
