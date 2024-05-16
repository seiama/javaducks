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
import com.seiama.javaducks.util.maven.MavenConstants;
import com.seiama.javaducks.util.maven.MavenHashPair;
import com.seiama.javaducks.util.maven.MavenHashType;
import com.seiama.javaducks.util.maven.metadata.MavenMetadata;
import com.seiama.javaducks.util.maven.metadata.Snapshot;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@NullMarked
@Service
class MavenServiceImpl implements MavenService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MavenServiceImpl.class);
  private static final Duration CACHED_METADATA_EXPIRATION = Duration.ofMinutes(5);
  private static final String USER_AGENT = "JavaDucks";
  private static final MavenHashType[] HASH_TYPES = MavenHashType.values();
  private final RestClient restClient = RestClient.create();
  private final MavenConfiguration configuration;

  @Autowired
  MavenServiceImpl(final MavenConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public @Nullable MavenMetadata metadataFor(final ArtifactRequest request) {
    for (final MavenConfiguration.Repositories.Repository repository : this.configuration.repositories().all()) {
      final MavenMetadata metadata = this.metadataFor(repository, request);
      if (metadata != null) {
        return metadata;
      }
    }
    return null;
  }

  @Override
  public @Nullable MavenMetadata metadataFor(final String repositoryName, final ArtifactRequest request) {
    return this.metadataFor(this.configuration.repositories().get(repositoryName), request);
  }

  @Override
  public @Nullable MavenMetadata metadataFor(final MavenConfiguration.Repositories.@Nullable Repository repository, final ArtifactRequest request) {
    final @Nullable Artifact bytes = this.get(repository, MavenConstants.metadataUrl(request.groupId(), request.artifactId(), request.version()), CACHED_METADATA_EXPIRATION);
    if (bytes != null) {
      try {
        return MavenMetadata.XML_MAPPER.readValue(bytes.bytes(), MavenMetadata.class);
      } catch (final IOException e) {
        LOGGER.error("Could not parse {} as maven metadata", bytes);
        e.printStackTrace(); // TODO
        return null;
      }
    }
    return null;
  }

  @Override
  public @Nullable Artifact artifactFor(final ArtifactRequest request) {
    for (final MavenConfiguration.Repositories.Repository repository : this.configuration.repositories().all()) {
      final @Nullable Artifact bytes = this.artifactFor(repository, request);
      if (bytes != null) {
        return bytes;
      }
    }
    return null;
  }

  @Override
  public @Nullable Artifact artifactFor(final String repositoryName, final ArtifactRequest request) {
    return this.artifactFor(this.configuration.repositories().get(repositoryName), request);
  }

  @Override
  public @Nullable Artifact artifactFor(final MavenConfiguration.Repositories.@Nullable Repository repository, final ArtifactRequest request) {
    if (request.isSnapshot()) {
      final @Nullable MavenMetadata metadata = this.metadataFor(repository, request);
      if (metadata != null) {
        final Snapshot snapshot = metadata.versioning().snapshot();
        if (snapshot != null) {
          LOGGER.info("Resolved snapshot for {} to {}", request, snapshot);
          return this.get(repository, request.withSnapshot(snapshot.timestamp(), Integer.parseInt(snapshot.buildNumber())).toUrl(), null);
        }
      }
    }
    return this.get(repository, request.toUrl(), null);
  }

  private @Nullable Artifact get(
    final MavenConfiguration.Repositories.@Nullable Repository repository,
    final String url,
    final @Nullable Duration lifetime
  ) {
    if (repository != null) {
      if (repository instanceof final MavenConfiguration.Repositories.Group group) {
        for (final String member : group.members()) {
          final @Nullable Artifact result = this.get(this.configuration.repositories().get(member), url, lifetime);
          if (result != null) {
            return result;
          }
        }
      } else if (repository instanceof final MavenConfiguration.Repositories.Proxied proxied) {
        return this.getProxied(proxied, url, lifetime);
      }
    }
    return null;
  }

  private @Nullable Artifact getProxied(
    final MavenConfiguration.Repositories.@Nullable Proxied repository,
    final String url,
    final @Nullable Duration lifetime
  ) {
    if (repository != null) {
      final URI remoteUrl = repository.url(url);

      final Path path = repository.cache().resolve(url);

      // First we check if the file exceeds the lifetime for cached files
      this.tryAndRemoveExpiredFile(path, lifetime);

      @Nullable MavenHashPair hash = null;
      if (shouldCheckHashes(url)) {
        hash = this.downloadHash(remoteUrl);
      }

      if (Files.isRegularFile(path)) {
        boolean canUseCachedAsset = true;
        if (hash != null) {
          try {
            final String hashOfCachedAsset = hash.type().algorithm().hash(path).toString();
            if (!hashOfCachedAsset.equals(hash.hash())) {
              canUseCachedAsset = false;
            }
          } catch (final IOException e) {
            LOGGER.error("Could not compute hash {} of cached asset {}", hash.type(), path);
          }
        }
        if (canUseCachedAsset) {
          try {
            final byte[] bytes = Files.readAllBytes(path);
            LOGGER.debug("Using non-expired cached file {} (hash: {}) for request {}", path, hash, url);
            return new Artifact(path, bytes);
          } catch (final IOException e) {
            LOGGER.error("Could not read cached asset {}", path);
            return null;
          }
        }
      }

      final ResponseEntity<byte[]> response;
      try {
        response = this.restClient.get()
          .uri(repository.url(url))
          .header(HttpHeaders.USER_AGENT, USER_AGENT)
          .retrieve()
          .toEntity(byte[].class);
      } catch (final HttpClientErrorException e) {
        LOGGER.error("Could not fetch remote asset %s due to http error".formatted(remoteUrl), e);
        return null;
      }

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        LOGGER.error("Could not fetch remote asset %s due to non-successful response".formatted(remoteUrl));
        return null;
      }

      if (hash != null && shouldCheckHashes(url)) {
        final String hashOfRemoteAsset = hash.type().algorithm().hash(response.getBody()).toString();
        if (!hashOfRemoteAsset.equals(hash.hash())) {
          LOGGER.warn("Could not verify remote asset {} due to {} hash mismatch - expected: {}, actual: {}", remoteUrl, hash.type(), hash.hash(), hashOfRemoteAsset);
          return null;
        }
      }

      final byte[] body = response.getBody();
      try {
        Files.createDirectories(path.getParent());
      } catch (final IOException e) {
        LOGGER.error("Could not create parent directories for asset {}", path);
        return null;
      }
      try (final OutputStream os = Files.newOutputStream(path)) {
        os.write(body.clone());
      } catch (final IOException e) {
        LOGGER.error("Could not write remote asset {} to local cache", path);
        return null;
      }
      return new Artifact(path, body);
    }
    return null;
  }

  private static boolean shouldCheckHashes(final String url) {
    return Arrays.stream(HASH_TYPES).noneMatch(type -> url.endsWith(type.extension()));
  }

  private void tryAndRemoveExpiredFile(final Path path, final @Nullable Duration lifetime) {
    if (lifetime != null) {
      if (Files.isRegularFile(path)) {
        final FileTime lastModified;
        try {
          lastModified = Files.getLastModifiedTime(path);
        } catch (final IOException e) {
          LOGGER.error("Could not get last modified time for file {}", path);
          return;
        }
        if (Duration.between(lastModified.toInstant(), Instant.now()).compareTo(lifetime) >= 0) {
          try {
            Files.deleteIfExists(path);
          } catch (final IOException e) {
            LOGGER.error("Could not delete expired file {}", path);
          }
        }
      }
    }
  }

  public @Nullable MavenHashPair downloadHash(final URI jarUri) {
    for (final MavenHashType type : this.configuration.hashes()) {
      final URI hashUri = UriComponentsBuilder.fromUri(jarUri).replacePath(jarUri.getPath() + "." + type.extension()).build().toUri();
      try {
        final ResponseEntity<String> response = this.restClient.get()
          .uri(hashUri)
          .header(HttpHeaders.USER_AGENT, USER_AGENT)
          .retrieve()
          .toEntity(String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          final String body = response.getBody();
          if (body != null) {
            return new MavenHashPair(body, type);
          }
        }
      } catch (final Exception e) {
        if (isActuallyAnException(e)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Could not download {} hash for X. Url: {}, Exception: {}: {}", type, hashUri, e.getClass().getName(), e.getMessage()); // TODO: better message
          } else {
            LOGGER.warn("Could not download {} hash for X. Url: {}, Exception: {}", type, hashUri, e.getClass().getName()); // TODO: better message
          }
        }
      }
    }
    return null;
  }

  private static boolean isActuallyAnException(final Exception exception) {
    if (exception instanceof final HttpClientErrorException e) {
      return e.getStatusCode() != HttpStatus.NOT_FOUND;
    }
    return true;
  }
}
