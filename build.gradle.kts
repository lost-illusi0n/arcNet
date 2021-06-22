import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("edu.sc.seis.launch4j") version "2.4.6"
    id("org.openjfx.javafxplugin") version "0.0.7"
}

group = "com.azedevs"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.jfrog.org/artifactory/libs-release")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("https://jitpack.io")
}

val lwjglVersion = "3.2.2-SNAPSHOT"
val lwjglNatives = "natives-windows"

dependencies {
    implementation(libs.bundles.core)
    implementation(libs.bundles.memscan)
    implementation(libs.bundles.twitch)
    implementation(libs.bundles.database)
    implementation(libs.bundles.gui)
}

launch4j {
    mainClassName = "MainKt"
    icon = "${projectDir}/resources/ic_gearnet.ico"
}

javafx {
    version = "11"
    modules = listOf("javafx.controls", "javafx.graphics")
}

val fatJar = task("fatJar", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // bug in intellij shows a false-positive error as it thinks we would be setting the underlying field. we are not
    // and as a workaround we just use #set to get around that error.
    // archiveBaseName = "${project.name}-fat"
    archiveBaseName.set("${project.name}-fat")


    manifest {
        attributes("Implementation-Title" to "GearNet Xrd",
            "Implementation-Version" to archiveVersion,
            "Main-Class" to "MainKt")
    }

    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map{ zipTree(it) })
    with(tasks["jar"] as CopySpec)
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "10"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
