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
package com.seiama.javaducks.service.maven.request;

import com.seiama.javaducks.util.maven.MavenConstants;
import com.seiama.javaducks.util.maven.MavenHashType;
import com.seiama.javaducks.util.maven.MavenSignatureType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ArtifactRequest(
  String groupId,
  String artifactId,
  String version,
  @Nullable String timestamp,
  @Nullable Integer buildNumber,
  @Nullable String classifier,
  String extension,
  @Nullable MavenHashType hash,
  @Nullable MavenSignatureType signature
) {
  public boolean isSnapshot() {
    return this.version.endsWith(MavenConstants.SNAPSHOT_VERSION_SUFFIX);
  }

  public ArtifactRequest withVersion(final String version) {
    return new ArtifactRequest(
      this.groupId,
      this.artifactId,
      version,
      this.timestamp,
      this.buildNumber,
      this.classifier,
      this.extension,
      this.hash,
      this.signature
    );
  }

  public ArtifactRequest withSnapshot(
    final @Nullable String timestamp,
    final @Nullable Integer buildNumber
  ) {
    return new ArtifactRequest(
      this.groupId,
      this.artifactId,
      this.version,
      timestamp,
      buildNumber,
      this.classifier,
      this.extension,
      this.hash,
      this.signature
    );
  }

  public ArtifactRequest withClassifier(final @Nullable String classifier) {
    return new ArtifactRequest(
      this.groupId,
      this.artifactId,
      this.version,
      this.timestamp,
      this.buildNumber,
      classifier,
      this.extension,
      this.hash,
      this.signature
    );
  }

  public ArtifactRequest withHash(final @Nullable MavenHashType hash) {
    return new ArtifactRequest(
      this.groupId,
      this.artifactId,
      this.version,
      this.timestamp,
      this.buildNumber,
      this.classifier,
      this.extension,
      hash,
      this.signature
    );
  }

  public ArtifactRequest withHash(final @Nullable MavenSignatureType signature) {
    return new ArtifactRequest(
      this.groupId,
      this.artifactId,
      this.version,
      this.timestamp,
      this.buildNumber,
      this.classifier,
      this.extension,
      this.hash,
      signature
    );
  }

  public String toUrl() {
    final StringBuilder sb = new StringBuilder();
    sb.append(this.groupId.replace('.', '/'));
    sb.append('/');
    sb.append(this.artifactId);
    sb.append('/');
    sb.append(this.version);
    sb.append('/');
    sb.append(this.artifactId);
    sb.append('-');
    sb.append(MavenConstants.versionWithoutSnapshotSuffix(this.version));
    if (this.timestamp != null && this.buildNumber != null) {
      sb.append('-');
      sb.append(this.timestamp);
      sb.append('-');
      sb.append(this.buildNumber);
    }
    if (this.classifier != null) {
      sb.append('-');
      sb.append(this.classifier);
    }
    sb.append('.').append(this.extension);
    if (this.hash != null) {
      sb.append('.').append(this.hash.extension());
    }
    if (this.signature != null) {
      sb.append('.').append(this.signature.extension());
    }
    return sb.toString();
  }
}
