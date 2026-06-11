plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.sonarqube)
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

sonar {
    properties {
        property("sonar.projectKey", "monta-app_ocpp-emulator")
        property("sonar.projectName", "ocpp-emulator")
        property("sonar.host.url", "https://sonarqube.vpn.internal.monta.app/")
        // Only the `common` module has tests/coverage (kover is applied there).
        property("sonar.coverage.jacoco.xmlReportPaths", "common/build/reports/kover/report.xml")
        // Kotlin Multiplatform build (jvm() target only). The Sonar Gradle plugin does
        // not reliably auto-detect KMP source sets, so point it at the JVM source roots
        // explicitly. v16 is the Compose desktop app shell.
        property("sonar.sources", "common/src/jvmMain/kotlin,v16/src/jvmMain/kotlin")
        property("sonar.tests", "common/src/jvmTest/kotlin")
    }
}
