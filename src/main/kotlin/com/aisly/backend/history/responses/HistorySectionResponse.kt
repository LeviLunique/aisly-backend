package com.aisly.backend.history.responses

import com.aisly.backend.history.HistorySection

data class HistorySectionResponse(val id: String, val name: String, val items: List<HistoryItemResponse>) {
    companion object {
        fun from(section: HistorySection) = HistorySectionResponse(section.id, section.name, section.items.map(HistoryItemResponse::from))
    }
}
