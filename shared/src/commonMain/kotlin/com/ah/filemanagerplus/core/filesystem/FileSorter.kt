package com.ah.filemanagerplus.core.filesystem

import com.ah.filemanagerplus.data.SortBy
import com.ah.filemanagerplus.data.model.FileItem

object FileSorter {

    /**
     * يفلتر الملفات المخفية (إن لزم) ثم يرتبها حسب المعيار والاتجاه.
     * المجلدات تبقى دائماً في الأعلى.
     */
    fun apply(
        files: List<FileItem>,
        sortBy: SortBy,
        ascending: Boolean,
        showHidden: Boolean
    ): List<FileItem> {
        val filtered = if (showHidden) files else files.filter { !it.isHidden }

        val comparator: Comparator<FileItem> = when (sortBy) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareBy { if (it.isDirectory) -1L else it.size }
            SortBy.DATE -> compareBy { it.lastModified }
            SortBy.TYPE -> compareBy {
                if (it.isDirectory) "" else it.extension
            }
        }

        val ordered = if (ascending) comparator else comparator.reversed()

        return filtered.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }.then(ordered)
        )
    }

    /** إرجاع خيارات الفرز المتاحة */
    fun sortOptions(): List<Pair<SortBy, String>> = listOf(
        SortBy.NAME to "الاسم",
        SortBy.SIZE to "الحجم",
        SortBy.DATE to "التاريخ",
        SortBy.TYPE to "النوع"
    )
}