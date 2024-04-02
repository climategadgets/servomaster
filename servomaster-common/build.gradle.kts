plugins {
    `java-library`
}

dependencies {
    api(libs.log4j.api)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)

    testRuntimeOnly(rootProject.libs.junit5.engine)
}
