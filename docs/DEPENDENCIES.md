# Dependencies

I kept this list short on purpose. There are no analytics, ads, networking, account or tracking
libraries. All versions are pinned in `gradle/libs.versions.toml`; I checked them against Maven
Central and Google Maven in October 2026.

| Dependency | Version | License | Why it's needed |
|---|---|---|---|
| Kotlin (`kotlin-multiplatform`, `kotlin-test`) | 2.4.21 | Apache-2.0 | The language, multiplatform build and test assertions. |
| Kotlin Compose compiler plugin | 2.4.21 | Apache-2.0 | Required to compile Compose code. |
| Compose Multiplatform (runtime, foundation, ui, components-resources, ui-test) | 1.12.1 | Apache-2.0 | Shared UI for Android and desktop, string resources, and UI tests. |
| Compose Material 3 (JetBrains) | 1.9.0 | Apache-2.0 | Dialogs, switches, chips, bottom sheets and theming. Writing these by hand would take a lot of code. |
| `compose.desktop.currentOs` (Skiko) | with Compose 1.12.1 | Apache-2.0 | Desktop rendering backend. |
| kotlinx.coroutines (core, android, swing, test) | 1.11.0 | Apache-2.0 | Debounced autosave and off-main-thread export and file work. |
| kotlinx.serialization JSON | 1.11.0 | Apache-2.0 | Reading and writing `.pixelpals` project files and settings. |
| AndroidX Activity Compose | 1.13.0 | Apache-2.0 | `ComponentActivity`, `setContent`, edge-to-edge, activity-result APIs (image picker, SAF) and `FileProvider` (through androidx.core). |
| Android Gradle Plugin | 9.4.1 | Apache-2.0 | Android build. Uses `com.android.kotlin.multiplatform.library` for the shared modules. |
| ktlint Gradle plugin (`org.jlleitschuh.gradle.ktlint`) | 14.2.0 | MIT | Lint and formatting in CI. It's a build tool only and isn't shipped in the app. |

The apps also pull in these transitive runtime dependencies, all Apache-2.0: the AndroidX support
libraries (core, lifecycle, savedstate, emoji2, window, profileinstaller and others), the JetBrains
lifecycle/savedstate/navigationevent ports, Skiko, `org.jetbrains:annotations`, `org.jspecify:jspecify`
and `com.google.guava:listenablefuture`. You can list them with
`./gradlew :androidApp:dependencies --configuration releaseRuntimeClasspath` and
`./gradlew :desktopApp:dependencies --configuration jvmRuntimeClasspath`.

Every pinned version above was the latest stable release on 13 October 2026.

Things I wrote myself instead of adding a dependency:

- the PNG encoder
- the GIF encoder with LZW
- run-length pixel encoding
- flood fill and rasterization
- sound synthesis
- the pixel-art icon set

Compression and ZIP use the JDK's built-in `java.util.zip`, and image decoding uses the platform
codecs (`BitmapFactory` on Android, `javax.imageio` on desktop).
