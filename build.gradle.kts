repositories {
    mavenCentral()
}

plugins {
    application
    java
    jacoco
}

application {
    mainClassName = "main.SokobanApplication"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    val junit = "5.3.2"
    val junitPlatform = "1.3.2"
    val testfx = "4.0.15-alpha"

    compile("org.jetbrains:annotations:16.0.3")

    testCompile("org.junit.jupiter:junit-jupiter-api:$junit")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junit")
    testCompile("org.junit.platform:junit-platform-runner:$junitPlatform")
    testCompile("org.testfx:testfx-core:$testfx")
    testCompile("org.testfx:testfx-junit5:$testfx")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junit")
    testRuntime("org.junit.platform:junit-platform-console:$junitPlatform")
}

sourceSets {
    getByName("test") {
        resources {
            srcDirs.add(File("src/main/resources"))
        }
    }
}

tasks {
    getByName<Test>("test") {
        useJUnitPlatform()
    }

    getByName<Wrapper>("wrapper") {
        gradleVersion = "5.0"
        distributionType = Wrapper.DistributionType.ALL
    }

    getByName<Javadoc>("javadoc") {
        setDestinationDir(File("$projectDir/docs"))
    }

    getByName<JacocoReport>("jacocoTestReport") {
        reports {
            xml.isEnabled = false
            csv.isEnabled = false
            html.destination = File("$buildDir/jacocoHTML")
        }
    }
}
