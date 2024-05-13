package com.seiama.javaducks.controller.api.v1;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.model.Project;
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
public class ProjectController {
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
    final Project project = new Project(this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).map(AppConfiguration.EndpointConfiguration::name).findFirst().orElseThrow(ProjectNotFound::new));
    final List<AppConfiguration.EndpointConfiguration.Version> versions = this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).findFirst().orElseThrow(ProjectNotFound::new).versions();
    return HTTP.cachedOk(ProjectResponse.from(project, versions), CACHE);
  }

  @Schema
  private record ProjectResponse(
    @Schema(name = "project", pattern = "[a-z]+", example = "paper")
    String project,
    @Schema(name = "versions")
    List<String> versions
  ) {
    static ProjectResponse from(final Project project, final List<AppConfiguration.EndpointConfiguration.Version> versions) {
      return new ProjectResponse(
        project.name(),
        versions.stream().map(AppConfiguration.EndpointConfiguration.Version::name).toList()
      );
    }
  }
}
