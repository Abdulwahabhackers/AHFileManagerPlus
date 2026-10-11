import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.ah.filemanagerplus.MainKt"

        // ✅ DirectX 11 (متوافق أكثر من DX12) + ذاكرة مخصصة
        jvmArgs += listOf(
            "-Xmx2048M",
            "-Dskiko.renderApi=DIRECT3D"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "AHFileManagerPlus"
            packageVersion = "1.0.0"
            description = "AH File Manager Plus - مدير الملفات +"
            vendor = "HACKERS AH"

            windows {
                iconFile.set(project.file("icon.ico"))
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "b8f7a3d2-4c5e-4f1a-9b8d-1234567890ab"
            }
        }
    }
}