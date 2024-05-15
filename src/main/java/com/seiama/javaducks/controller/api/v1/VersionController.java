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
package com.seiama.javaducks.controller.api.v1;

import com.seiama.javaducks.api.v1.error.request.NamespaceNotFound;
import com.seiama.javaducks.api.v1.error.request.ProjectNotFound;
import com.seiama.javaducks.api.v1.error.request.VersionNotFound;
import com.seiama.javaducks.api.v1.response.VersionResponse;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.util.HTTP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public final class VersionController {
  private static final CacheControl CACHE = HTTP.sMaxAgePublicCache(Duration.ofMinutes(5));
  private final AppConfiguration configuration;

  @Autowired
  private VersionController(
    final AppConfiguration configuration
  ) {
    this.configuration = configuration;
  }

  @ApiResponse(
    content = @Content(
      schema = @Schema(implementation = VersionResponse.class)
    ),
    responseCode = "200"
  )
  @GetMapping("/api/v1/projects/{project:[a-z]+}/versions/{version:" + AppConfiguration.EndpointConfiguration.Version.PATTERN + "}")
  @Operation(summary = "Gets information about a version.")
  public ResponseEntity<?> version(
    @Parameter(name = "project", description = "The project identifier.", example = "paper")
    @PathVariable("project")
    @Pattern(regexp = "[a-z]+")
    final String projectName,
    @Parameter(description = "A version of the project.")
    @PathVariable("version")
    @Pattern(regexp = AppConfiguration.EndpointConfiguration.Version.PATTERN)
    final String versionName
  ) {
    final @Nullable String namespace = this.configuration.namespaceFromProjectName(projectName);
    if (namespace == null) {
      return HTTP.fail(VersionResponse.error(new NamespaceNotFound(projectName)));
    }

    final AppConfiguration.Project project = this.configuration.projectFromNamespace(namespace, projectName); // TODO: this might need to be com.seiama.javaducks.api.model.Project
    if (project == null) {
      return HTTP.fail(VersionResponse.error(new ProjectNotFound(namespace, projectName)));
    }
    for (final AppConfiguration.EndpointConfiguration endpoint : this.configuration.endpoints()) {
      if (endpoint.name().equals(projectName)) {
        for (final AppConfiguration.EndpointConfiguration.Version version : endpoint.versions()) {
          if (version.name().equals(versionName)) {
            return HTTP.cachedOk(VersionResponse.from(project.toApiModel(namespace, projectName), version, this.configuration), CACHE);
          }
        }
      }
    }
    return HTTP.fail(VersionResponse.error(new VersionNotFound(versionName))); // TODO: should include project and shit
  }
}
