plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    api(libs.kotlinjson)
    api(libs.kotlinx.coroutines.core)
    api(libs.jsoup)
    api(libs.okhttp)
    api(libs.chicory.runtime)
    api(libs.quickjs.jvm)

    testImplementation(libs.junit)
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.2.10")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.halovoid.bunori"
            artifactId = "extension-api"
            version = "1.0.0"
        }
    }
}


