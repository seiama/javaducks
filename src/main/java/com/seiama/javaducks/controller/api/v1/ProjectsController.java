package com.seiama.javaducks.controller.api.v1;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.model.Project;
import com.seiama.javaducks.util.HTTP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectsController {
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
    final List<Project> projects = this.configuration.endpoints().stream().map(endpoint -> new Project(endpoint.name())).toList();
    return HTTP.cachedOk(ProjectsResponse.from(projects), CACHE);
  }

  @Schema
  private record ProjectsResponse(
    @Schema(name = "projects")
    List<String> projects
  ) {
    static ProjectsResponse from(final List<Project> projects) {
      return new ProjectsResponse(projects.stream().map(Project::name).toList());
    }
  }
}
