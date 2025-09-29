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
package com.seiama.javaducks.service.javadoc.injection;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.service.javadoc.JavadocKey;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@NullMarked
public class ExperimentalBannerInjection implements Injection {
  private static final Pattern EXPERIMENTAL_RELEASE_PATTERN = Pattern.compile("\\b(?:rc|pre)\\b", Pattern.CASE_INSENSITIVE);

  private final AppConfiguration configuration;
  private final String template;

  public ExperimentalBannerInjection(final AppConfiguration configuration) {
    this.configuration = configuration;
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("experimental-banner.html").getInputStream()))) {
      this.template = reader.lines().collect(Collectors.joining("\n"));
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canInject(final Path file, final JavadocKey key) {
    return file.toString().endsWith(HTML) && this.experimentalVersion(key);
  }

  @Override
  public String inject(final String line, final Path file, final JavadocKey key) {
    if (line.contains("<div class=\"top-nav\"") || line.contains("<div class=\"topNav\"")) {
      final String newVersion = this.latestVersion(key);
      return this.template.formatted(
        StringUtils.capitalize(key.project()) + " " + key.version(),
        StringUtils.capitalize(key.project()) + " " + newVersion,
        key.project() + "/" + key.version() + "/",
        key.project() + "/" + newVersion + "/",
        line
      );
    }
    return line;
  }

  private boolean experimentalVersion(final JavadocKey key) {
    return EXPERIMENTAL_RELEASE_PATTERN.matcher(key.version()).find();
  }

  private String latestVersion(final JavadocKey key) {
    return this.configuration.endpoints().stream()
      .filter(e -> e.name().equals(key.project()))
      .findFirst()
      .map(e -> {
        final List<AppConfiguration.EndpointConfiguration.Version> list = e.versions().stream().filter(version -> version.type() == AppConfiguration.EndpointConfiguration.Version.Type.RELEASE).toList();
        return list.isEmpty() ? key.version() : list.get(list.size() - 1).name();
      })
      .orElse(key.version());
  }
}
