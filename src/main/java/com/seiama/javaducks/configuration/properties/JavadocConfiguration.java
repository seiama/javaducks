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

import com.seiama.javaducks.service.maven.request.ArtifactRequest;
import com.seiama.javaducks.util.maven.MavenConstants;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.javadoc")
@NullMarked
public record JavadocConfiguration(
  List<Alias> aliases
) {
  public @Nullable Alias findAlias(final String aliasName) {
    for (final Alias alias : this.aliases) {
      if (alias.name.equals(aliasName)) {
        return alias;
      }
    }
    return null;
  }

  public Alias.@Nullable Endpoint findAliasEndpoint(final String aliasName, final String endpointName) {
    final @Nullable Alias alias = this.findAlias(aliasName);
    if (alias != null) {
      for (final Alias.Endpoint endpoint : alias.endpoints) {
        for (final String name : endpoint.names) {
          if (name.equals(endpointName)) {
            return endpoint;
          }
        }
      }
    }
    return null;
  }

  @NullMarked
  public record Alias(
    String namespace,
    String name,
    @Nullable Path favicon,
    List<Endpoint> endpoints
  ) {
    @NullMarked
    public record Endpoint(
      List<String> names,
      String repository,
      Artifact artifact
    ) {
      @NullMarked
      public record Artifact(
        String groupId,
        String artifactId,
        String version
      ) {
        public ArtifactRequest asMavenRequest() {
          return new ArtifactRequest(this.groupId, this.artifactId, this.version, null, null, MavenConstants.CLASSIFIER_JAVADOC, MavenConstants.EXTENSION_JAR, null, null);
        }
      }
    }
  }
}
