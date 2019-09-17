import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    java
    maven
    kotlin("jvm").version("1.3.40")
    id("nebula.dependency-lock").version("2.2.4")
    id("org.jetbrains.kotlin.kapt").version("1.3.40")
}

group = "click.seichi"
version = "0.1.0-SNAPSHOT"

project.sourceSets {
    getByName("main") {
        java.srcDir("src/main/java")

        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/main/java")
        }
    }
    getByName("test") {
        java.srcDir("src/test/java")

        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/test/java")
        }
    }
}

repositories {
    maven { url = URI("https://oss.sonatype.org/content/repositories/snapshots") }
    mavenCentral()
}

val embed: Configuration by configurations.creating

configurations.implementation { extendsFrom(embed) }

dependencies {
    embed("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    embed(kotlin("stdlib-jdk8"))

    implementation("net.md-5:bungeecord-api:1.8-SNAPSHOT")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    from(sourceSets.main.get().resources.srcDirs) {
        include("**")

        val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name
        )

        filter<ReplaceTokens>("tokens" to tokenReplacementMap)
    }
    from(projectDir) { include("LICENSE") }
}


tasks.withType(JavaCompile::class.java).all {
    this.options.encoding = "UTF-8"
}

tasks.jar {
    // Configurationをコピーしないと変更を行っているとみなされて怒られる
    val embedConfiguration = embed.copy()

    from(embedConfiguration.map { if (it.isDirectory) it else zipTree(it) })
}

val compilerArgument = listOf("-Xlint:unchecked", "-Xlint:deprecation")
val kotlinCompilerArgument = listOf("-Xjsr305=strict")

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = compilerArgument + kotlinCompilerArgument
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = compilerArgument + kotlinCompilerArgument
}

val compileJava: JavaCompile by tasks
compileJava.options.compilerArgs.addAll(compilerArgument)
