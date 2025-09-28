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
package com.seiama.javaducks.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "metadata")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = MavenMetadata.Builder.class)
public record MavenMetadata(
  @JacksonXmlProperty(localName = "modelVersion")
  @JsonProperty("modelVersion")
  String modelVersion,
  @JacksonXmlProperty(localName = "groupId")
  @JsonProperty("groupId")
  String groupId,
  @JacksonXmlProperty(localName = "artifactId")
  @JsonProperty("artifactId")
  String artifactId,
  @JacksonXmlProperty(localName = "version")
  @JsonProperty("version")
  String version,
  @JacksonXmlProperty(localName = "versioning")
  @JsonProperty("versioning")
  Versioning versioning
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonDeserialize(builder = Versioning.Builder.class)
  public record Versioning(
    @JacksonXmlProperty(localName = "latest")
    @JsonProperty("latest")
    String latest,
    @JacksonXmlProperty(localName = "release")
    @JsonProperty("release")
    String release,
    @JacksonXmlProperty(localName = "lastUpdated")
    @JsonProperty("lastUpdated")
    String lastUpdated,
    @JacksonXmlProperty(localName = "snapshot")
    @JsonProperty("snapshot")
    Snapshot snapshot,
    @JacksonXmlElementWrapper(localName = "versions")
    @JacksonXmlProperty(localName = "version")
    @JsonProperty("versions")
    List<String> versions,
    @JacksonXmlElementWrapper(localName = "snapshotVersions")
    @JacksonXmlProperty(localName = "snapshotVersion")
    @JsonProperty("snapshotVersions")
    List<SnapshotVersion> snapshotVersions
  ) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(builder = Snapshot.Builder.class)
    public record Snapshot(
      @JacksonXmlProperty(localName = "timestamp")
      @JsonProperty("timestamp")
      String timestamp,
      @JacksonXmlProperty(localName = "buildNumber")
      @JsonProperty("buildNumber")
      Integer buildNumber
    ) {
      public static class Builder {
        private String timestamp;
        private Integer buildNumber;

        @JsonProperty("timestamp")
        public Builder timestamp(final String timestamp) {
          this.timestamp = timestamp;
          return this;
        }

        @JsonProperty("buildNumber")
        public Builder buildNumber(final Integer buildNumber) {
          this.buildNumber = buildNumber;
          return this;
        }

        public Snapshot build() {
          return new Snapshot(this.timestamp, this.buildNumber);
        }
      }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(builder = SnapshotVersion.Builder.class)
    public record SnapshotVersion(
      @JacksonXmlProperty(localName = "extension")
      @JsonProperty("extension")
      String extension,
      @JacksonXmlProperty(localName = "value")
      @JsonProperty("value")
      String value,
      @JacksonXmlProperty(localName = "updated")
      @JsonProperty("updated")
      String updated,
      @JacksonXmlProperty(localName = "classifier")
      @JsonProperty("classifier")
      String classifier
    ) {
      public static class Builder {
        private String extension;
        private String value;
        private String updated;
        private String classifier;

        @JsonProperty("extension")
        public Builder extension(final String extension) {
          this.extension = extension;
          return this;
        }

        @JsonProperty("value")
        public Builder value(final String value) {
          this.value = value;
          return this;
        }

        @JsonProperty("updated")
        public Builder updated(final String updated) {
          this.updated = updated;
          return this;
        }

        @JsonProperty("classifier")
        public Builder classifier(final String classifier) {
          this.classifier = classifier;
          return this;
        }

        public SnapshotVersion build() {
          return new SnapshotVersion(this.extension, this.value, this.updated, this.classifier);
        }
      }
    }

    public static class Builder {
      private String latest;
      private String release;
      private String lastUpdated;
      private Snapshot snapshot;
      private List<String> versions;
      private List<SnapshotVersion> snapshotVersions;

      @JsonProperty("latest")
      public Builder latest(final String latest) {
        this.latest = latest;
        return this;
      }

      @JsonProperty("release")
      public Builder release(final String release) {
        this.release = release;
        return this;
      }

      @JsonProperty("lastUpdated")
      public Builder lastUpdated(final String lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
      }

      @JsonProperty("snapshot")
      public Builder snapshot(final Snapshot snapshot) {
        this.snapshot = snapshot;
        return this;
      }

      @JsonProperty("versions")
      @JacksonXmlElementWrapper(localName = "versions")
      @JacksonXmlProperty(localName = "version")
      public Builder versions(final List<String> versions) {
        this.versions = versions;
        return this;
      }

      @JsonProperty("snapshotVersions")
      @JacksonXmlElementWrapper(localName = "snapshotVersions")
      @JacksonXmlProperty(localName = "snapshotVersion")
      public Builder snapshotVersions(final List<SnapshotVersion> snapshotVersions) {
        this.snapshotVersions = snapshotVersions;
        return this;
      }

      public Versioning build() {
        return new Versioning(this.latest, this.release, this.lastUpdated, this.snapshot, this.versions, this.snapshotVersions);
      }
    }
  }

  public static class Builder {
    private String modelVersion;
    private String groupId;
    private String artifactId;
    private String version;
    private Versioning versioning;

    @JsonProperty("modelVersion")
    public Builder modelVersion(final String modelVersion) {
      this.modelVersion = modelVersion;
      return this;
    }

    @JsonProperty("groupId")
    public Builder groupId(final String groupId) {
      this.groupId = groupId;
      return this;
    }

    @JsonProperty("artifactId")
    public Builder artifactId(final String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    @JsonProperty("version")
    public Builder version(final String version) {
      this.version = version;
      return this;
    }

    @JsonProperty("versioning")
    public Builder versioning(final Versioning versioning) {
      this.versioning = versioning;
      return this;
    }

    public MavenMetadata build() {
      return new MavenMetadata(this.modelVersion, this.groupId, this.artifactId, this.version, this.versioning);
    }
  }
}
