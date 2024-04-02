dependencies {

    if (rootProject.name == "servomaster") {
        implementation(project(":servomaster-common"))
    } else {
        implementation(project(":servomaster:servomaster-common"))
    }

    implementation(libs.usb4java)
}
