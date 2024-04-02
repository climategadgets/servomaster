plugins {
    java
    jacoco
    `maven-publish`
    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube)

    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.gradle.dependency.analysis)
    alias(libs.plugins.gradle.doctor)
}

sonarqube {
    properties {
        property("sonar.projectKey", "climategadgets_servomaster")
        property("sonar.organization", "climategadgets")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

subprojects {

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = rootProject.libs.plugins.errorprone.get().pluginId)

    group = "net.sf.servomaster"
    version = "3.0.1-SNAPSHOT"

    java {

        withSourcesJar()
        // VT: FIXME: Disabled for now; too verbose on obsolete classes
        // withJavadocJar()
    }

    tasks.compileJava {
        options.release = 11
    }

    tasks.withType<Javadoc> {
        isFailOnError = false
    }

    jacoco {
        toolVersion = rootProject.libs.versions.jacoco.get()
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

// VT: FIXME: How does this work with Kotlin DSL?
//    artifacts {
//        archives javadocJar, sourcesJar
//    }

    repositories {
        mavenCentral()
    }

    dependencies {
        errorprone(rootProject.libs.errorprone)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
