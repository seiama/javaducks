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

import com.seiama.javaducks.api.model.Project;
import com.seiama.javaducks.util.maven.MavenHashType;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
@NullMarked
public record AppConfiguration(
  URI rootRedirect,
  URI apiBaseUrl,
  String apiTitle,
  String apiVersion,
  Path storage,
  List<EndpointConfiguration> endpoints,
  Map<String, Map<String, Project>> projects,
  @DefaultValue({"SHA256", "SHA1"})
  List<MavenHashType> hashTypes
) {

  public @Nullable String namespaceFromProjectName(final String projectName) {
    for (final Map.Entry<String, Map<String, Project>> entry : this.projects.entrySet()) {
      for (final String name : entry.getValue().keySet()) {
        if (name.equals(projectName)) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  public @Nullable Project projectFromNamespace(final String namespace, final String projectName) {
    final Map<String, Project> projects = this.projects.get(namespace);
    return projects == null ? null : projects.get(projectName); // Could also return a dummy project with an empty namespace and displayName
  }

  public EndpointConfiguration.@Nullable Version endpoint(final String endpointName, final String versionName) {
    for (final EndpointConfiguration endpoint : this.endpoints) {
      if (endpoint.name().equals(endpointName)) {
        for (final EndpointConfiguration.Version version : endpoint.versions()) {
          if (version.name().equals(versionName)) {
            return version;
          }
        }
      }
    }
    return null;
  }

  @NullMarked
  public record Project(
    String displayName
  ) {
    // TODO: ugly bad no
    public com.seiama.javaducks.api.model.Project toApiModel(final String namespace, final String name) {
      return new com.seiama.javaducks.api.model.Project(namespace, name, this.displayName);
    }
  }

  @NullMarked
  public record EndpointConfiguration(
    String name,
    List<Version> versions
  ) {
    @NullMarked
    public record Version(
      String name,
      String path,
      Type type
    ) {
      public static final String PATTERN = "[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?";

      public URI asset(final String name) {
        return URI.create(this.path + name);
      }

      @NullMarked
      public enum Type {
        SNAPSHOT,
        RELEASE,
        REDIRECT,
      }
    }
  }
}
