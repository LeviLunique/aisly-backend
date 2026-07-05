package com.aisly.backend.lists

enum class TemplateRecurrence {
    WEEKLY,
    BIWEEKLY,
    MONTHLY;

    companion object {
        fun fromIos(value: String?): TemplateRecurrence? =
            when (value?.trim()?.lowercase()) {
                "weekly" -> WEEKLY
                "biweekly" -> BIWEEKLY
                "monthly" -> MONTHLY
                null, "" -> null
                else -> error("Unsupported template recurrence: $value")
            }
    }

    fun toIos(): String = name.lowercase()
}
