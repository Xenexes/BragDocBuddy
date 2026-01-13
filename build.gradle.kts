plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    jacoco
    id("com.gradleup.shadow") version "9.3.0"
    id("org.graalvm.buildtools.native") version "0.11.3"
    `jvm-test-suite`
    idea
}

group = "de.xenexes"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-logger-slf4j:4.1.1")

    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.2")
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }

    sourceSets {
        main {
            kotlin.srcDir("build/generated/sources/version/kotlin")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register<GenerateVersionTask>("generateVersionPropertiesAndConstant") {
    group = "build"
    description = "Generates version.properties file and Version.kt constant"

    version.set(project.version.toString())

    resourcesOutputDir.set(
        layout.buildDirectory.dir("resources/main"),
    )

    kotlinOutputDir.set(
        layout.buildDirectory.dir("generated/sources/version/kotlin/infrastructure/version"),
    )
}

tasks.named("compileKotlin") {
    dependsOn("generateVersionPropertiesAndConstant")
}

tasks.named("processResources") {
    dependsOn("generateVersionPropertiesAndConstant")
}

tasks.named("nativeCompile") {
    dependsOn("generateVersionPropertiesAndConstant")
}

tasks.matching { it.name.startsWith("runKtlint") || it.name == "ktlintFormat" }.configureEach {
    dependsOn("generateVersionPropertiesAndConstant")
}

ktlint {
    filter {
        exclude("**/generated/**")
        exclude("build/generated/**")
    }
    version.set("1.7.1")
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)

        register<JvmTestSuite>("konsistTest") {
            dependencies {
                implementation(project(":"))
                implementation("com.lemonappdev:konsist:0.17.3")
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("konsistTest"))
}

tasks.named("konsistTest") {
    mustRunAfter("shadowJar")
}

tasks.shadowJar {
    archiveBaseName.set("BragDocBuddy")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes(
            "Main-Class" to "MainKt",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString(),
        )
    }
    mergeServiceFiles()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        csv.required = false
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("BragDocBuddy")
            mainClass.set("MainKt")

            buildArgs.addAll(
                "--no-fallback",
                "--install-exit-handlers",
                "--initialize-at-build-time=kotlin.DeprecationLevel",
                "-H:ReflectionConfigurationFiles=$projectDir/src/main/resources/reflection-config.json",
            )
        }
    }
}

tasks.register("dist") {
    group = "distribution"
    description = "Creates all distribution artifacts (fat jar and native binary)"
    dependsOn("shadowJar", "nativeCompile")
}

abstract class GenerateVersionTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val resourcesOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val kotlinOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        // version.properties
        val resourcesDir = resourcesOutputDir.get().asFile
        resourcesDir.mkdirs()
        File(resourcesDir, "version.properties")
            .writeText("version=${version.get()}\n")

        // Version.kt
        val kotlinDir = kotlinOutputDir.get().asFile
        kotlinDir.mkdirs()
        File(kotlinDir, "Version.kt").writeText(
            """
            package infrastructure.version

            object Version {
                const val VERSION = "${version.get()}"
            }
            """.trimIndent() + "\n",
        )
    }
}
