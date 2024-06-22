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
package com.seiama.javaducks.api.v1.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.seiama.javaducks.api.model.Project;
import com.seiama.javaducks.api.model.Version;
import com.seiama.javaducks.api.v1.error.Error;
import com.seiama.javaducks.api.v1.error.ToError;
import com.seiama.javaducks.configuration.properties.AppConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema
public record VersionResponse(
  @Schema(name = "ok")
  boolean ok,
  @Schema(name = "project", pattern = "[a-z]+", example = "paper")
  @Nullable Project project,
  @Schema(name = "version", pattern = AppConfiguration.EndpointConfiguration.Version.PATTERN, example = "1.18")
  @Nullable Version version,
  @JsonUnwrapped
  @Nullable
  Error error
) {
  public static VersionResponse from(final Project project, final AppConfiguration.EndpointConfiguration.Version version, final AppConfiguration configuration) {
    return new VersionResponse(
      true,
      project,
      new Version(version.name(), new Version.Javadocs(configuration.apiBaseUrl().resolve(project.name() + "/" + version.name() + "/"))),
      null
    );
  }

  public static VersionResponse error(final ToError error) {
    return new VersionResponse(false, null, null, error.toError());
  }
}
