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
import com.seiama.javaducks.service.javadoc.JavadocKey;
import com.seiama.javaducks.service.maven.MavenService;
import com.seiama.javaducks.service.maven.request.ArtifactRequest;
import com.seiama.javaducks.util.exception.HashNotFoundException;
import com.seiama.javaducks.util.maven.MavenConstants;
import com.seiama.javaducks.util.maven.MavenHashType;
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
  private final LoadingCache<JavadocKey, CachedLookup> contents;
  private final MavenService mavenService;

  @Autowired
  public JavadocService(final AppConfiguration configuration, final MavenService mavenService) {
    this.configuration = configuration;
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
        final AppConfiguration.EndpointConfiguration.Version config = this.configuration.endpoint(key.project(), key.version());
        if (config != null) {
          // WEEEEEEEEEEEEEEE wat do
          
          //return new CachedLookup(FileSystems.newFileSystem(this.configuration.endpoint(key.project(), key.version())))
          if (true) return null;
          return switch (config.type()) {
            case SNAPSHOT, RELEASE -> {
              final Path path = this.configuration.storage().resolve(key.project()).resolve(key.version() + ".jar");
              if (Files.isRegularFile(path)) {
                yield new CachedLookup(FileSystems.newFileSystem(path), null);
              }
              yield null;
            }
            case REDIRECT -> new CachedLookup(null, URI.create(config.path()));
          };
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
    return this.configuration.storage().resolve(project).resolve("favicon.ico");
  }

  @Scheduled(
    initialDelay = REFRESH_INITIAL_DELAY,
    fixedRate = REFRESH_RATE,
    timeUnit = TimeUnit.MINUTES
  )
  public void refreshAll() {
    for (final AppConfiguration.EndpointConfiguration endpoint : this.configuration.endpoints()) {
      this.refreshEndpoint(endpoint);
    }
  }

  private void refreshEndpoint(final AppConfiguration.EndpointConfiguration endpoint) {
    final Path basePath = this.configuration.storage().resolve(endpoint.name());
    for (final AppConfiguration.EndpointConfiguration.Version version : endpoint.versions()) {
      System.out.println("VERSION REGEX SPLIT HERE!!!: " + version.path());
      this.refreshVersion(endpoint, version, basePath);
    }
  }

  private void refreshVersion(final AppConfiguration.EndpointConfiguration config, final AppConfiguration.EndpointConfiguration.Version version, final Path basePath) {
    // Download using MavenServiceen
    final String[] versionRegex = version.path().split(":");
    System.out.println("VERSION REGEX SPLIT HERE!!!: " + version.path() + " " + versionRegex[0] + " " + versionRegex[1] + " " + versionRegex[2]);
    final byte[] result = this.mavenService.artifactFor("papermc-public", new ArtifactRequest(
      versionRegex[0],
      versionRegex[1],
      versionRegex[2],
      null,
      null,
      MavenConstants.CLASSIFIER_JAVADOC,
      MavenConstants.EXTENSION_JAR,
      null,
      null
    ));

    if (true) return;
    final URI jar = this.resolveUriFor(config, version);
    if (jar == null) return;

    if (version.type() == AppConfiguration.EndpointConfiguration.Version.Type.REDIRECT) {
      LOGGER.debug("Javadoc for {} {} is a redirect and will not be updated", config.name(), version.name());
      return;
    }

    final Path versionPath = basePath.resolve(version.name() + ".jar");
    try {
      Files.createDirectories(versionPath.getParent());
    } catch (final IOException e) {
      LOGGER.warn("Could not update javadoc for {} {}. Couldn't create directory. Exception: {}: {}", config.name(), version.name(), e.getClass().getName(), e.getMessage());
      return;
    }

    // don't download again if it's a release
    if (version.type() == AppConfiguration.EndpointConfiguration.Version.Type.RELEASE && Files.exists(versionPath)) {
      LOGGER.debug("Javadoc for {} {} is a release and will not be updated", config.name(), version.name());
      return;
    }

    // get hash
    final MavenHashPair hashPair = this.downloadHash(config, jar, version);

    // check hash
    if (Files.isReadable(versionPath)) {
      try {
        final String hashOnDisk = hashPair.type().algorithm().hash(versionPath).toString();
        if (hashOnDisk.equals(hashPair.hash())) {
          LOGGER.debug("Javadoc for {} {} is up to date", config.name(), version.name());
          return;
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }

    this.downloadJar(config, version, jar, hashPair, versionPath);

  }

  private void downloadJar(final AppConfiguration.EndpointConfiguration config, final AppConfiguration.EndpointConfiguration.Version version, final URI jar, final MavenHashPair hashPair, final Path versionPath) {
    try {
      final ResponseEntity<byte[]> response = this.restClient.get()
        .uri(jar)
        .header(HttpHeaders.USER_AGENT, USER_AGENT)
        .retrieve()
        .toEntity(byte[].class);
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        LOGGER.warn("Could not update javadoc for {} {}. Couldn't download jar. Url: {}, Status code: {}", config.name(), version.name(), jar, response.getStatusCode());
        return;
      }
      final String downloadedHash = hashPair.type().algorithm().hash(response.getBody()).toString();
      if (!downloadedHash.equals(hashPair.hash())) {
        LOGGER.warn("Could not update javadoc for {} {}. {} Hash mismatch. Expected: {}, got: {}", config.name(), version.name(), hashPair.type(), hashPair.hash(), downloadedHash);
        return;
      }
      Files.write(versionPath, response.getBody(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (final Exception e) {
      LOGGER.warn("Could not update javadoc for {} {}. Couldn't download jar. Url: {}, Exception: {}: {}", config.name(), version.name(), jar, e.getClass().getName(), e.getMessage());
      return;
    }
    LOGGER.info("Updated javadoc for {} {}", config.name(), version.name());
  }

  private @Nullable URI resolveUriFor(final AppConfiguration.EndpointConfiguration config, final AppConfiguration.EndpointConfiguration.Version version) {
    return switch (version.type()) {
      case RELEASE, REDIRECT -> URI.create(version.path());
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
  }

  public MavenHashPair downloadHash(final AppConfiguration.EndpointConfiguration config, final URI jarUri, final AppConfiguration.EndpointConfiguration.Version version) {
    for (final MavenHashType hashType : this.configuration.hashTypes()) {
      final URI hashUri = UriComponentsBuilder.fromUri(jarUri).replacePath(jarUri.getPath() + "." + hashType.extension()).build().toUri();
      try {
        final ResponseEntity<String> response = this.restClient.get()
          .uri(hashUri)
          .header(HttpHeaders.USER_AGENT, USER_AGENT)
          .retrieve()
          .toEntity(String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          LOGGER.debug("Downloaded hash for {}. Url: {} using hash type {}", config.name(), hashUri, hashType);
          return new MavenHashPair(response.getBody(), hashType);
        }
      } catch (final Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Could not download {} hash for {}. Url: {}, Exception: {}: {}", hashType, config.name(), hashUri, e.getClass().getName(), e.getMessage());
        } else {
          LOGGER.warn("Could not download {} hash for {}. Url: {}, Exception: {}", hashType, config.name(), hashUri, e.getClass().getName());
        }
      }
    }
    throw new HashNotFoundException(config.name(), version.name());
  }

  @NullMarked
  public record MavenHashPair(
    String hash,
    MavenHashType type
  ) {
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
