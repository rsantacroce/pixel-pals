import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")
    androidLibrary {
        namespace = "app.pixelpals.io"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
    }

    // Android and desktop are both JVMs, so java.io / java.util.zip code lives once in "jvmShared".
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withCompilations {
                    it.target.platformType == KotlinPlatformType.jvm || it.target.platformType == KotlinPlatformType.androidJvm
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.coreModel)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
