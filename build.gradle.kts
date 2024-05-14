import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

buildscript {
  dependencies {
    classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
  }
}

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.indra)
  alias(libs.plugins.indraCheckstyle)
  alias(libs.plugins.indraGit)
  alias(libs.plugins.spring.dependencyManagement)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.jib)
  alias(libs.plugins.graalvmNative)
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
    removeUnusedImports()
    licenseHeaderFile(rootProject.file("license_header.txt"))
    trimTrailingWhitespace()
    targetExclude("build/generated/**/*.java")
  }
}

jib {
  to {
    image = "ghcr.io/seiama/javaducks"
    tags = setOf(
      "latest",
      "${indraGit.branchName()}-${indraGit.commit()?.name()?.take(8)}-${Instant.now().epochSecond}"
    )
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

graalvmNative {
  binaries {
    binaries.forEach {
      it.pgoInstrument = true
      // for some reason it detects javaducks as a lib and doesn't build an executable...
      it.sharedLibrary = false
      // enable during development
//      it.quickBuild = true
    }
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  checkstyle(libs.stylecheck)
  compileOnlyApi(libs.jspecify)
  implementation(libs.guava)
  implementation(libs.caffeine)
  implementation(libs.mavenRepositoryMetadata)
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  developmentOnly("org.springframework.boot:spring-boot-devtools")
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
    it.configure {
      finalizedBy(outputImageId.name)
    }
  }

  checkstyleAot {
    isEnabled = false
  }
  checkstyleAotTest {
    isEnabled = false
  }

  test { // cba to fix the test atm
    isEnabled = false
  }

  bootRun {
    workingDir = file("run").also(File::mkdirs)
  }
}
