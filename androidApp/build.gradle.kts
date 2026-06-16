import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(projects.sharedLogic) {
        exclude(group = "io.grpc", module = "grpc-protobuf-lite")
    }

    implementation(libs.androidx.activity.compose)
    implementation(libs.grpc.okhttp)

    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
    implementation(compose.materialIconsExtended)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "dev.usbharu.tolo_staff"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.usbharu.tolo_staff"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
