plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Cada build do GitHub Actions gera uma versão nova (permite atualizar por cima)
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "br.com.skyfit.chamada"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.skyfit.chamada"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Chave fixa: sem ela, cada build teria assinatura diferente e a TV recusaria a atualização.
    // PRODUÇÃO: mover a chave e as senhas para GitHub Secrets.
    signingConfigs {
        create("skyfit") {
            storeFile = file("skyfit.keystore")
            storePassword = "skyfit123"
            keyAlias = "skyfit"
            keyPassword = "skyfit123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("skyfit")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sem dependências externas de propósito: app leve para TVs com pouca memória.
