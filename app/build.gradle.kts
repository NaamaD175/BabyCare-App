plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.babycareapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.babycareapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Google maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    //Firebase BoM
    implementation(platform(libs.firebase.bom))

    //Firebase dependencies
    implementation(libs.firebase.analytics)

    //AuthUI
    implementation(libs.firebase.ui.auth)

    //Firebase storage
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.location)

    //Firebase realtime database
    implementation(libs.firebase.database)
}