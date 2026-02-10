@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import net.kyori.indra.git.IndraGitExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.time.Instant

plugins {
    alias(libs.plugins.indra.git)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jib)
}

val javaTarget = 25
val entryPoint = "io.ktor.server.netty.EngineMain"

java {
    val targetVersion = JavaVersion.toVersion(javaTarget)
    sourceCompatibility = targetVersion
    targetCompatibility = targetVersion
    if (JavaVersion.current() < targetVersion) {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaTarget)) }
        kotlin.jvmToolchain(javaTarget)
    }
}

repositories {
    mavenCentral()
}

spotless {
    fun com.diffplug.gradle.spotless.FormatExtension.setup() {
        endWithNewline()
        trimTrailingWhitespace()
    }
    kotlin {
        setup()
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        setup()
        ktlint(libs.versions.ktlint.get())
    }
}

kotlin {
    explicitApi()

    jvm {
        mainRun {
            mainClass = entryPoint
        }
        compilerOptions {
            jvmTarget =
                org.jetbrains.kotlin.gradle.dsl.JvmTarget
                    .fromTarget("$javaTarget")
            freeCompilerArgs.add("-Xjdk-release=$javaTarget")
        }
    }

    js {
        browser {
        }
        compilerOptions {
            target = "es2015"
        }
        binaries.executable()
    }

    sourceSets {
        configureEach {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }

        named("commonMain") {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.html)
                implementation(libs.ktor.openapi.schema)
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(libs.bundles.ktor.server)
                implementation(libs.bundles.ktor.client)
                implementation(libs.ktor.network)

                implementation(libs.adventure.minimessage)
                implementation(libs.adventure.text.serializer.gson)
                implementation(libs.adventure.text.serializer.legacy)
                implementation(libs.cache4k)
                implementation(libs.logback.classic)
            }
        }
    }
}

jib {
    from {
        image = "azul/zulu-openjdk-alpine:$javaTarget-jre"
        platforms {
            // We can only build multi-arch images when pushing to a registry, not when building locally
            val requestedTasks = gradle.startParameter.taskNames
            if ("jibBuildTar" in requestedTasks || "jibDockerBuild" in requestedTasks) {
                platform {
                    // todo: better logic
                    architecture =
                        when (System.getProperty("os.arch")) {
                            "aarch64" -> "arm64"
                            else -> "amd64"
                        }
                    os = "linux"
                }
            } else {
                platform {
                    architecture = "amd64"
                    os = "linux"
                }
                platform {
                    architecture = "arm64"
                    os = "linux"
                }
            }
        }
        configurationName = "jvmRuntimeClasspath"
    }
    container {
        mainClass = entryPoint
        labels.put(
            "org.opencontainers.image.description",
            indraGit.commit().map { it.name }.orElse("<unknown>").map { commit ->
                """A Web UI for working with Adventure components.
            Built with Adventure ${libs.versions.adventure.get()}, from webui commit $commit"""
            },
        )
        jvmFlags = listOf("--enable-native-access=ALL-UNNAMED")
    }
    to {
        image = "ghcr.io/papermc/adventure-webui/webui"
        tags =
            setOf(
                "latest",
                "${indraGit.branchName().getOrElse(
                    "nogit",
                ).replace("/", "_")}-${indraGit.commit().orNull?.name()?.take(7)}-${Instant.now().epochSecond}",
            )
    }
}

sourceSets {
    // jib is stupid and inflexible
    register("main") {
        val jvmMain = getByName("jvmMain")
        (output.classesDirs as ConfigurableFileCollection).from(jvmMain.output.classesDirs)
        jvmMain.output.resourcesDir?.let { output.setResourcesDir(it) }
    }
}

tasks {
    val webpackTask =
        if (isDevelopment()) {
            "jsBrowserDevelopmentWebpack"
        } else {
            "jsBrowserProductionWebpack"
        }.let { taskName ->
            named<KotlinWebpack>(taskName)
        }

    named<Jar>("jvmJar") {
        rootProject.indraGit.applyVcsInformationToManifest(manifest)
    }

    named<AbstractCopyTask>("jvmProcessResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(webpackTask.flatMap { it.mainOutputFile }) {
            into("web/js")
            include("*.js", "*.js.map")
        }

        val configProperties = objects.mapProperty(String::class, String::class)
        configProperties.put(
            "miniMessageVersion",
            libs.adventure.minimessage.map { it.versionConstraint.requiredVersion },
        )
        configProperties.put(
            "commitHash",
            rootProject.extensions
                .getByType<IndraGitExtension>()
                .commit()
                .map { it.name }
                .orElse(""),
        )
        inputs.property("configProperties", configProperties)

        doFirst {
            filesMatching("application.conf") {
                expand(configProperties.get())
            }
        }
    }

    // the kotlin plugin creates this task super late for some reason?
    configureEach {
        if (name == "jvmRun" && isDevelopment()) {
            (this as JavaExec).jvmArgs("-Dio.ktor.development=true", "-DwebuiLogLevel=trace")
        }
    }
}

/** Checks if the development property is set. */
fun isDevelopment(): Boolean = project.hasProperty("isDevelopment")
