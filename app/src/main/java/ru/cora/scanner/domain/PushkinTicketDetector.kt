package ru.cora.scanner.domain

/**
 * Эвристика для билетов Пушкинской карты (PRO.Культура / партнёрские штрихкоды).
 * Точное правило задаётся бэкендом; здесь — распознавание маркеров в payload.
 */
object PushkinTicketDetector {

    private val markers = listOf(
        "pushkin",
        "пушкин",
        "culture",
        "proculture",
        "pushka",
    )

    fun isPushkinPayload(raw: String): Boolean {
        val lower = raw.lowercase()
        return markers.any { lower.contains(it) }
    }
}
