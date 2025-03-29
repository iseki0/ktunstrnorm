import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform") version "2.1.20"
}

group = "space.iseki.ktunstrnorm"
version = "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
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
//                jvmTarget = JvmTarget.JVM_1_8
                freeCompilerArgs.add("-Xjvm-default=all-compatibility")
            }
        }
//        js {
//            browser()
//            nodejs()
//        }
//        wasmJs {
//            browser()
//            nodejs()
//        }
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
//        linuxX64()
//        linuxArm64()
        watchosArm32()
        watchosArm64()
        watchosX64()
        watchosSimulatorArm64()
        tvosSimulatorArm64()
        tvosX64()
        tvosArm64()

        // Tier 3
//        androidNativeArm32()
//        androidNativeArm64()
//        androidNativeX64()
//        androidNativeX86()
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

        val linux32Main by creating { dependsOn(commonMain.get()) }
        fun NamedDomainObjectProvider<KotlinSourceSet>.linux32() {
            get().dependsOn(linux32Main)
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
