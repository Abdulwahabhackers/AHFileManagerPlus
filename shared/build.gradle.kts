plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val javafxPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    when {
        os.contains("win") -> "win"
        os.contains("mac") -> if (arch.contains("aarch64")) "mac-aarch64" else "mac"
        else -> "linux"
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        jvmMain.dependencies {
            implementation(libs.jlayer)
            implementation(libs.mp3spi)
            implementation(libs.tritonus)
            implementation(libs.mp3agic)
            implementation(libs.zip4j)
            implementation(libs.commonsCompress)

            val jfxVersion = libs.versions.javafx.get()
            implementation("org.openjfx:javafx-base:$jfxVersion:$javafxPlatform")
            implementation("org.openjfx:javafx-graphics:$jfxVersion:$javafxPlatform")
            implementation("org.openjfx:javafx-media:$jfxVersion:$javafxPlatform")
            implementation("org.openjfx:javafx-swing:$jfxVersion:$javafxPlatform")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}