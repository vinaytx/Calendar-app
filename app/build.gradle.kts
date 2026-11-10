import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

android {
    namespace = "com.groupcalendar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.groupcalendar.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Externalized credentials injected into BuildConfig
        val defaultBotToken = localProperties.getProperty("TELEGRAM_BOT_TOKEN") ?: ""
        val defaultChatId = localProperties.getProperty("TELEGRAM_DEFAULT_CHAT_ID") ?: ""
        val defaultInviteLink = localProperties.getProperty("TELEGRAM_GROUP_INVITE_LINK") ?: ""

        buildConfigField("String", "DEFAULT_BOT_TOKEN", "\"$defaultBotToken\"")
        buildConfigField("String", "DEFAULT_CHAT_ID", "\"$defaultChatId\"")
        buildConfigField("String", "DEFAULT_INVITE_LINK", "\"$defaultInviteLink\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("RELEASE_KEYSTORE_PATH")
            val keystorePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
            val keyAliasName = localProperties.getProperty("RELEASE_KEY_ALIAS")
            val keyPass = localProperties.getProperty("RELEASE_KEY_PASSWORD")

            if (keystorePath != null && keystorePassword != null && keyAliasName != null && keyPass != null) {
                storeFile = rootProject.file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Networking & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
