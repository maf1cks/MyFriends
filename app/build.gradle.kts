plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.map1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.map1"
        minSdk = 29
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))
    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))

    // Declare the dependency for the Cloud Firestore library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.firestore)
    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.google.firebase.auth)
    implementation(libs.maps.mobile)
    // Also add the dependency for the Google Play services library and specify its version
    implementation(libs.play.services.auth)
    implementation(libs.picasso)
    implementation(libs.firebase.storage)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.database)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.okhttp)
    implementation(libs.maps.mobile.v4141lite)
    implementation(libs.maps.mobile.v4150full)

    implementation(libs.constraintlayout.v201)
    implementation(libs.preference)
    implementation(libs.gms.play.services.location)
    implementation(libs.concurrent.futures)

    implementation(libs.guava)
    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}