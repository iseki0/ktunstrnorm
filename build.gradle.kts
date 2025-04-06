import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.*

plugins {
    kotlin("multiplatform") version "2.1.20"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
    `maven-publish`
    signing
}

allprojects {
    group = "space.iseki.ktunstrnorm"
    version = "0.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

}

dependencies {
    commonTestImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    targets {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
        jvm {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_1_8
                freeCompilerArgs.add("-Xjvm-default=all-compatibility")
            }
        }
        js {
            browser()
            nodejs()
        }
        wasmJs {
            browser()
            nodejs()
        }
//        wasmWasi {
//            nodejs()
//        }

        // Tier 1
        macosX64()
        macosArm64()
        iosSimulatorArm64()
        iosX64()
        iosArm64()

        // Tier 2
        linuxX64()
        linuxArm64()
        watchosArm32()
        watchosArm64()
        watchosX64()
        watchosSimulatorArm64()
        tvosSimulatorArm64()
        tvosX64()
        tvosArm64()

        // Tier 3
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX64()
        androidNativeX86()
        mingwX64()
        watchosDeviceArm64()
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val apple64Main by creating { dependsOn(commonMain.get()) }
        fun NamedDomainObjectProvider<KotlinSourceSet>.apple64() {
            get().dependsOn(apple64Main)
        }

        val apple32Main by creating { dependsOn(commonMain.get()) }
        fun NamedDomainObjectProvider<KotlinSourceSet>.apple32() {
            get().dependsOn(apple32Main)
        }

        val linux64Main by creating { dependsOn(commonMain.get()) }
        fun NamedDomainObjectProvider<KotlinSourceSet>.linux64() {
            get().dependsOn(linux64Main)
        }

        //        val linux32Main by creating { dependsOn(commonMain.get()) }
        fun NamedDomainObjectProvider<KotlinSourceSet>.linux32() {
//            get().dependsOn(linux32Main)
            linux64()
        }


        // Tier 1
        macosX64Main.apple64()
        macosArm64Main.apple64()
        iosSimulatorArm64Main.apple64()
        iosX64Main.apple64()
        iosArm64Main.apple64()

        // Tier 2
        linuxX64Main.linux64()
        linuxArm64Main.linux64()
        watchosArm32Main.apple32()
        watchosArm64Main.apple32()
        watchosX64Main.apple64()
        watchosSimulatorArm64Main.apple64()
        tvosSimulatorArm64Main.apple64()
        tvosX64Main.apple64()
        tvosArm64Main.apple64()

        // Tier 3
        androidNativeArm32Main.linux32()
        androidNativeArm64Main.linux64()
        androidNativeX64Main.linux32()
        androidNativeX86Main.linux32()
        mingwX64Main
        watchosDeviceArm64Main.apple64()

    }
}

tasks.getByName("jvmJar") {
    check(this is Jar)
    manifest {
        attributes("Automatic-Module-Name" to "space.iseki.ktunstrnorm")
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

publishing {
    repositories {
        maven {
            name = "Central"
            afterEvaluate {
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    // uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                    uri("https://oss.sonatype.org/content/repositories/snapshots")
                } else {
                    // uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                    uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                }
            }
            credentials {
                username = properties["ossrhUsername"]?.toString() ?: System.getenv("OSSRH_USERNAME")
                password = properties["ossrhPassword"]?.toString() ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
    publications {
        withType<MavenPublication> {
            val pubName = name.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val emptyJavadocJar by tasks.register<Jar>("emptyJavadocJar$pubName") {
                archiveClassifier = "javadoc"
                archiveBaseName = artifactId
            }
            artifact(emptyJavadocJar)
            pom {
                val projectUrl = "https://github.com/iseki0/ktunstrnorm"
                description = "A very small library provide Unicode normalization for Kotlin Multiplatform"
                url = projectUrl
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "iseki0"
                        name = "iseki zero"
                        email = "iseki@iseki.space"
                    }
                }
                inceptionYear = "2025"
                scm {
                    connection = "scm:git:$projectUrl.git"
                    developerConnection = "scm:git:$projectUrl.git"
                    url = projectUrl
                }
                issueManagement {
                    system = "GitHub"
                    url = "$projectUrl/issues"
                }
                ciManagement {
                    system = "GitHub"
                    url = "$projectUrl/actions"
                }
            }
        }
    }
}