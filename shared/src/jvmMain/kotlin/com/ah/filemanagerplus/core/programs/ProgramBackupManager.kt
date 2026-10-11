package com.ah.filemanagerplus.core.programs

import com.ah.filemanagerplus.data.model.InstalledProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProgramBackupManager {

    suspend fun backup(
        program: InstalledProgram,
        onProgress: (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            val userHome = System.getProperty("user.home")
                ?: return@withContext null
            val docs = File(userHome, "Documents")
            val backupsRoot = File(docs, "AHFM Backups")
            if (!backupsRoot.exists()) backupsRoot.mkdirs()

            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val safeName = program.name.replace(Regex("""[<>:"/\\|?*]"""), "_").take(60)
            val backupDir = File(backupsRoot, "$safeName-$stamp")
            backupDir.mkdirs()

            val infoFile = File(backupDir, "backup_info.txt")
            infoFile.writeText(buildInfoText(program), Charsets.UTF_8)

            val install = program.installLocation
            val installDir = if (install.isNotBlank()) File(install) else null
            if (installDir != null && installDir.exists() && installDir.isDirectory) {
                val filesDir = File(backupDir, "files")
                filesDir.mkdirs()
                onProgress("جارٍ نسخ الملفات...")
                installDir.copyRecursively(filesDir, overwrite = true)
            } else {
                onProgress("مجلد التثبيت غير متاح — تم حفظ المعلومات فقط")
            }

            onProgress("جارٍ إنشاء الأرشيف...")
            val zipFile = File(backupsRoot, "$safeName-$stamp.zip")
            val zf = ZipFile(zipFile)
            val params = ZipParameters().apply {
                compressionLevel = net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
            }
            backupDir.listFiles()?.forEach { f ->
                if (f.isDirectory) zf.addFolder(f, params)
                else zf.addFile(f, params)
            }

            backupDir.deleteRecursively()

            onProgress("تم الحفظ في: ${zipFile.absolutePath}")
            zipFile.absolutePath
        } catch (e: Exception) {
            println("[Backup] error: ${e.message}")
            onProgress("فشل: ${e.message}")
            null
        }
    }

    private fun buildInfoText(p: InstalledProgram): String {
        return buildString {
            appendLine("=== نسخة احتياطية — AH File Manager Plus ===")
            appendLine("التاريخ: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            appendLine("الاسم: ${p.name}")
            appendLine("الإصدار: ${p.version}")
            appendLine("الناشر: ${p.publisher}")
            appendLine("تاريخ التثبيت: ${p.installDate}")
            appendLine("الحجم التقديري: ${p.estimatedSize} bytes")
            appendLine("مسار التثبيت: ${p.installLocation}")
            appendLine("أمر إلغاء التثبيت: ${p.uninstallString}")
            appendLine("مسار الأيقونة: ${p.iconPath ?: "—"}")
            appendLine()
            appendLine("ملاحظة: لإعادة التثبيت، استخدم أمر إلغاء التثبيت الأصلي أو مثبّت البرنامج.")
            appendLine("هذه النسخة تحتوي على ملفات البرنامج ومعلوماته فقط.")
        }
    }
}