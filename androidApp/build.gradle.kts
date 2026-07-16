import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun org.gradle.api.Project.stringProperty(name: String, default: String): String =
    providers.gradleProperty(name).orElse(default).get()

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
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.grpc.okhttp)
    implementation(libs.koin.core)

    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.appauth.android)
    implementation(compose.materialIconsExtended)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.uiTestManifest)
    implementation(libs.grpc.protobuf.lite)

    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTestJunit4)
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TOLO_TENANT_ID", "\"${project.stringProperty("tolo.tenantId", "tenant-dev")}\"")
        buildConfigField("String", "TOLO_EVENT_ID", "\"${project.stringProperty("tolo.eventId", "event-dev")}\"")
        buildConfigField("String", "TOLO_STAFF_ID", "\"${project.stringProperty("tolo.staffId", "tanaka")}\"")
        buildConfigField("String", "TOLO_GRPC_HOST", "\"${project.stringProperty("tolo.grpcHost", "10.0.2.2")}\"")
        buildConfigField("int", "TOLO_GRPC_PORT", project.stringProperty("tolo.grpcPort", "8080"))
        buildConfigField("boolean", "TOLO_FIRESTORE_ENABLED", project.stringProperty("tolo.firestoreEnabled", "true"))
        buildConfigField("String", "TOLO_FIRESTORE_HOST", "\"${project.stringProperty("tolo.firestoreHost", "10.0.2.2")}\"")
        buildConfigField("int", "TOLO_FIRESTORE_PORT", project.stringProperty("tolo.firestorePort", "8081"))
        buildConfigField("String", "TOLO_FIRESTORE_PROJECT_ID", "\"${project.stringProperty("tolo.firestoreProjectId", "tolo-communication-poc")}\"")
        buildConfigField("String", "TOLO_FIRESTORE_DATABASE_ID", "\"${project.stringProperty("tolo.firestoreDatabaseId", "")}\"")
        buildConfigField("String", "TOLO_FIREBASE_APPLICATION_ID", "\"${project.stringProperty("tolo.firebaseApplicationId", "1:1234567890:android:tolostaff")}\"")
        buildConfigField("String", "TOLO_FIREBASE_API_KEY", "\"${project.stringProperty("tolo.firebaseApiKey", "fake-api-key")}\"")
        buildConfigField("String", "TOLO_FIREBASE_GCM_SENDER_ID", "\"${project.stringProperty("tolo.firebaseGcmSenderId", "1234567890")}\"")
        buildConfigField("String", "TOLO_FIREBASE_AUTH_HOST", "\"${project.stringProperty("tolo.firebaseAuthHost", "10.0.2.2")}\"")
        buildConfigField("int", "TOLO_FIREBASE_AUTH_PORT", project.stringProperty("tolo.firebaseAuthPort", "9099"))
        buildConfigField("boolean", "TOLO_OIDC_ENABLED", project.stringProperty("tolo.oidc.enabled", "false"))
        buildConfigField("String", "TOLO_OIDC_ISSUER", "\"${project.stringProperty("tolo.oidc.issuer", "http://10.0.2.2:8080")}\"")
        buildConfigField("String", "TOLO_OIDC_CLIENT_ID", "\"${project.stringProperty("tolo.oidc.clientId", "tolo-staff-mobile")}\"")
        buildConfigField("String", "TOLO_OIDC_REDIRECT_URI", "\"${project.stringProperty("tolo.oidc.redirectUri", "dev.usbharu.tolostaff:/oauth2redirect")}\"")
        buildConfigField("String", "TOLO_OIDC_SCOPES", "\"${project.stringProperty("tolo.oidc.scopes", "openid profile offline_access tenant.read tenant.write events.read events.write")}\"")
        buildConfigField("String", "TOLO_OIDC_AUDIENCE", "\"${project.stringProperty("tolo.oidc.audience", "backend-api")}\"")
        buildConfigField("String", "TOLO_OIDC_RESOURCE_BASE_URL", "\"${project.stringProperty("tolo.oidc.resourceBaseUrl", "https://api.example.com")}\"")
        manifestPlaceholders["appAuthRedirectScheme"] = project.stringProperty("tolo.oidc.redirectScheme", "dev.usbharu.tolostaff")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "TOLO_FIRESTORE_EMULATOR_ENABLED", project.stringProperty("tolo.firestoreEmulatorEnabled", "true"))
            buildConfigField("boolean", "TOLO_FIREBASE_AUTH_EMULATOR_ENABLED", project.stringProperty("tolo.firebaseAuthEmulatorEnabled", "true"))
            buildConfigField(
                "String",
                "TOLO_OIDC_DEBUG_ORIGIN_OVERRIDE",
                "\"${project.stringProperty("tolo.oidc.debugOriginOverride", "http://10.0.2.2:8080")}\"",
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("boolean", "TOLO_FIRESTORE_EMULATOR_ENABLED", "false")
            buildConfigField("boolean", "TOLO_FIREBASE_AUTH_EMULATOR_ENABLED", "false")
            buildConfigField("String", "TOLO_OIDC_DEBUG_ORIGIN_OVERRIDE", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations.configureEach {
    if (
        name == "debugRuntimeClasspath" ||
        name == "releaseRuntimeClasspath" ||
        name == "debugCompileClasspath" ||
        name == "releaseCompileClasspath"
    ) {
        exclude(
            group = "com.google.api.grpc",
            module = "proto-google-common-protos"
        )
        exclude(
            group = "com.google.protobuf",
            module = "protobuf-java"
        )
    }
}
