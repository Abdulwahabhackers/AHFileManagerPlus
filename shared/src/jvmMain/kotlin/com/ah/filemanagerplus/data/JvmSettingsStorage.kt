package com.ah.filemanagerplus.data

import java.util.prefs.Preferences

class JvmSettingsStorage : SettingsStorage {

    private val prefs: Preferences = Preferences.userRoot().node("com/ah/filemanagerplus")

    override fun load(): AppSettings {
        return try {
            AppSettings(
                themeMode = runCatching {
                    ThemeMode.valueOf(prefs.get("themeMode", ThemeMode.LIGHT.name))
                }.getOrDefault(ThemeMode.LIGHT),
                sizeUnit = runCatching {
                    SizeUnitMode.valueOf(prefs.get("sizeUnit", SizeUnitMode.AUTO.name))
                }.getOrDefault(SizeUnitMode.AUTO),
                defaultViewMode = runCatching {
                    ViewMode.valueOf(prefs.get("defaultViewMode", ViewMode.LIST.name))
                }.getOrDefault(ViewMode.LIST),
                defaultSortBy = runCatching {
                    SortBy.valueOf(prefs.get("defaultSortBy", SortBy.NAME.name))
                }.getOrDefault(SortBy.NAME),
                defaultSortAscending = prefs.getBoolean("defaultSortAscending", true),
                showHiddenByDefault = prefs.getBoolean("showHiddenByDefault", false),
                useTrashByDefault = prefs.getBoolean("useTrashByDefault", false),
                animateTransitions = prefs.getBoolean("animateTransitions", true),
                compactMode = prefs.getBoolean("compactMode", false),
                onboardingSeen = prefs.getBoolean("onboardingSeen", false)
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    override fun save(settings: AppSettings) {
        try {
            prefs.put("themeMode", settings.themeMode.name)
            prefs.put("sizeUnit", settings.sizeUnit.name)
            prefs.put("defaultViewMode", settings.defaultViewMode.name)
            prefs.put("defaultSortBy", settings.defaultSortBy.name)
            prefs.putBoolean("defaultSortAscending", settings.defaultSortAscending)
            prefs.putBoolean("showHiddenByDefault", settings.showHiddenByDefault)
            prefs.putBoolean("useTrashByDefault", settings.useTrashByDefault)
            prefs.putBoolean("animateTransitions", settings.animateTransitions)
            prefs.putBoolean("compactMode", settings.compactMode)
            prefs.putBoolean("onboardingSeen", settings.onboardingSeen)
            prefs.flush()
        } catch (e: Exception) {
            println("[Settings] save error: ${e.message}")
        }
    }
}

actual fun createSettingsStorage(): SettingsStorage = JvmSettingsStorage()