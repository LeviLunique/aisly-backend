package com.aisly.backend.lists

enum class ShoppingUnit(val wireValue: String) {
    UNIT("un"),
    GRAM("g"),
    KILOGRAM("kg"),
    MILLILITER("mL"),
    LITER("L");

    companion object {
        fun fromWire(value: String): ShoppingUnit =
            entries.firstOrNull { it.wireValue == value } ?: UNIT
    }
}
