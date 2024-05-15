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

import com.seiama.javaducks.api.model.Project;
import com.seiama.javaducks.api.v1.response.ProjectsResponse;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.util.HTTP;
import com.seiama.javaducks.util.exception.NamespaceNotFound;
import com.seiama.javaducks.util.exception.ProjectNotFound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public final class ProjectsController {
  private static final CacheControl CACHE = HTTP.sMaxAgePublicCache(Duration.ofDays(7));
  private final AppConfiguration configuration;

  @Autowired
  private ProjectsController(final AppConfiguration configuration) {
    this.configuration = configuration;
  }

  @ApiResponse(
    content = @Content(
      schema = @Schema(implementation = ProjectsResponse.class)
    ),
    responseCode = "200"
  )
  @GetMapping("/api/v1/projects")
  @Operation(summary = "Gets a list of all available projects.")
  public ResponseEntity<?> projects() {
    // TODO: clean this up
    final List<Project> projects = this.configuration.endpoints().stream().map(endpoint -> {
      final @Nullable String namespace = this.configuration.namespaceFromProjectName(endpoint.name());
      if (namespace == null) {
        // return here?
        throw new NamespaceNotFound();
      }
      final AppConfiguration.Project project = this.configuration.projectFromNamespace(namespace, endpoint.name());
      if (project == null) {
        throw new ProjectNotFound("No project found for namespace: " + namespace + " and project: " + endpoint.name());
      }
      return new Project(namespace, endpoint.name(), project.displayName());
    }).toList();
    return HTTP.cachedOk(ProjectsResponse.success(projects), CACHE);
  }
}
