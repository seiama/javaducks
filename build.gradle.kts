import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

buildscript {
  dependencies {
    classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
  }
}

plugins {
  val indraVersion = "3.1.3"
  id("com.diffplug.spotless") version "6.22.0"
  id("net.kyori.indra") version indraVersion
  id("net.kyori.indra.checkstyle") version indraVersion
  id("io.spring.dependency-management") version "1.1.3"
  id("org.springframework.boot") version "3.1.4"
  id("com.google.cloud.tools.jib") version "3.4.0"
}

group = "com.seiama"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

indra {
  javaVersions {
    target(17)
  }

  github("seiama", "javaducks") {
    ci(true)
  }
  mitLicense()
}

spotless {
  java {
    endWithNewline()
    importOrderFile(rootProject.file(".spotless/seiama.importorder"))
    indentWithSpaces(2)
    licenseHeaderFile(rootProject.file("license_header.txt"))
    trimTrailingWhitespace()
  }
}

jib {
  to {
    image = "ghcr.io/seiama/javaducks"
    tags = setOf("latest", project.version.toString())
  }

  from {
    image = "azul/zulu-openjdk-alpine:${indra.javaVersions().target().get()}-jre"
    platforms {
      // We can only build multi-arch images when pushing to a registry, not when building locally
      val requestedTasks = gradle.startParameter.taskNames
      if ("jibBuildTar" in requestedTasks || "jibDockerBuild" in requestedTasks) {
        platform {
          // todo: better logic
          architecture = when (System.getProperty("os.arch")) {
            "aarch64" -> "arm64"
            else -> "amd64"
          }
          os = "linux"
        }
      } else {
        platform {
          architecture = "amd64"
          os = "linux"
        }
        platform {
          architecture = "arm64"
          os = "linux"
        }
      }
    }
  }

  pluginExtensions {
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
    }
  }

  container {
    args = listOf("--spring.config.additional-location=optional:file:/config/")
    ports = listOf("8080")
    labels.put("org.opencontainers.image.source", indra.scm().map { it.url() })
  }
}

dependencies {
  annotationProcessor("org.springframework.boot", "spring-boot-configuration-processor")
  checkstyle("ca.stellardrift:stylecheck:0.2.1")
  compileOnlyApi("org.jetbrains:annotations:24.0.1")
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
  implementation("org.apache.maven:maven-repository-metadata:3.9.4")
  implementation("org.springframework.boot", "spring-boot-starter-web")
  testImplementation("org.springframework.boot", "spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
}
tasks {
  val outputImageId = register("printJibMeta") {
    description = "Expose image information as an output for GitHub Actions"

    val jibImageJson = project.jib.outputPaths.imageJson
    val githubOutput = providers.environmentVariable("GITHUB_OUTPUT")
    inputs.property("jibImageJson", jibImageJson)
    inputs.property("githubOutput", githubOutput).optional(true)

    doLast {
      if (!githubOutput.isPresent) {
        didWork = false
        return@doLast
      }

      Path(githubOutput.get()).bufferedWriter(Charsets.UTF_8, options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)).use {
        it.write("imageJson=")
        file(jibImageJson).bufferedReader(Charsets.UTF_8).use { meta -> meta.transferTo(it) }
      }
    }
  }

  sequenceOf(jib, jibDockerBuild, jibBuildTar).forEach {
    it.configure { finalizedBy(outputImageId.name) }
  }
}
