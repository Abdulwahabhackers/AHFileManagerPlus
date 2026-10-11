package com.ah.filemanagerplus.data.model

enum class DriveType {
    FIXED,
    REMOVABLE,
    NETWORK,
    OPTICAL,
    UNKNOWN
}

data class Drive(
    val rootPath: String,
    val label: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val type: DriveType
) {
    val usedBytes: Long get() = totalBytes - freeBytes

    val usedPercent: Int
        get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

    /** حرف القرص فقط: "C:" */
    val letter: String
        get() = rootPath.trimEnd('\\', '/').let {
            if (it.endsWith(":")) it else "$it:"
        }

    /** التسمية بدون (C:) */
    val cleanLabel: String
        get() {
            var l = label.trim()
            l = l.replace(Regex("""\s*\([A-Za-z]:?\\?\)\s*$"""), "")
            l = l.replace(Regex("""\s*[A-Za-z]:\\?\s*$"""), "")
            return l.trim()
        }

    /** النص الرئيسي (اسم القرص) */
    val displayTitle: String
        get() {
            val l = cleanLabel
            return if (l.isBlank() || l.equals(letter, true)) "القرص $letter" else l
        }

    /** النص الثانوي (حرف القرص) */
    val displaySubtitle: String
        get() = letter
}