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
package com.seiama.javaducks.service.javadoc;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.seiama.javaducks.configuration.properties.JavadocConfiguration;
import com.seiama.javaducks.service.maven.MavenService;
import com.seiama.javaducks.service.maven.request.ArtifactRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class JavadocService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JavadocService.class);
  private static final Duration EXPIRE_AFTER = Duration.ofMinutes(10);
  private static final String TEMPORARY_FILE_PREFIX = "fill_jd_";
  private static final @Nullable String TEMPORARY_FILE_SUFFIX = null;
  private final LoadingCache<ArtifactRequest, Entry> contents;

  @Autowired
  public JavadocService(
    final JavadocConfiguration javadocConfiguration,
    final MavenService mavenService
  ) {
    this.contents = Caffeine.newBuilder()
      .expireAfterAccess(EXPIRE_AFTER)
      .removalListener((RemovalListener<ArtifactRequest, Entry>) (key, value, cause) -> {
        if (value != null) {
          try {
            value.data().close();
          } catch (final IOException e) {
            LOGGER.error("Could not close file system", e);
          }
          try {
            Files.deleteIfExists(value.path());
          } catch (final IOException e) {
            LOGGER.error("Could not delete temporary file", e);
          }
        }
      })
      .build(key -> {
        for (final String repository : javadocConfiguration.repositories()) {
          final InputStream stream = mavenService.artifactFor(repository, key);
          if (stream != null) {
            final Path path = Files.createTempFile(TEMPORARY_FILE_PREFIX, TEMPORARY_FILE_SUFFIX);
            Files.write(path, stream.readAllBytes());
            if (Files.isRegularFile(path)) {
              return new Entry(path, FileSystems.newFileSystem(path));
            }
          }
        }
        return null;
      });
  }

  public @Nullable FileSystem contentsFor(final ArtifactRequest key) {
    final @Nullable Entry entry = this.contents.get(key);
    if (entry != null) {
      return entry.data();
    }
    return null;
  }

  private record Entry(
    Path path,
    FileSystem data
  ) {
  }
}
