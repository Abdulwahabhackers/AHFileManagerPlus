package com.ah.filemanagerplus.data

import com.ah.filemanagerplus.data.model.FavoriteItem
import java.util.prefs.Preferences

class JvmFavoritesStorage : FavoritesStorage {

    private val prefs: Preferences =
        Preferences.userRoot().node("com/ah/filemanagerplus/favorites")

    private val separator = "\n\u0001"

    override fun loadAll(): List<FavoriteItem> {
        return try {
            val raw = prefs.get("items", "")
            if (raw.isBlank()) return emptyList()
            raw.split(separator).mapNotNull { line ->
                val parts = line.split("\u0002")
                if (parts.size < 4) null
                else FavoriteItem(
                    path = parts[0],
                    name = parts[1],
                    isDirectory = parts[2] == "1",
                    addedAt = parts[3].toLongOrNull() ?: 0L
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun save(items: List<FavoriteItem>) {
        try {
            val raw = items.joinToString(separator) {
                "${it.path}\u0002${it.name}\u0002${if (it.isDirectory) "1" else "0"}\u0002${it.addedAt}"
            }
            prefs.put("items", raw)
            prefs.flush()
        } catch (e: Exception) {
            println("[Favorites] save error: ${e.message}")
        }
    }
}

actual fun createFavoritesStorage(): FavoritesStorage = JvmFavoritesStorage()