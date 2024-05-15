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
package com.seiama.javaducks.service.maven;

import com.seiama.javaducks.configuration.properties.MavenConfiguration;
import com.seiama.javaducks.service.maven.request.ArtifactRequest;
import com.seiama.javaducks.util.maven.metadata.MavenMetadata;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface MavenService {
  @Nullable MavenMetadata metadataFor(final ArtifactRequest request);

  @Nullable MavenMetadata metadataFor(final String repositoryName, final ArtifactRequest request);

  @Nullable MavenMetadata metadataFor(final MavenConfiguration.Repositories.@Nullable Repository repository, final ArtifactRequest request);

  @Nullable Artifact artifactFor(final ArtifactRequest request);

  @Nullable Artifact artifactFor(final String repositoryName, final ArtifactRequest request);

  @Nullable Artifact artifactFor(final MavenConfiguration.Repositories.@Nullable Repository repository, final ArtifactRequest request);

  @NullMarked
  record Artifact(
    Path path,
    byte[] bytes
  ) {
  }
}
