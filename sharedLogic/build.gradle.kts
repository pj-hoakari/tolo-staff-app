import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
    alias(libs.plugins.kotlinxRpc)
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            binaryOption("bundleId", "dev.usbharu.tolostaff.SharedLogic")
        }
    }
    
    androidLibrary {
       namespace = "dev.usbharu.tolo_staff.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.android.bom.get()))
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.grpc.protobuf.lite)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.rpc.grpc.core)
            api(libs.kotlinx.rpc.protobuf.core)
            implementation(libs.kotlinx.rpc.grpc.client)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.grpc.netty)
        }
    }
}

rpc {
    protoc()
}


configurations.configureEach {
    if (
        name == "androidCompileClasspath" ||
        name == "androidRuntimeClasspath" ||
        name.endsWith("AndroidCompileClasspath") ||
        name.endsWith("AndroidRuntimeClasspath")
    ) {
        exclude(
            group = "com.google.api.grpc",
            module = "proto-google-common-protos"
        )
    }
}