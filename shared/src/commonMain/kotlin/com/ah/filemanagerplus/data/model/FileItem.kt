package com.ah.filemanagerplus.data.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val isHidden: Boolean,
    val itemCount: Int = 0
) {
    val extension: String
        get() = if (!isDirectory && name.contains('.')) {
            name.substringAfterLast('.').lowercase()
        } else ""

    fun readableSize(): String {
        if (isDirectory) return ""
        if (size <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        val tb = gb * 1024
        return when {
            size >= tb -> "${formatDecimal(size / tb)} TB"
            size >= gb -> "${formatDecimal(size / gb)} GB"
            size >= mb -> "${formatDecimal(size / mb)} MB"
            size >= kb -> "${formatDecimal(size / kb)} KB"
            else -> "$size B"
        }
    }

    private fun formatDecimal(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val scaled = (value * 100).toLong()
        val whole = scaled / 100
        val frac = (scaled % 100).toInt()
        return if (frac == 0) {
            whole.toString()
        } else {
            val fracStr = frac.toString().padStart(2, '0').trimEnd('0')
            if (fracStr.isEmpty()) whole.toString() else "$whole.$fracStr"
        }
    }
}