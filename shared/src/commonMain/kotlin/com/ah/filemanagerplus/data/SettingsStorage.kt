package com.ah.filemanagerplus.data

interface SettingsStorage {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}

expect fun createSettingsStorage(): SettingsStorage