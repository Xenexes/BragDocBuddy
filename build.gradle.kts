plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    jacoco
    id("com.gradleup.shadow") version "9.2.2"
    id("org.graalvm.buildtools.native") version "0.11.3"
    `jvm-test-suite`
}

group = "de.xenexes"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-logger-slf4j:4.1.1")

    implementation("io.ktor:ktor-client-core:3.3.2")
    implementation("io.ktor:ktor-client-cio:3.3.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.1")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)

        register<JvmTestSuite>("konsistTest") {
            dependencies {
                implementation(project())
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

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register("generateVersionProperties") {
    group = "build"
    description = "Generates version.properties file with current version"

    val outputDir =
        layout.buildDirectory
            .dir("resources/main")
            .get()
            .asFile
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        val versionFile = File(outputDir, "version.properties")
        versionFile.writeText("version=${project.version}\n")
        println("Generated version.properties with version: ${project.version}")
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}

ktlint {
    filter {
        exclude("**/generated/**")
    }
    version.set("1.7.1")
}

tasks.shadowJar {
    archiveBaseName.set("BragDocBuddy")
    archiveClassifier.set("")
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
            )
        }
    }
}

tasks.register("dist") {
    group = "distribution"
    description = "Creates all distribution artifacts (fat jar and native binary)"
    dependsOn("shadowJar", "nativeCompile")
}
