plugins {
    java
    application
    id("io.freefair.lombok") version "8.4"
    id("me.champeau.jmh") version "0.7.2"
}

group "org.example"
version "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Lombok
    implementation("io.freefair.gradle:lombok-plugin:8.4")
    compileOnly ("org.projectlombok:lombok:1.18.36")
    annotationProcessor ("org.projectlombok:lombok:1.18.36")

    testCompileOnly ("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.36")

    // JMH
    implementation ("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor ("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

application {
    mainClass.set("org.example.Main")
}




