plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

private fun Provider<String>.getInt() = get().toInt()

android {
    namespace = "com.github.damontecres.wholphin.baselineprofile"
    compileSdk {
        version = release(libs.versions.compileSdk.getInt())
    }

    defaultConfig {
        minSdk = 28
        targetSdk = libs.versions.targetSdk.getInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Only the default flavor is profiled, the app merges its profile into main for all of them
    flavorDimensions += "version"
    productFlavors {
        create("default") {
            dimension = "version"
        }
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

baselineProfile {
    // Generated on a connected Android TV, there is no TV emulator image that can be managed by Gradle
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
