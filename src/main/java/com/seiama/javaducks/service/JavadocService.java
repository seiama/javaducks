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
package com.seiama.javaducks.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.configuration.properties.JavadocConfiguration;
import com.seiama.javaducks.service.javadoc.JavadocKey;
import com.seiama.javaducks.service.maven.MavenService;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class JavadocService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JavadocService.class);
  private static final long REFRESH_INITIAL_DELAY = 0; // in minutes
  private static final long REFRESH_RATE = 15; // in minutes
  private final JavadocConfiguration configuration;
  private final AppConfiguration appConfiguration;
  private final LoadingCache<JavadocKey, CachedLookup> contents;
  private final MavenService mavenService;

  @Autowired
  public JavadocService(final JavadocConfiguration configuration, final AppConfiguration appConfiguration, final MavenService mavenService) {
    this.configuration = configuration;
    this.appConfiguration = appConfiguration;
    this.mavenService = mavenService;
    this.contents = Caffeine.newBuilder()
      .refreshAfterWrite(Duration.ofMinutes(10))
      .removalListener((RemovalListener<JavadocKey, CachedLookup>) (key, value, cause) -> {
        if (value != null) {
          try {
            value.close();
          } catch (final IOException e) {
            LOGGER.error("Could not close file system", e);
          }
        }
      })
      .build(key -> {
        final JavadocConfiguration.Alias.Endpoint endpoint = configuration.findAliasEndpoint(key.project(), key.version());
        if (endpoint != null) {
          final MavenService.Artifact artifact = mavenService.artifactFor(endpoint.repository(), endpoint.artifact().asMavenRequest());
          if (artifact != null) {
            return new CachedLookup(FileSystems.newFileSystem(artifact.path()), null);
          }
        }
        return null;
      });
  }

  public @Nullable Result contentsFor(final JavadocKey key, final String path) {
    final CachedLookup lookup = this.contents.get(key);
    if (lookup != null) {
      if (lookup.fs() != null) {
        return new Result(lookup.fs().getPath(path), null);
      } else if (lookup.uri() != null) {
        return new Result(null, lookup.uri());
      }
    }
    return null;
  }

  public Path faviconFor(final String project) {
    return this.appConfiguration.storage().resolve(project).resolve("favicon.ico");
  }

  @Scheduled(
    initialDelay = REFRESH_INITIAL_DELAY,
    fixedRate = REFRESH_RATE,
    timeUnit = TimeUnit.MINUTES
  )
  public void preloadAll() {
    for (final JavadocConfiguration.Alias alias : this.configuration.aliases()) {
      for (final JavadocConfiguration.Alias.Endpoint endpoint : alias.endpoints()) {
        this.preloadEndpoint(endpoint);
      }
    }
  }

  private void preloadEndpoint(final JavadocConfiguration.Alias.Endpoint endpoint) {
    LOGGER.info("Preloading javadoc for {}...", endpoint.artifact());
    Throwable error = null;
    try {
      final MavenService.Artifact artifact = this.mavenService.artifactFor(endpoint.repository(), endpoint.artifact().asMavenRequest());
      if (artifact != null) {
        LOGGER.info("Successfully preloaded javadocs for {}", endpoint.artifact());
        return;
      }
    } catch (final Throwable t) {
      error = t;
    }
    final String message = String.format("Could not preload javadocs for %s", endpoint.artifact());
    if (error != null) {
      LOGGER.error(message, error);
    } else {
      LOGGER.error(message);
    }
  }

  @NullMarked
  record CachedLookup(
    @Nullable FileSystem fs,
    @Nullable URI uri
  ) implements AutoCloseable {
    @Override
    public void close() throws IOException {
      if (this.fs != null) {
        this.fs.close();
      }
    }
  }

  @NullMarked
  public record Result(
    @Nullable Path file,
    @Nullable URI uri
  ) {
  }
}
