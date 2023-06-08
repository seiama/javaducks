plugins {
  val indraVersion = "3.1.1"
  id("com.diffplug.spotless") version "6.19.0"
  id("net.kyori.indra") version indraVersion
  id("net.kyori.indra.checkstyle") version indraVersion
  id("io.spring.dependency-management") version "1.1.0"
  id("org.springframework.boot") version "3.1.0"
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

  github("seiama", "javaducks")
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

dependencies {
  annotationProcessor("org.springframework.boot", "spring-boot-configuration-processor")
  checkstyle("ca.stellardrift:stylecheck:0.2.1")
  compileOnlyApi("org.jetbrains:annotations:24.0.1")
  implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.6")
  implementation("org.apache.maven", "maven-repository-metadata", "3.9.2")
  implementation("org.springframework.boot", "spring-boot-starter-web")
  testImplementation("org.springframework.boot", "spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
}
