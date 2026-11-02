import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val firebaseConfig = Properties().apply {
    val f = rootProject.file("firebase.local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun config(name: String, fallback: String = "") = firebaseConfig.getProperty(name, fallback)
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
android {
    namespace = "cl.amigovirtual.app"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "cl.amigovirtual.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "environment"
    productFlavors {
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
            buildConfigField("boolean", "USE_EMULATORS", "true")
            buildConfigField("String", "FIREBASE_PROJECT", quoted("demo-viatech"))
            buildConfigField("String", "FIREBASE_API_KEY", quoted("demo-api-key"))
            buildConfigField("String", "FIREBASE_APP_ID", quoted("1:1234567890:android:viatech"))
            buildConfigField("String", "EMULATOR_HOST", quoted(config("emulator.host", "10.0.2.2")))
            manifestPlaceholders["cleartext"] = "true"
        }
        create("cloud") {
            dimension = "environment"
            buildConfigField("boolean", "USE_EMULATORS", "false")
            buildConfigField("String", "FIREBASE_PROJECT", quoted(config("firebase.projectId")))
            buildConfigField("String", "FIREBASE_API_KEY", quoted(config("firebase.apiKey")))
            buildConfigField("String", "FIREBASE_APP_ID", quoted(config("firebase.appId")))
            buildConfigField("String", "EMULATOR_HOST", quoted(""))
            manifestPlaceholders["cleartext"] = "false"
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
