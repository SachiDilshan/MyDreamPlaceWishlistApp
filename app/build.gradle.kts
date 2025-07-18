plugins {
    alias(libs.plugins.android.application)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.s92086882.mydreamplacewishlist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.s92086882.mydreamplacewishlist"
        minSdk = 24
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.auth)
    implementation (libs.play.services.location)
    implementation (libs.places)

    implementation(libs.firebase.auth)
    implementation(libs.google.firebase.auth)
    implementation(libs.firebase.bom)
    implementation (libs.firebase.firestore)
    implementation (libs.firebase.storage)
    implementation (libs.google.firebase.storage)

    implementation (libs.cardview)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    implementation(libs.photoview)


    implementation("com.github.xabaras:RecyclerViewSwipeDecorator:1.3")

}

apply(plugin = "com.google.gms.google-services")