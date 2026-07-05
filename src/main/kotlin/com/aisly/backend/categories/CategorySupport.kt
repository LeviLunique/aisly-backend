package com.aisly.backend.categories

import java.text.Normalizer

/** Case/diacritic-insensitive form used to compare category names. */
fun normalizeCategoryName(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
