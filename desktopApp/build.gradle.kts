import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.ui)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.pixelpals.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // From ./gradlew suggestRuntimeModules; java.desktop (AWT dialogs, ImageIO, Java Sound) is included by default.
            modules("java.instrument", "jdk.unsupported")
            packageName = "PixelPals"
            packageVersion = "1.0.0"
            description = "Pixel Pals - a pixel art editor for kids"
            vendor = "Pixel Pals"
            macOS {
                bundleID = "app.pixelpals"
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                menuGroup = "Pixel Pals"
                upgradeUuid = "6a4f3c1e-2b7d-4e8a-9f10-5c3d2e1b0a77"
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
