plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    application
    id("com.gradleup.shadow") version "9.2.2"
    id("org.graalvm.buildtools.native") version "0.11.2"
}

group = "de.xenexes"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-logger-slf4j:4.1.1")

    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")
    testImplementation("com.lemonappdev:konsist:0.17.3")

    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.1")
    testImplementation("io.mockk:mockk:1.14.6")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

ktlint {
    filter {
        exclude("**/generated/**")
    }
    version.set("1.7.1")
}

application {
    mainClass.set("MainKt")
    applicationName = "BragLogBuddy"
}

tasks.shadowJar {
    archiveBaseName.set("BragLogBuddy")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes(
            "Main-Class" to "MainKt",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString()
        )
    }
    mergeServiceFiles()
}

tasks.startScripts {
    applicationName = "BragLogBuddy"
}

// Native image configuration - may have path issues on Windows
// Use GitHub Actions for reliable native image builds
graalvmNative {
    binaries {
        named("main") {
            imageName.set("BragLogBuddy")
            mainClass.set("MainKt")
            buildArgs.addAll(
                "--no-fallback",
                "--install-exit-handlers",
            )
        }
    }
}

tasks.register<Exec>("createInstaller") {
    group = "distribution"
    description = "Creates a platform-specific installer using jpackage"
    dependsOn("shadowJar")

    doFirst {
        val distDir = File(project.projectDir, "dist")
        distDir.mkdirs()

        val os = System.getProperty("os.name").lowercase()
        val installerType =
            when {
                os.contains("windows") -> "exe"
                os.contains("mac") -> "pkg"
                else -> "deb"
            }

        commandLine(
            "jpackage",
            "--input",
            "build/libs",
            "--name",
            "BragLogBuddy",
            "--main-jar",
            "BragLogBuddy-${project.version}.jar",
            "--main-class",
            "MainKt",
            "--dest",
            "dist",
            "--type",
            installerType,
            "--app-version",
            project.version.toString(),
            "--description",
            "A command line tool for journaling daily accomplishments",
            "--vendor",
            "BragLogBuddy",
            "--copyright",
            "2025 BragLogBuddy",
        )
    }

    doLast {
        println("Installer created in dist/ directory")
    }
}

tasks.register("dist") {
    group = "distribution"
    description = "Creates all distribution artifacts"
    dependsOn("shadowJar", "nativeCompile", "createInstaller")
}
