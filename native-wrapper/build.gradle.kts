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
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.register("buildNative") {
    dependsOn(":native:buildNative")
    doLast {
        val nativeDir = project(":native").projectDir.resolve("target/release")
        if (nativeDir.exists()) {
            // Copy native library to resources
            copy {
                from(nativeDir)
                into("${project.layout.buildDirectory.get()}/resources/main")
                include("*.dylib", "*.so", "*.dll")
            }
        } else {
            println("Warning: Native library directory not found. Skipping copy.")
            println("Install Rust and build the native library first.")
        }
    }
}

tasks.named("processResources") {
    dependsOn("buildNative")
}
