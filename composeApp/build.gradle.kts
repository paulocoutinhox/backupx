import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "com.backupx"
version = "1.0.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/jvmMain/kotlin")
    val appVersion = project.version.toString()

    inputs.property("appVersion", appVersion)
    outputs.dir(outputDir)

    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/backupx/app")
        packageDir.mkdirs()
        packageDir.resolve("BuildConfig.kt").writeText(
            """
            package com.backupx.app

            object BuildConfig {
                const val APP_VERSION = "$appVersion"
            }
            """.trimIndent() + "\n"
        )
    }
}

// the native library ships inside the app bundle so jpackage signs it with the app identity
val appResourcesDir = layout.buildDirectory.dir("appResources")

// compile the macOS native bridge (jni dylib) when building on macOS
val compileMacBridge = tasks.register<Exec>("compileMacBridge") {
    onlyIf { System.getProperty("os.name").lowercase().contains("mac") }
    val source = file("src/jvmMain/native/macbridge.m")
    val output = appResourcesDir.get().dir("macos").file("macbridge.dylib").asFile
    val javaHome = System.getProperty("java.home")
    inputs.file(source)
    inputs.property("javaHome", javaHome)
    outputs.file(output)
    doFirst { output.parentFile.mkdirs() }
    commandLine(
        "clang", "-dynamiclib", "-fobjc-arc", "-mmacosx-version-min=11.0",
        "-arch", "arm64", "-arch", "x86_64",
        "-I", "$javaHome/include", "-I", "$javaHome/include/darwin",
        "-framework", "Foundation", "-framework", "AppKit", "-framework", "Security",
        "-o", output.absolutePath, source.absolutePath
    )
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.minio)
            }
        }
    }
}

// build the native library before it is staged into the app bundle resources
tasks.matching { it.name == "prepareAppResources" }.configureEach {
    dependsOn(compileMacBridge)
}


compose.desktop {
    application {
        mainClass = "com.backupx.app.MainKt"

        // -PmacAppStore=true switches to a sandboxed Mac App Store build (.pkg)
        val macAppStore = (project.findProperty("macAppStore") as String?) == "true"

        nativeDistributions {
            if (macAppStore) {
                targetFormats(TargetFormat.Pkg)
            } else {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            }
            packageName = "BackupX"
            packageVersion = version.toString()
            appResourcesRootDir.set(appResourcesDir)
            copyright = "2026 Paulo Coutinho. All rights reserved."
            vendor = "Paulo Coutinho"
            licenseFile.set(project.file("../LICENSE.txt"))
            modules(
                "jdk.unsupported"
            )

            windows {
                dirChooser = true
                menuGroup = "BackupX"
                iconFile.set(project.file("src/jvmMain/resources/icons/app.ico"))
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/app.icns"))
                bundleID = "com.backupx.app"
                appCategory = "public.app-category.utilities"
                appStore = macAppStore

                // arm64-only builds require a deployment target of 12.0+ to pass app store validation
                minimumSystemVersion = "12.0"

                val providers = project.providers
                if (macAppStore) {
                    // app store: sandboxed, signed with the 3rd party mac developer certificates
                    entitlementsFile.set(project.file("entitlements-mac.plist"))
                    runtimeEntitlementsFile.set(project.file("runtime-entitlements-mac.plist"))
                    provisioningProfile.set(project.file("embedded.provisionprofile"))

                    signing {
                        sign.set(true)
                        identity.set(providers.environmentVariable("MAS_APP_IDENTITY"))
                    }
                } else {
                    // direct distribution: developer id, notarized dmg
                    signing {
                        sign.set(true)
                        identity.set(providers.environmentVariable("SIGNING_IDENTITY"))
                    }

                    notarization {
                        appleID.set(providers.environmentVariable("NOTARIZATION_APPLE_ID"))
                        teamID.set(providers.environmentVariable("NOTARIZATION_TEAM_ID"))
                        password.set(providers.environmentVariable("NOTARIZATION_PASSWORD"))
                    }
                }
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icons/app.png"))
            }
        }
    }
}
