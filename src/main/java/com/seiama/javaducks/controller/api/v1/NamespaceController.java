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

import com.seiama.javaducks.api.model.Namespace;
import com.seiama.javaducks.api.model.Project;
import com.seiama.javaducks.api.v1.error.request.NamespaceNotFound;
import com.seiama.javaducks.api.v1.response.NamespaceResponse;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.util.HTTP;
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
public final class NamespaceController {
  private static final CacheControl CACHE = HTTP.sMaxAgePublicCache(Duration.ofMinutes(30));
  private final AppConfiguration configuration;

  @Autowired
  private NamespaceController(
    final AppConfiguration configuration
  ) {
    this.configuration = configuration;
  }

  @ApiResponse(
    content = @Content(
      schema = @Schema(implementation = NamespaceResponse.class)
    ),
    responseCode = "200"
  )
  @GetMapping("/api/v1/namespaces/{namespace:[a-z]+}")
  @Operation(summary = "Gets information about a namespace.")
  public ResponseEntity<?> namespace(
    @Parameter(name = "namespace", description = "The project namespace.", example = "papermc")
    @PathVariable("namespace")
    @Pattern(regexp = "[a-z]+")
    final String spaceName // TODO: better name lol
  ) {
    // Implement the new NameespaceNotFound error
    for (final String namespaceStr : this.configuration.projects().keySet()) {
      if (namespaceStr.equals(spaceName)) {
        final Namespace namespace = new Namespace(spaceName);
        final List<Project> projects = this.configuration.projects().get(spaceName).entrySet().stream().map(proj -> new Project(spaceName, proj.getKey(), proj.getValue().displayName())).toList();

        return HTTP.cachedOk(NamespaceResponse.from(namespace, projects), CACHE);
      }
    }
    return HTTP.fail(NamespaceResponse.error(new NamespaceNotFound(spaceName)));

//    final Namespace namespace = this.configuration.projects().keySet().stream().filter(i -> i.equals(spaceName)).map(Namespace::new).findFirst().get(); // TODO: get bad
//    final List<Project> projects = this.configuration.projects().get(spaceName).entrySet().stream().map(proj -> new Project(spaceName, proj.getKey(), proj.getValue().displayName())).toList();
//
//    return HTTP.cachedOk(NamespaceResponse.from(namespace, projects), CACHE);
  }
}
