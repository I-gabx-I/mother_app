plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android.plugin)
}

android {
    namespace = "gt.marcos.joyeria"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "gt.marcos.joyeria"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    // MigrationTestHelper (D-033) necesita el schema "de origen"
    // (app/schemas/.../1.json) como asset legible desde el test.
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// room-testing-android:2.8.5 declara kotlinx-serialization-json:1.8.1 (que
// pide kotlinx-serialization-core:1.8.1) para leer app/schemas/*.json en
// MigrationTestHelper -- pero el propio metadata de módulo de Room 2.8.5
// (room-runtime) publica una restricción `strictly 1.7.3` sobre
// kotlinx-serialization-core que pisa ese pedido y deja una versión que no
// tiene el método que room-testing necesita en tiempo de ejecución
// (AbstractMethodError: GeneratedSerializer.typeParametersSerializers()).
// Es una inconsistencia real dentro del propio Room 2.8.5, verificada
// leyendo el árbol de dependencias resuelto (`:app:dependencies
// --configuration debugAndroidTestRuntimeClasspath`), no una suposición.
// Se fuerza acá, solo en las configuraciones de `androidTest` (nunca se
// empaqueta en la app en sí), a la versión que room-testing-android ya
// pide en su propio POM -- D-034.
configurations.matching { it.name.contains("AndroidTest", ignoreCase = false) }.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
            "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
        )
    }
}

// Robolectric (usado solo en tests de data/, D-012) hace reflexión profunda
// sobre internals de la JDK que el module system de JDK 17+ bloquea por
// defecto. Sin esto, cualquier test con @RunWith(AndroidJUnit4::class)
// respaldado por Robolectric falla con IllegalAccessException antes de
// llegar al cuerpo del test. No es una dependencia nueva, es una bandera de
// la JVM que corre los tests.
tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}