@file:OptIn(ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.crowded-libs"
version = "0.1.1"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-attach-js-exception")
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.duks)
                implementation(libs.lmdb)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io.core)
            }
        }
    }
}

android {
    namespace = "io.github.crowdedlibs.duks.storage.lmdb"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.all { test ->
            test.enabled = true
        }
    }
}

dokka {
    moduleName = project.name
    dokkaSourceSets {
        named("commonMain")
    }
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Function to copy WASM resources for tests
fun copyWasmResources(targetType: String) {
    val packagesDir = rootProject.layout.buildDirectory.dir("$targetType/packages").get().asFile
    val lmdbVersion = libs.versions.lmdb.get()
    val lmdbPackageDir = rootProject.layout.buildDirectory.dir("$targetType/packages_imported/kotlin-lmdb-wasm-js/$lmdbVersion/kotlin").get().asFile
    
    if (lmdbPackageDir.exists() && packagesDir.exists()) {
        // Find all test kotlin directories
        packagesDir.walkTopDown().forEach { dir ->
            if (dir.name == "kotlin" && dir.isDirectory && 
                dir.absolutePath.contains("test")) {
                
                // Copy WASM resources to test kotlin directories
                copy {
                    from(lmdbPackageDir) {
                        include("lmdb-wrapper.mjs", "lmdb.mjs", "lmdb.wasm")
                    }
                    into(dir)
                }
                
                logger.lifecycle("Copied WASM resources to test directory: ${dir.absolutePath}")
            }
        }
    } else {
        logger.warn("LMDB package directory not found: $lmdbPackageDir")
    }
}

// Copy WASM resources for both Node.js and browser tests
tasks.named("wasmJsNodeTest") {
    doFirst {
        copyWasmResources("wasm")
    }
}

tasks.named("wasmJsBrowserTest") {
    doFirst {
        copyWasmResources("wasm")
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), "duks-storage-lmdb", version.toString())

    pom {
        name = project.name
        description = "LMDB-based persistent storage implementation for duks library"
        inceptionYear = "2025"
        url = "https://github.com/crowded-libs/duks-storage-lmdb/"
        licenses {
            license {
                name = "Apache 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "coreykaylor"
                name = "Corey Kaylor"
                email = "corey@kaylors.net"
            }
        }
        scm {
            url = "https://github.com/crowded-libs/duks-storage-lmdb/"
            connection = "scm:git:git://github.com/crowded-libs/duks-storage-lmdb.git"
            developerConnection = "scm:git:ssh://git@github.com/crowded-libs/duks-storage-lmdb.git"
        }
    }
}
