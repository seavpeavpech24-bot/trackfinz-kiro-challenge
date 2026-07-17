package com.trackfinz.app.i18n

import androidx.compose.runtime.compositionLocalOf

/**
 * Provides a lambda (key) -> String throughout the Compose tree.
 * Usage: val s = LocalStrings.current; Text(s(Str.HELLO))
 */
val LocalStrings = compositionLocalOf<(String) -> String> {
    { key -> key } // default: return key as-is (should never be reached)
}

/** Provides the current AppLanguage so screens can use month helpers etc. */
val LocalLanguage = compositionLocalOf { AppLanguage.ENGLISH }
