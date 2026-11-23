import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------------------
// Firma release.
// Los datos de la clave NUNCA van en el código. Se leen de:
//   1) el archivo keystore.properties (en la raíz, ignorado por git), o
//   2) variables de entorno (las usa GitHub Actions).
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun firma(propKey: String, envKey: String): String? =
    (keystoreProps.getProperty(propKey) ?: System.getenv(envKey))?.takeIf { it.isNotBlank() }

val storePath = firma("storeFile", "RADIO_KEYSTORE_FILE")
val storePass = firma("storePassword", "RADIO_KEYSTORE_PASSWORD")
val keyAliasV = firma("keyAlias", "RADIO_KEY_ALIAS")
val keyPass = firma("keyPassword", "RADIO_KEY_PASSWORD")
val hayClave = storePath != null && storePass != null && keyAliasV != null && keyPass != null

android {
    namespace = "cu.radioenciclopedia.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cu.radioenciclopedia.app"
        minSdk = 28            // Android 9.0
        targetSdk = 35
        versionCode = 21
        versionName = "2.3.0"
    }

    signingConfigs {
        if (hayClave) {
            create("release") {
                storeFile = rootProject.file(storePath!!)
                storePassword = storePass
                keyAlias = keyAliasV
                keyPassword = keyPass
                enableV1Signing = true   // inofensivo; minSdk 28 usa v2/v3
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hayClave) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Sin bloque de metadatos de dependencias dentro del APK (más pequeño y más privado)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // APK universal: no se configuran splits ni filtros de ABI (la app no tiene código nativo).
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

// La compilación release NO puede producir un APK sin firmar ni firmarlo con la clave de depuración.
gradle.taskGraph.whenReady {
    val pideRelease = allTasks.any { it.path == ":app:assembleRelease" || it.path == ":app:packageRelease" }
    if (pideRelease && !hayClave) {
        throw GradleException(
            "Falta la clave de firma release. Crea una con tools/crear_clave.sh y define " +
                "keystore.properties (storeFile, storePassword, keyAlias, keyPassword) o las variables " +
                "RADIO_KEYSTORE_FILE, RADIO_KEYSTORE_PASSWORD, RADIO_KEY_ALIAS y RADIO_KEY_PASSWORD. " +
                "Consulta el README."
        )
    }
}

// Sin dependencias externas: solo la plataforma Android y la biblioteca estándar de Kotlin
// (que el plugin de Kotlin añade solo). No usa AndroidX ni Google Play Services.
