plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
        }
    }

    namespace = "com.example.proyecto_agenda"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.proyecto_agenda"
        minSdk = 24
        targetSdk = 36
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // NUEVAS DEPENDENCIAS

    // Navegación (Fundamental para el menú lateral e inferior)
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    // Room (Base de datos local)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // CAMBIO IMPORTANTE: de annotationProcessor a kapt
    kapt("androidx.room:room-compiler:$roomVersion")
    // Gson (Para convertir datos a JSON para Google Drive)
    implementation("com.google.code.gson:gson:2.10.1")
    // Testing (mantener)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //MAPS
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-ktx:3.4.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    // Google Sign-In y Drive API
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.http-client:google-http-client-gson:2.0.2")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")

    // ESTA FALTABA: Contiene AndroidHttp y GoogleAccountCredential
    implementation("com.google.api-client:google-api-client-android:2.8.1")

    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation("com.google.http-client:google-http-client-android:1.43.3")


}

