plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlin.stdlib)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.register("buildNative") {
    doLast {
        try {
            exec {
                workingDir = projectDir
                commandLine("cargo", "build", "--release")
            }
        } catch (e: Exception) {
            println("Warning: Cargo not found. Skipping native build.")
            println("To build the native library, install Rust: https://rustup.rs/")
            println("Or run: curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh")
        }
    }
}

tasks.named("build") {
    dependsOn("buildNative")
}
