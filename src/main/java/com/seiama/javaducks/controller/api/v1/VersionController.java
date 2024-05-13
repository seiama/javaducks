package com.seiama.javaducks.controller.api.v1;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.model.Project;
import com.seiama.javaducks.util.HTTP;
import com.seiama.javaducks.util.exception.ProjectNotFound;
import com.seiama.javaducks.util.exception.VersionNotFound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;

import java.net.URI;
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
public class VersionController {
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
    @Pattern(regexp = "[a-z]+") //
    final String projectName,
    @Parameter(description = "A version of the project.")
    @PathVariable("version")
    @Pattern(regexp = AppConfiguration.EndpointConfiguration.Version.PATTERN) //
    final String versionName
  ) {
    final Project project = new Project(this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).map(AppConfiguration.EndpointConfiguration::name).findFirst().orElseThrow(ProjectNotFound::new));
    final AppConfiguration.EndpointConfiguration.Version version = this.configuration.endpoints().stream().filter(endpoint -> endpoint.name().equals(projectName)).findFirst().orElseThrow(VersionNotFound::new).versions().stream().filter(v -> v.name().equals(versionName)).findFirst().orElseThrow(VersionNotFound::new);
    return HTTP.cachedOk(VersionResponse.from(project, version, this.configuration), CACHE);
  }

  @Schema
  private record VersionResponse(
    @Schema(name = "project", pattern = "[a-z]+", example = "paper")
    String project,
    @Schema(name = "version", pattern = AppConfiguration.EndpointConfiguration.Version.PATTERN, example = "1.18")
    String version,
    @Schema(name = "uri")
    URI uri
  ) {
    static VersionResponse from(final Project project, final AppConfiguration.EndpointConfiguration.Version version, final AppConfiguration configuration) {
      return new VersionResponse(
        project.name(),
        version.name(),
        configuration.apiBaseUrl().resolve(project.name() + "/" + version.name() + "/")
      );
    }
  }
}
