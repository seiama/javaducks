/*
 * This file is part of javaducks, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 Seiama
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.seiama.javaducks.configuration.properties;

import com.seiama.javaducks.util.maven.MavenHashType;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
@NullMarked
public record AppConfiguration(
  URI rootRedirect,
  URI hostName,
  Path storage,
  List<EndpointConfiguration> endpoints,
  @DefaultValue({"SHA512", "SHA256", "SHA1"})
  List<MavenHashType> hashTypes
) {

  public EndpointConfiguration.@Nullable Version endpoint(final String endpointName, final String versionName) {
    for (final EndpointConfiguration endpoint : this.endpoints) {
      if (endpoint.name().equals(endpointName)) {
        for (final EndpointConfiguration.Version version : endpoint.versions()) {
          if (version.name().equals(versionName)) {
            return version;
          }
        }
      }
    }
    return null;
  }

  @NullMarked
  public record EndpointConfiguration(
    String name,
    List<Version> versions
  ) {
    @NullMarked
    public record Version(
      String name,
      @Nullable String path,
      @Nullable URI repository,
      @Nullable String group,
      @Nullable String artifact,
      @Nullable String version,
      Type type
    ) {
      public URI redirectUri() {
        return URI.create(Objects.requireNonNull(this.path, "path"));
      }

      public URI artifactMetadata() {
        return URI.create(this.mavenArtifactBase() + "maven-metadata.xml");
      }

      public URI versionMetadata() {
        return URI.create(this.mavenVersionBase(Objects.requireNonNull(this.version, "version")) + "maven-metadata.xml");
      }

      public URI snapshotJavadocJar(final String snapshotVersion) {
        return URI.create(this.mavenVersionBase(Objects.requireNonNull(this.version, "version")) + this.requireArtifact() + "-" + snapshotVersion + "-javadoc.jar");
      }

      public URI javadocJar() {
        return this.javadocJar(Objects.requireNonNull(this.version, "version"));
      }

      public URI javadocJar(final String resolvedVersion) {
        return URI.create(this.mavenArtifactBase() + resolvedVersion + "/" + this.requireArtifact() + "-" + resolvedVersion + "-javadoc.jar");
      }

      private String mavenArtifactBase() {
        final String repositoryString = Objects.requireNonNull(this.repository, "repository").toString();
        final String normalizedRepository = repositoryString.endsWith("/") ? repositoryString : repositoryString + "/";
        return normalizedRepository + this.requireGroup().replace('.', '/') + "/" + this.requireArtifact() + "/";
      }

      private String mavenVersionBase(final String version) {
        return this.mavenArtifactBase() + version + "/";
      }

      private String requireGroup() {
        return Objects.requireNonNull(this.group, "group");
      }

      private String requireArtifact() {
        return Objects.requireNonNull(this.artifact, "artifact");
      }

      public boolean isSnapshot() {
        return Objects.requireNonNull(this.version, "version").endsWith("-SNAPSHOT");
      }

      public boolean isChangingRelease() {
        return Objects.requireNonNull(this.version, "version").endsWith("+");
      }

      public String changingReleasePrefix() {
        final String resolvedVersion = Objects.requireNonNull(this.version, "version");
        return resolvedVersion.substring(0, resolvedVersion.length() - 1);
      }

      @NullMarked
      public enum Type {
        MAVEN,
        REDIRECT,
      }
    }
  }
}
