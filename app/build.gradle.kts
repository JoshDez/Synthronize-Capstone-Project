import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.synthronize"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.synthronize"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Other Dependencies
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.orhanobut:dialogplus:1.11@aar")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")
    implementation("me.relex:circleindicator:2.1.6")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("me.relex:circleindicator:2.1.6")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    kapt("com.github.bumptech.glide:compiler:4.16.0")




}