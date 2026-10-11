package com.ah.filemanagerplus.data

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class SizeUnitMode { AUTO, MB, GB }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val sizeUnit: SizeUnitMode = SizeUnitMode.AUTO,
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val defaultSortBy: SortBy = SortBy.NAME,
    val defaultSortAscending: Boolean = true,
    val showHiddenByDefault: Boolean = false,
    val useTrashByDefault: Boolean = false,
    val animateTransitions: Boolean = true,
    val compactMode: Boolean = false,
    val onboardingSeen: Boolean = false
)