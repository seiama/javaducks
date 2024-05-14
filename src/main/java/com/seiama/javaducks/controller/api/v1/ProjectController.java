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
import com.seiama.javaducks.api.model.Version;
import com.seiama.javaducks.api.v1.response.ProjectResponse;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.util.HTTP;
import com.seiama.javaducks.util.exception.ProjectNotFound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
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
public final class ProjectController {
  private static final CacheControl CACHE = HTTP.sMaxAgePublicCache(Duration.ofMinutes(30));
  private final AppConfiguration configuration;

  @Autowired
  private ProjectController(
    final AppConfiguration configuration
  ) {
    this.configuration = configuration;
  }

  @ApiResponse(
    content = @Content(
      schema = @Schema(implementation = ProjectResponse.class)
    ),
    responseCode = "200"
  )
  @GetMapping("/api/v1/projects/{project:[a-z]+}")
  @Operation(summary = "Gets information about a project.")
  public ResponseEntity<?> project(
    @Parameter(name = "project", description = "The project identifier.", example = "paper")
    @PathVariable("project")
    @Pattern(regexp = "[a-z]+") //
    final String projectName
  ) {
    final Project project = new Project("papermc", this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).map(AppConfiguration.EndpointConfiguration::name).findFirst().orElseThrow(ProjectNotFound::new), "b");
    final List<AppConfiguration.EndpointConfiguration.Version> versions = this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).findFirst().orElseThrow(ProjectNotFound::new).versions();
    return HTTP.cachedOk(ProjectResponse.from(project, versions.stream().filter(v -> v.type() != AppConfiguration.EndpointConfiguration.Version.Type.REDIRECT).map(i -> new Version(i.name(), null)).toList()), CACHE);
  }
}
