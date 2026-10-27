import org.gradle.api.file.FileTreeElement

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.explorationcore"
version = "1.3.0-dev.1"

// Lowothra's exact Paper build is not recorded. 1.21.11 is the newest Paper API
// this machine's JDK 21 can compile. plugin.yml api-version stays 1.21 so any
// 1.21 server, and newer Paper, can load the jar.
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.yaml:snakeyaml:2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(mapOf("version" to pluginVersion))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("ExplorationCore-${project.version}.jar")
    mergeServiceFiles()
    // The SQLite native library looks up org.sqlite.core.NativeDB by JNI name.
    // Relocating that package makes the driver fail when it opens a database.
    // The driver ships a native build for every OS and CPU. Keep the two this
    // plugin runs on: Windows x86_64 here, and glibc Linux x86_64 on the VPS.
    exclude { element: FileTreeElement ->
        val path = element.path.replace('\\', '/')
        path.startsWith("org/sqlite/native/") &&
            !path.startsWith("org/sqlite/native/Linux/x86_64/") &&
            !path.startsWith("org/sqlite/native/Windows/x86_64/")
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

// Writes dev/book-preview/book.js from ExplorationBook.prepare. The browser
// paints that file; it does not wrap lines itself.
tasks.register<JavaExec>("bookPreview") {
    group = "development"
    description = "Write the exploration book preview."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.explorationcore.book.BookPreviewDump")
    workingDir = layout.projectDirectory.asFile
    dependsOn(tasks.named("testClasses"))
    val previewDir = layout.projectDirectory.dir("dev/book-preview")
    args(
        previewDir.file("book.js").asFile.absolutePath,
        previewDir.dir("assets").asFile.absolutePath
    )
    if (project.hasProperty("minecraftJar")) {
        args(project.property("minecraftJar").toString())
    }
    if (project.hasProperty("player")) {
        systemProperty("book.player", project.property("player").toString())
    }
}
