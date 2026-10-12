plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dailybudget.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dailybudget.app"
        minSdk = 26
        targetSdk = 35
        // La CI fournit le numéro de build pour que chaque APK se mette à jour par-dessus le précédent.
        val build = (project.findProperty("buildNumber") as String?)?.toInt() ?: 1
        versionCode = build
        versionName = "1.0.$build"
    }

    signingConfigs {
        // Clé fixe versionnée : chaque APK construit peut être installé par-dessus le précédent.
        create("shared") {
            storeFile = file("dailybudget.keystore")
            storePassword = "dailybudget"
            keyAlias = "dailybudget"
            keyPassword = "dailybudget"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("shared")
        }
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Les captures d'écran (README) sont générées par ScreenshotTest via Robolectric.
            all { it.systemProperty("roborazzi.test.record", "true") }
        }
    }
    androidResources {
        // Permet de choisir la langue de l'app dans les réglages Android (13+).
        generateLocaleConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.40.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.40.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
