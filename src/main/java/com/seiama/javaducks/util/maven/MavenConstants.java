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
package com.seiama.javaducks.util.maven;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MavenConstants {
  public static final String SNAPSHOT_VERSION_SUFFIX = "-SNAPSHOT";

  public static final String METADATA_FILE_NAME = "maven-metadata.xml";

  public static final String CLASSIFIER_JAVADOC = "javadoc";
  public static final String EXTENSION_JAR = "jar";

  private MavenConstants() {
  }

  public static String versionWithoutSnapshotSuffix(final String version) {
    if (version.endsWith(SNAPSHOT_VERSION_SUFFIX)) {
      return version.substring(0, version.length() - SNAPSHOT_VERSION_SUFFIX.length());
    }
    return version;
  }

  public static String metadataUrl(final String groupId, final String artifactId, final @Nullable String version) {
    final StringBuilder sb = new StringBuilder();
    sb.append(groupId.replace('.', '/'));
    sb.append('/');
    sb.append(artifactId);
    sb.append('/');
    if (version != null) {
      sb.append(version);
      sb.append('/');
    }
    sb.append(METADATA_FILE_NAME);
    return sb.toString();
  }
}
