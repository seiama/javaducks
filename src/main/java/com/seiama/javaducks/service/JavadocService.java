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
import com.seiama.javaducks.util.HashUtil;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@NullMarked
@Service
public class JavadocService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JavadocService.class);
  private static final long REFRESH_INITIAL_DELAY = 0; // in minutes
  private static final long REFRESH_RATE = 15; // in minutes
  private static final String USER_AGENT = "JavaDucks";
  private static final String MAVEN_METADATA = "maven-metadata.xml";
  private final RestClient restClient = RestClient.create();
  private final AppConfiguration configuration;
  private final LoadingCache<Key, FileSystem> contents;

  @Autowired
  public JavadocService(final AppConfiguration configuration) {
    this.configuration = configuration;
    this.contents = Caffeine.newBuilder()
      .refreshAfterWrite(Duration.ofMinutes(10))
      .removalListener((RemovalListener<Key, FileSystem>) (key, value, cause) -> {
        if (value != null) {
          try {
            value.close();
          } catch (final IOException e) {
            LOGGER.error("Could not close file system", e);
          }
        }
      })
      .build(key -> {
        final Path path = this.configuration.storage().resolve(key.project()).resolve(key.version() + ".jar");
        if (Files.isRegularFile(path)) {
          return FileSystems.newFileSystem(path);
        }
        return null;
      });
  }

  public @Nullable FileSystem contentsFor(final Key key) {
    return this.contents.get(key);
  }

  @Scheduled(
    initialDelay = REFRESH_INITIAL_DELAY,
    fixedRate = REFRESH_RATE,
    timeUnit = TimeUnit.MINUTES
  )
  public void refreshAll() {
    for (final AppConfiguration.EndpointConfiguration endpoint : this.configuration.endpoints()) {
      this.refreshOne(endpoint);
    }
  }

  private void refreshOne(final AppConfiguration.EndpointConfiguration config) {
    final Path basePath = this.configuration.storage().resolve(config.name());
    for (final AppConfiguration.EndpointConfiguration.Version version : config.versions()) {
      final @Nullable URI jar = switch (version.type()) {
        case RELEASE -> {
          yield URI.create(version.path());
        }
        case SNAPSHOT -> {
          final URI metaDataUri = version.asset(MAVEN_METADATA);
          final Metadata metadata;
          try {
            final ResponseEntity<String> response = this.restClient.get()
              .uri(metaDataUri)
              .header(HttpHeaders.USER_AGENT, USER_AGENT)
              .retrieve()
              .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
              LOGGER.warn("Could not fetch metadata for {} {}. Url: {}, Status code: {}", config.name(), version.name(), metaDataUri, response.getStatusCode());
              yield null;
            }
            final MetadataXpp3Reader reader = new MetadataXpp3Reader();
            metadata = reader.read(new StringReader(response.getBody()), true);
          } catch (final Exception e) {
            LOGGER.warn("Could not fetch metadata for {} {}. Url: {}, Exception: {}: {}", config.name(), version.name(), metaDataUri, e.getClass().getName(), e.getMessage());
            yield null;
          }
          final @Nullable Snapshot snapshot = metadata.getVersioning().getSnapshot();
          if (snapshot != null) {
            yield version.asset(String.format(
              "%s-%s-%s-%d-javadoc.jar",
              metadata.getArtifactId(),
              metadata.getVersion().replace("-SNAPSHOT", ""),
              snapshot.getTimestamp(),
              snapshot.getBuildNumber()
            ));
          } else {
            LOGGER.warn("Could not find latest version for {} {}", config.name(), version.name());
            yield null;
          }
        }
      };
      if (jar != null) {
        final Path versionPath = basePath.resolve(version.name() + ".jar");
        try {
          Files.createDirectories(versionPath.getParent());
        } catch (final IOException e) {
          LOGGER.warn("Could not update javadoc for {} {}. Couldn't not create dir. Exception: {}: {}", config.name(), version.name(), e.getClass().getName(), e.getMessage());
          continue;
        }

        // don't download again if it's a release
        if (version.type() == AppConfiguration.EndpointConfiguration.Version.Type.RELEASE && Files.exists(versionPath)) {
          LOGGER.debug("Javadoc for {} {} is a release and will not be updated", config.name(), version.name());
          return;
        }

        // get hash
        final URI hashUri = UriComponentsBuilder.fromUri(jar).replacePath(jar.getPath() + ".sha256").build().toUri();
        final String hash;
        try {
          final ResponseEntity<String> response = this.restClient.get()
            .uri(hashUri)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .retrieve()
            .toEntity(String.class);
          if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            LOGGER.warn("Could not update javadoc for {} {}. Couldn't download hash. Url: {}, Status code: {}", config.name(), version.name(), hashUri, response.getStatusCode());
            continue;
          }
          hash = response.getBody();
        } catch (final Exception e) {
          LOGGER.warn("Could not update javadoc for {} {}. Couldn't download hash. Url: {}, Exception: {}: {}", config.name(), version.name(), hashUri, e.getClass().getName(), e.getMessage());
          continue;
        }
        // check hash
        if (Files.isReadable(versionPath)) {
          try {
            final String hashOnDisk = HashUtil.sha256(Files.readAllBytes(versionPath));
            if (hashOnDisk.equals(hash)) {
              LOGGER.debug("Javadoc for {} {} is up to date", config.name(), version.name());
              continue;
            }
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        }
        // download jar
        try {
          final ResponseEntity<byte[]> response = this.restClient.get()
            .uri(jar)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .retrieve()
            .toEntity(byte[].class);
          if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            LOGGER.warn("Could not update javadoc for {} {}. Couldn't download jar. Url: {}, Status code: {}", config.name(), version.name(), jar, response.getStatusCode());
            continue;
          }
          final String downloadedHash = HashUtil.sha256(response.getBody());
          if (!downloadedHash.equals(hash)) {
            LOGGER.warn("Could not update javadoc for {} {}. Hash mismatch. Expected: {}, got: {}", config.name(), version.name(), hash, downloadedHash);
            continue;
          }
          Files.write(versionPath, response.getBody(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final Exception e) {
          LOGGER.warn("Could not update javadoc for {} {}. Couldn't download jar. Url: {}, Exception: {}: {}", config.name(), version.name(), jar, e.getClass().getName(), e.getMessage());
          continue;
        }
        LOGGER.info("Updated javadoc for {} {}", config.name(), version.name());
      }
    }
  }

  @NullMarked
  public record Key(
    String project,
    String version
  ) {
  }
}
