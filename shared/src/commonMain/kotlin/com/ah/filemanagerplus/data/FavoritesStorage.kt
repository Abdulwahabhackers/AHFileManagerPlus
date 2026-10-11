package com.ah.filemanagerplus.data

import com.ah.filemanagerplus.data.model.FavoriteItem

interface FavoritesStorage {
    fun loadAll(): List<FavoriteItem>
    fun save(items: List<FavoriteItem>)
}

expect fun createFavoritesStorage(): FavoritesStorage