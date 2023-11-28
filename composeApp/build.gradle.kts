import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        desktopTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter:5.10.1")
            implementation("com.willowtreeapps.assertk:assertk:0.27.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation("ch.qos.logback:logback-classic:1.4.12")
            implementation("org.slf4j:slf4j-api:2.0.9")
            implementation("com.aallam.openai:openai-client:3.6.1")
            implementation("com.aallam.ktoken:ktoken:0.3.0")
            implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")

            val ktorVersion = "2.3.6"
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-cio:$ktorVersion")
            implementation("io.ktor:ktor-client-serialization:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
            implementation("io.ktor:ktor-client-logging:$ktorVersion")
        }
    }
}

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "meetnote.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            modules("java.naming")
            modules("java.instrument")
            modules("java.management")
            modules("java.sql")
            modules("jdk.unsupported")

            packageName = "MeetNote"
            packageVersion = (findProperty("release") ?: "1.0.0").toString()

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
        }
    }
}
